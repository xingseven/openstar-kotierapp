# Adapters

这里放不同前端或平台到统一后端协议的薄适配层说明。

## Android Adapter

- 负责把 VpnService 的 TUN 句柄接到后端。
- 负责权限申请、前台服务、后台保活。
- 不负责业务协议本身。
- Android 侧一些可复用的 backend 工具已经提到 `android/app/src/main/java/com/easytier/backend/`，例如 TOML 脱敏、节点 JSON 解析、一键联机码编解码，以及运行实例名/代理 CIDR 的 JSON 解析。
- Android 侧的 `SettingsRepository` 也开始承担网络配置的 typed 读写，`ServersPage` 通过它回填服务器列表，不再直接改 JSON 数组。
- Android 侧的 `NetworkConfig` 也开始承担一键联机默认配置工厂，`OneClickPage` 不再直接拼 host/guest 初始字段。
- Android 侧的 `SettingsRepository` 也统一承担收藏服务器的 typed 持久化和旧默认服务器迁移，`ServersPage` 只消费 `List<ServerEntry>`。

## Qt Adapter

- 负责桌面端配置读取、窗口交互和运行态展示。
- 不直接操作核心协议以外的细节。
- 当前 Qt 端的 adapter 实现先落在 `qt-easy-tier-master/SRC/backend/`，用于把 FFI 运行时封装成薄客户端。
- 运行态 JSON 解析也已开始下沉到 `qt-easy-tier-master/SRC/backend/RuntimeInfoParser.*`，节点列表和日志事件解释不再直接挂在页面类上。
- 运行态缓存回写规则也已下沉到 `qt-easy-tier-master/SRC/backend/RuntimeStateStore.*`，启动/停止/轮询后的状态清理和日志增量合并不再由 `QtETNetwork` 直接逐字段操作。
- `qt-easy-tier-master/SRC/backend/RuntimeService.*` 现在接管了 worker 线程、跨线程调用和同步停机等待；`QtETNetwork` 与 `QtETOneClick` 都不再直接持有 `QThread` / `ETRunWorker`。
- `qt-easy-tier-master/SRC/backend/OneClickRuntimeParser.*` 现在接管了一键联机页的房主 IP、房客房主地址和联机人数相关 JSON 解释，`QtETOneClick` 不再自己解析运行态 JSON 结构。
- `qt-easy-tier-master/SRC/backend/OneClickConfigBuilder.*` 现在接管了一键联机页 host/guest TOML 配置拼装，`QtETOneClick` 不再自己拼接运行配置文本。
- `qt-easy-tier-master/SRC/backend/OneClickConnectionCode.*` 现在接管了一键联机页的联机码生成/解码与 Base32 编解码，`QtETOneClick` 不再自己维护这套编码逻辑。
- Android 侧对应的共享工具落在 `android/app/src/main/java/com/easytier/backend/OneClickConnectionCode.kt`，与 Qt 保持同样的联机码格式和编码规则。

## 约束

适配层的代码应尽量薄，后续新增前端时优先复用协议，不要复制一份业务逻辑。