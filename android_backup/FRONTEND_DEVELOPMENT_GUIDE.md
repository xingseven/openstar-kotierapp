# Android Split Frontend Development Guide

## 目标

这份文档给要改 `android_backup/` 的开发者用，重点解决两个问题：

1. 前端代码应该写到哪里。
2. 前端应该怎样和 `backend/` 对接，而不是重新把后端逻辑写回 app。

当前这套可运行结构不是“纯前端 + 纯服务器”，而是：

- `android_backup/`：前端 UI + Android 宿主壳
- `backend/`：共享后端核心 + JNI + 原生库

## 先理解现在的边界

### `android_backup/` 负责什么

- 页面、组件、主题、图标、资源
- Activity / Application / Manifest
- Android 平台能力：`VpnService`、`Context`、权限申请、前台服务
- 调用 `EasyTierService`，把后端能力接到 UI 上

### `backend/` 负责什么

- 可复用的数据模型
- JSON-RPC 协议层
- JNI 后端客户端
- EasyTier 原生库打包
- 多前端可以共用的工具函数

### 不要混淆的点

- `android_backup/` 不是“只剩页面”的 Web 前端，它仍然是 Android App。
- `backend/` 不是远程 HTTP 服务，它目前仍然是本地 Android library 模块。
- 最终 APK 是 `android_backup + backend` 一起打进去，不是运行时动态下载后端。

## 目录速览

```text
android_backup/
├── app/src/main/java/com/easytier/
│   ├── ui/pages/          页面
│   ├── ui/components/     复用组件
│   ├── ui/theme/          主题
│   ├── service/           Android 宿主服务层
│   ├── backend/           Android 平台适配层
│   ├── MainActivity.kt    Compose 入口
│   └── EasyTierApplication.kt
├── app/src/main/res/      资源
└── app/build.gradle.kts   app 构建配置
```

最常用的入口文件：

- `app/src/main/java/com/easytier/ui/pages/HomePage.kt`
- `app/src/main/java/com/easytier/ui/pages/NetworkConfigPage.kt`
- `app/src/main/java/com/easytier/ui/pages/OneClickPage.kt`
- `app/src/main/java/com/easytier/service/EasyTierService.kt`
- `app/src/main/java/com/easytier/backend/AndroidAdapter.kt`

## 前端开发的标准路径

### 1. 改页面

页面放在 `ui/pages/`。

适合放页面的内容：

- 页面布局
- 交互状态
- 页面内表单
- 页面级协程调用
- 页面跳转与弹窗

如果一个控件会被多个页面复用，放到 `ui/components/`。

### 2. 改共享组件

共享组件放在 `ui/components/`。

适合放组件的内容：

- Dialog
- TopBar
- 卡片组件
- 通用开关、表单项、图标组件

原则：组件只管展示和回调，不要在组件里直接调 JNI。

### 3. 改主题与视觉

主题相关放在：

- `ui/theme/Theme.kt`
- `res/values/colors.xml`
- `res/values/themes.xml`

如果只是视觉调整，优先从主题和组件下手，不要把颜色硬编码散落到页面里。

### 4. 需要持久化时走 `SettingsRepository`

本地设置、收藏服务器、网络配置这类持久化数据，不要页面直接写 `SharedPreferences`，统一走：

- `service/SettingsRepository.kt`

这样页面只关心数据，不关心存储细节。

### 5. 需要日志时走 `LogService`

页面和服务层要记录可见日志时，统一走：

- `service/LogService.kt`

不要在页面里只打 `Log.d()` 然后 UI 看不到。

## 前端如何调用后端

推荐调用链：

```text
UI Page
  -> EasyTierService
  -> AndroidAdapter
  -> JsonRpcClient
  -> BackendClient / JniBackendClient
  -> EasyTierJNI
  -> native so
```

### 页面层应该调用谁

页面优先调用：

- `EasyTierService`

不要在 `ui/pages/` 里直接 new `JniBackendClient()`，也不要直接调 `EasyTierJNI.*`。

一个典型页面调用示例：

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        val result = EasyTierService.startNetwork(config)
        if (!result.success) {
            LogService.error("启动失败: ${result.errorMessage}", source = "NetworkPage")
        }
    }
}) {
    Text("启动网络")
}
```

### 什么场景走 `AndroidAdapter`

`AndroidAdapter` 主要处理 Android 平台特有能力，例如：

- VPN 授权
- TUN fd 绑定
- 监控节点轮询
- 启停 `EasyTierVpnService`

也就是说：

- 能放到共享 backend 的能力，尽量放 backend。
- 一旦依赖 `Context`、`VpnService`、权限、系统服务，就留在 app 侧。

## 新增一个页面时的推荐步骤

1. 在 `ui/pages/` 新建页面文件。
2. 如果有复用控件，先提到 `ui/components/`。
3. 如果页面要显示已有运行态数据，优先复用 `EasyTierService` 的现成方法。
4. 如果页面要新增后端能力，不要先改页面，先改 `backend/` 的接口与路由。
5. 页面只接 typed API，不直接解析原始 JSON。

## 新增一个前端功能时的判断标准

### 只改 `android_backup/`

满足这些情况时，只改前端壳：

- 新页面
- 组件重构
- 主题样式调整
- 本地交互状态调整
- 日志展示、表单交互、页面引导

### 同时改 `backend/`

满足这些情况时，要联动 backend：

- 需要新的后端能力
- 现有能力返回字段不够
- 需要新增数据模型
- 需要把重复工具函数下沉到共享层

## 明确不要做的事

### 不要在页面里直接调 JNI

错误方向：

- 页面直接调用 `EasyTierJNI.collectNetworkInfos()`
- 页面直接拼 JSON-RPC 请求字符串
- 页面自己解析运行态 JSON

正确方向：

- 页面只调用 `EasyTierService` / `AndroidAdapter` 暴露出来的方法

### 不要把 Android 专属能力硬塞进 `backend/`

这些东西不要往 `backend/` 移：

- `VpnService`
- `Context`
- `Activity`
- `startService`
- 权限申请
- `SharedPreferences`

### 不要在 `android_backup/` 重新复制 backend 代码

现在 `backend/` 已经是共享源头。

不要再把这些目录拷贝回 app：

- `com.easytier.backend`
- `com.easytier.data`
- `com.easytier.jni`

## 构建与验证

在 Windows 下进入 `android_backup/` 后执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

当前 split 版包名：

```text
com.easytier.app.split
```

当前 split 版应用名：

```text
EasyTier Split
```

这样它可以和原始 `android/` 工程的：

```text
com.easytier.app
```

并行安装，适合做前后端拆分验证。

## 前端开发自检清单

提交前至少检查：

1. 新逻辑是不是写在对的层里。
2. 页面有没有直接依赖 JNI 或原始 JSON。
3. 复用组件有没有抽到 `ui/components/`。
4. 持久化是不是统一走 `SettingsRepository`。
5. 新日志是不是进了 `LogService`。
6. `:app:assembleDebug` 是否通过。

## 一句话原则

前端只负责“展示、交互、宿主能力”，后端只负责“共享逻辑、协议、JNI 和原生能力”。

如果一段代码换个平台还能复用，它更应该在 `backend/`；如果它强依赖 Android 系统，它更应该留在 `android_backup/`。