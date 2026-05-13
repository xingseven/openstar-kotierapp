# Adapters

这里放不同前端或平台到统一后端协议的薄适配层说明。

## 分层职责

```
前端 UI → Adapter（平台适配层）→ JSON-RPC 协议层 → BackendClient（JNI/FFI）→ EasyTier 引擎
```

- **Adapter**：处理平台特有逻辑（VPN 权限、TUN fd、桌面保活等），对外提供 typed API
- **JSON-RPC 协议层**（JsonRpcClient）：接收 JSON-RPC 格式的请求字符串，路由到 BackendClient，返回 JSON-RPC 格式的响应。任何前端只要会发 JSON 就能接这套后端。
- **BackendClient**：JNI/FFI 薄封装，对应底层引擎能力

## Android Adapter

位置：`android/app/src/main/java/com/easytier/backend/AndroidAdapter.kt`

- 通过 `JsonRpcClient` 走 JSON-RPC 协议与后端通信
- 封装 VpnService 授权、TUN 文件描述符管理、前台服务
- 提供 typed Kotlin API：`ping()`、`startNetwork(config)`、`stopNetwork(name)` 等
- 内置节点监控协程：`startMonitoring(instanceName, onNodes)`

调用示例：
```kotlin
val adapter = AndroidAdapter(jsonRpcClient, context)

// 检查后端状态
val ping = adapter.ping()

// 启动网络
adapter.startNetwork(config)

// 绑定 TUN
adapter.vpnAttach("instance-1", tunFd)

// 监控节点
adapter.startMonitoring("instance-1") { nodes ->
    // UI 更新
}
```

## Qt Adapter

位置：`qt-easy-tier-master/SRC/backend/QtAdapter.h/.cpp`

- 通过 `JsonRpcClient` 走 JSON-RPC 协议与后端通信
- 处理桌面端配置读取、窗口交互、运行态展示
- 通过 Qt 信号槽机制通知 UI 更新：`networkStarted`、`networkStopped`、`stateUpdated` 等
- 提供 typed C++ API 和 `callJsonRpc() 原始接口`

调用示例：
```cpp
auto adapter = new QtAdapter(backend, this);

// 检查后端状态
auto ping = adapter.ping();

// 启动网络
adapter.startNetwork("inst-1", tomlConfig);

// 监听状态变化
connect(adapter, &QtAdapter::stateUpdated, this, [](const QString &name) {
    // UI 更新
});
```

## 约束

- 适配层代码应尽量薄，只处理平台差异
- 后续新增前端时优先复用协议，不要复制一份业务逻辑
- 所有新能力先定义到 `protocol/easytier.rpc.json`，再在各端 Adapter 中实现
