# kotier

kotier 是一款基于 [EasyTier](https://github.com/EasyTier/EasyTier) 去中心化引擎的 Android 异地组网工具，目标是把“配置网络、启动 VPN、查看节点、快速联机”收敛到一个移动端应用里完成。

当前仓库以 Android 客户端为主，界面由 Kotlin + Jetpack Compose 实现，底层通过 JNI 连接 EasyTier 原生能力。

## 当前能力

- 普通网络页：创建和保存多套组网配置，支持 DHCP / 静态 IP、入口服务器、启动与停止网络。
- 一键联机页：支持房主创建网络、房客粘贴编码加入网络，适合快速联机。
- 服务器页：展示社区公共节点状态，支持收藏服务器和查看延迟概况。
- 设置页：支持主题切换、日志级别、运行日志查看、项目链接与基础信息展示。
- Android VPN 集成：通过 VpnService 建立 TUN 接口，把虚拟网段路由交给 EasyTier 后端。
- 运行状态监控：定时拉取节点状态、网络信息与运行日志，便于排查连接问题。

## 技术栈

- 语言：Kotlin
- UI：Jetpack Compose + Material 3
- 异步：Kotlin Coroutines
- 网络：OkHttp
- 组网引擎：EasyTier Rust 原生库（JNI）
- VPN：Android VpnService（TUN）
- 构建：Gradle Kotlin DSL

## 项目结构

```text
android/
├── app/
│   └── src/main/java/com/easytier/
│       ├── backend/     # Android 侧后端调用适配
│       ├── service/     # 核心服务，如 EasyTierService、VpnService、日志与设置
│       ├── ui/
│       │   ├── components/  # 通用组件
│       │   ├── pages/       # 网络、一键联机、服务器、设置、日志页面
│       │   └── theme/       # Material 3 主题与配色
│       ├── EasyTierApplication.kt
│       └── MainActivity.kt
│
backend/
├── adapters/       # 平台适配说明
├── core/           # 后端抽象核心
├── protocol/       # 协议层定义
└── src/            # 后端模块源码
```

## 架构说明

- 当前 Android 端仍以 JNI 直连 EasyTier 原生引擎为主，优先保证移动端可用性与稳定性。
- 仓库中的 `backend/` 目录用于收敛协议边界，减少前端页面直接耦合 JNI 细节。
- Android UI 主要通过 `EasyTierService`、`AndroidAdapter` 和 JNI 桥接层与底层组网能力交互。

## 构建要求

- JDK 17
- Android SDK 35
- 最低 Android 版本：26
- 当前默认只构建 `arm64-v8a`

## 本地构建

使用 Android Studio 打开 `android` 目录即可同步和构建，或直接使用 Gradle Wrapper：

```powershell
cd android
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

构建产物命名格式为：

```text
kotier-v<version>-<versionCode>-release.apk
```

## 原生库依赖

构建前需要准备以下原生库，并放入 `android/app/src/main/jniLibs/arm64-v8a/`：

- `easytier_ffi.so`
- `easytier_android_jni.so`

## 仓库说明

- Android 应用包名：`com.easytier.app.split`
- 当前仓库主项目地址：`https://github.com/xingseven/openstar-kotierapp`
- 详细更新记录见 `版本记录.md`
