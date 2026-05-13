# Backend Integration Guide

## 目标

这份文档给要改 `backend/` 或要把前端功能接到 backend 的开发者用，重点回答三个问题：

1. `backend/` 现在到底承载什么。
2. 新能力应该按什么顺序接进去。
3. 前端应该怎样对接 backend，才不会把分层打回去。

## 当前架构

当前 Android split 版的实际调用链是：

```text
UI Page
  -> EasyTierService
  -> AndroidAdapter
  -> JsonRpcClient
  -> BackendClient
  -> JniBackendClient
  -> EasyTierJNI
  -> libeasytier_ffi.so / libeasytier_android_jni.so
```

这里要注意：

- `backend/` 当前还是本地 library，不是单独启动的服务器进程。
- “前后端分离”在这个仓库里更准确叫“前端壳和共享后端核心分层”。

## `backend/` 目录怎么理解

```text
backend/
├── build.gradle.kts
├── README.md
├── DEVELOPMENT_PLAN.md
├── protocol/
│   ├── README.md
│   └── easytier.rpc.json
├── core/
│   └── easytier.models.json
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/easytier/backend/
    ├── java/com/easytier/data/
    ├── java/com/easytier/jni/
    └── jniLibs/arm64-v8a/
```

### 各块职责

#### `protocol/`

- 定义统一协议
- 机器可读协议文件在 `easytier.rpc.json`
- 新增公共能力时，优先先看这里要不要补定义

#### `core/`

- 放共享模型说明
- 如果新增字段会影响多端，先在这里定模型边界

#### `src/main/java/com/easytier/backend/`

- 后端接口
- JSON-RPC 路由层
- 工具函数

最关键的文件：

- `BackendClient.kt`
- `JsonRpcClient.kt`
- `BackendNodeInfoUtils.kt`
- `OneClickConnectionCode.kt`
- `CsvListUtils.kt`

#### `src/main/java/com/easytier/backend/jni/`

- JNI 后端实现
- 目前核心实现类是 `JniBackendClient.kt`

#### `src/main/java/com/easytier/jni/`

- JNI 壳对象 `EasyTierJNI.kt`
- 这里只做薄封装，不做业务逻辑

#### `src/main/java/com/easytier/data/`

- 共享数据模型
- 比如 `NetworkConfig`、`NodeInfo`、`ServerEntry`

#### `src/main/jniLibs/arm64-v8a/`

- native so 打包位置
- 当前已经由 backend 统一承载：
  - `libeasytier_ffi.so`
  - `libeasytier_android_jni.so`

## app 侧和 backend 侧的边界

### 应该放在 backend 的内容

- 可复用数据模型
- 可复用工具函数
- JSON-RPC 协议层
- JNI/FFI 封装
- 原生能力的统一调用入口

### 不应该放在 backend 的内容

- `VpnService`
- `Context`
- `Activity`
- `SharedPreferences`
- `startService`
- 权限申请
- Android 页面状态

这些内容仍然应该留在 app 侧，比如：

- `android_backup/app/src/main/java/com/easytier/backend/AndroidAdapter.kt`
- `android_backup/app/src/main/java/com/easytier/service/EasyTierService.kt`
- `android_backup/app/src/main/java/com/easytier/service/EasyTierVpnService.kt`

## 前端如何接现有 backend 能力

如果只是使用已有能力，不需要直接改 `backend/`，按下面接：

1. 先确认 `BackendClient` 里已经有这个方法。
2. 确认 `JniBackendClient` 已实现。
3. 看 `JsonRpcClient` 是否已有对应 method 路由。
4. 在 app 侧通过 `AndroidAdapter` 或 `EasyTierService` 暴露 typed API。
5. 页面只调用 service / adapter，不直接写 JSON-RPC 字符串。

一个现有能力的调用例子是 `network.start`：

```text
NetworkConfigPage
  -> EasyTierService.startNetwork(config)
  -> backend.startNetwork(config)
  -> JniBackendClient.startNetwork(config)
  -> EasyTierJNI.runNetworkInstance(toml)
```

## 新增一个 backend 能力的推荐步骤

