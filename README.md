# QtEasyTier

一款基于 [EasyTier](https://github.com/EasyTier/EasyTier) 去中心化引擎的 Android 异地组网工具，美观实用，帮助您快速联机组网。

> 该项目由 C++/Qt 6 重构为纯 Kotlin + Jetpack Compose 实现。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **异步**: Kotlin Coroutines
- **组网引擎**: EasyTier Rust 原生库（通过 JNI 调用）
- **网络**: OkHttp
- **VPN**: Android VpnService (TUN)

## 架构方向

- 当前 Android 端仍以 JNI 直连原生引擎为主。
- 新增 `backend/` 目录，作为统一协议与适配层的落点，目标是把前端与后端调用边界稳定下来。
- 后续不同前端尽量只调用同一套协议，不再直接依赖各自的 JNI/FFI 细节。

## 功能

- 去中心化组网，无需公网 IP
- NAT 穿透（UDP/TCP 打洞、UPnP）
- 多协议支持：KCP、QUIC、TCP、UDP、WebSocket (WSS)
- 端到端 AES-GCM 加密
- 社区公共节点发现与状态监控
- 3 秒轮询节点实时状态（延迟、协议、流量）
- 智能路由：延迟优先、中继转发、出口节点

## 项目结构

```
backend/
├── adapters/       # Android / Qt 的平台适配层说明
└── protocol/       # 统一后端调用协议

android/
├── app/src/main/java/com/easytier/
│   ├── data/       # 数据模型 (NetworkConfig, NodeInfo, etc.)
│   ├── jni/        # JNI 桥接层 (EasyTierJNI)
│   ├── service/    # 核心服务 (EasyTierService, VpnService, etc.)
│   └── ui/
│       ├── components/ # 可复用组件
│       ├── pages/      # 页面 (网络、一键联机、服务器、设置)
│       └── theme/      # Material3 主题

android_app/
└── 旧目录与迁移残留
```

## 构建

使用 Android Studio 打开 `android_app/` 目录，同步 Gradle 后即可构建。

> 需要预编译 `easytier_ffi.so` 和 `easytier_android_jni.so` 放置于 `app/src/main/jniLibs/arm64-v8a/`。