如果要新增真正的后端能力，按这个顺序做最稳：

### 1. 先定协议边界

如果这是公共能力，先看是否需要更新：

- `protocol/easytier.rpc.json`
- `core/easytier.models.json`

不要先改页面再回头补协议，否则很容易把平台特有实现写死。

### 2. 在 `BackendClient.kt` 补接口

这是共享后端能力的源头接口。

原则：

- 方法名语义清晰
- 入参尽量用共享模型
- 返回值尽量用共享结果模型

### 3. 在 `JniBackendClient.kt` 补实现

这里负责把共享接口落到底层 JNI。

要做的事通常包括：

- 调 `EasyTierJNI.*`
- 处理错误串
- 把底层结果转成 Kotlin 结果对象

### 4. 在 `JsonRpcClient.kt` 补路由

如果这项能力需要走统一协议边界，就在这里增加 method 路由。

这里负责：

- 解析 params
- 调用 `BackendClient`
- 组装 JSON-RPC result / error

### 5. 如有新结构，补 `ProtocolModels.kt`

如果响应结构变复杂，不要让页面自己解析 `JSONObject`，把 typed model 落到：

- `backend/src/main/java/com/easytier/backend/protocol/ProtocolModels.kt`

### 6. app 侧补 typed API

如果 Android 页面要用，继续补：

- `android_backup/app/src/main/java/com/easytier/backend/AndroidAdapter.kt`
- `android_backup/app/src/main/java/com/easytier/service/EasyTierService.kt`

原则：

- `AndroidAdapter` 负责 Android 平台差异
- `EasyTierService` 负责给页面一个更稳定的服务入口

### 7. 页面最后再接

页面只接最终 typed API，不直接碰 JNI，不直接拼协议字符串。

## 一个最小接入模板

下面这个模板适合你新增一项 backend 能力时对照：

```text
protocol/easytier.rpc.json        如果要新增公共协议
core/easytier.models.json         如果要新增共享模型
BackendClient.kt                  补接口
JniBackendClient.kt               补 JNI 实现
JsonRpcClient.kt                  补 method 路由
ProtocolModels.kt                 补结果模型（可选）
AndroidAdapter.kt                 补 Android typed API
EasyTierService.kt                给页面暴露入口
ui/pages/*.kt                     页面接入
```

## 常见错误

### 错误 1：页面直接调 `EasyTierJNI`

这样会导致：

- 页面层和底层强耦合
- 后续 Qt / Web 无法复用
- 日后 JSON-RPC 边界失效

### 错误 2：把 `VpnService` 往 backend 里搬

`VpnService` 是 Android 宿主能力，不是共享 backend 能力。

如果放进 backend，会让 backend 重新和 Android 强绑定，违背当前分层目标。

### 错误 3：native so 在 app 和 backend 两边各放一份

当前已经统一到：

- `backend/src/main/jniLibs/arm64-v8a/`

不要再在 `android_backup/app/src/main/jniLibs/` 复制一份，避免双份维护和打包歧义。

### 错误 4：改了 `BackendClient` 却没改 `JsonRpcClient`

如果你打算让多端共用协议边界，只改底层实现还不够，必须把路由也补齐。

### 错误 5：新增字段后页面直接解析原始 JSON

正确做法是：

- 先在共享模型层补字段
- 再暴露 typed model
- 最后页面消费 typed model

## 构建与验证

### Android split 版构建

```powershell
Set-Location "d:\code\code\qt_easytier_mobile\android_backup"
.\gradlew.bat :app:assembleDebug
```

### 关键验证点

至少检查下面几项：

1. `android_backup` 能成功依赖 `:backend`。
2. 最终 APK 里包含 backend 的 so。
3. 应用启动后日志出现 backend 初始化成功。
4. 新增能力能从 UI 顺着 service 跑到 JNI。

## 当前这套结构的一句话原则

`backend/` 负责“共享逻辑和统一边界”，`android_backup/` 负责“Android 壳和页面接入”。

如果一段代码脱离 Android 还能复用，就优先放 backend；如果它必须依赖 Android 平台能力，就留在 app。