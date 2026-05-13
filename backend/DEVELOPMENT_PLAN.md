# Backend 统一后端开发计划

## 目标

将 EasyTier Mobile 的后端逻辑从各前端中剥离，通过统一的 JSON-RPC 协议交互，实现"前端可换，协议不换"。

## 架构

```
前端 UI → Adapter（平台适配层）→ JSON-RPC 协议层 → BackendClient（JNI/FFI）→ EasyTier 引擎
```

## 当前状态

- [x] protocol/easytier.rpc.json — 协议定义（9 方法 + 4 事件 + 4 错误码）
- [x] core/easytier.models.json — 数据模型定义（JSON Schema）
- [x] Qt 侧工具下沉（RuntimeService、RuntimeInfoParser、RuntimeStateStore 等 15 文件）
- [x] Android 侧基础工具下沉（BackendClient、BackendNodeInfoUtils、OneClickConnectionCode 等 5 文件）

## 完成情况

### 第 1 阶段：Android 端协议实现 ✅

| 任务 | 状态 | 文件 |
|------|------|------|
| 1.1 补全 BackendClient 接口 | ✅ | BackendClient.kt — 新增 ping/validate/list/state/vpnAttach/vpnDetach/logSubscribe |
| 1.2 实现 JniBackendClient 缺失方法 | ✅ | JniBackendClient.kt — 基于 EasyTierJNI 实现全部 13 个方法 |
| 1.3 JSON-RPC 消息封装 | ✅ | JsonRpcMessage.kt + ProtocolModels.kt — Request/Response/Error 数据结构 |
| 1.4 JSON-RPC 协议客户端 | ✅ | JsonRpcClient.kt — 接收 JSON-RPC 请求，路由到 BackendClient，返回响应 |
| 1.5 Android 适配层 | ✅ | AndroidAdapter.kt — 封装 VPN/TUN，对外提供 typed API |

### 第 2 阶段：Qt 端协议实现 ✅

| 任务 | 状态 | 文件 |
|------|------|------|
| 2.1 补全 BackendClient 接口 | ✅ | BackendClient.h — 新增 ping/validate/list/state/logSubscribe |
| 2.2 实现 FfiBackendClient 缺失方法 | ✅ | FfiBackendClient.h/cpp — 基于 EasyTierFFI 实现全部协议方法 |
| 2.3 JSON-RPC 消息封装 | ✅ | JsonRpcMessage.h/cpp — Request/Response/Error 数据结构 |
| 2.4 JSON-RPC 协议客户端 | ✅ | JsonRpcClient.h/cpp — 接收 JSON-RPC 请求，返回响应 |
| 2.5 Qt 适配层 | ✅ | QtAdapter.h/cpp — 封装桌面平台逻辑，通过 Qt 信号槽通知 UI |

### 第 3 阶段：适配层标准化 ✅

| 任务 | 状态 | 说明 |
|------|------|------|
| 3.1 adapters/README.md 更新 | ✅ | 补充适配层对接指南和调用示例 |
| 3.2 EasyTierService 接入 | ✅ | Android 端使用 JsonRpcClient + AndroidAdapter 新架构 |

### 第 4 阶段：多网络实例同时运行 ✅

| 任务 | 状态 | 说明 |
|------|------|------|
| 4.1 EasyTierService 增加实例感知的 stopVpnService | ✅ | 新增带 instanceName 参数的 stopVpnService，仅停止匹配的 VPN 实例 |
| 4.2 EasyTierService 增加 VPN 冲突检测 | ✅ | 新增 isVpnInUseByOther / getActiveVpnInstanceName 方法 |
| 4.3 配置标签运行状态指示器 | ✅ | 每个配置标签前显示绿色/灰色圆点表示运行状态 |
| 4.4 UI 停止按钮逻辑修复 | ✅ | 修复启动/停止按钮，支持独立停止每个网络实例 |
| 4.5 VPN 占用冲突提示对话框 | ✅ | 当启动需要 VPN 的实例而 VPN 已被占用时，弹窗提示用户释放 |
| 4.6 无 TUN 模式共存 | ✅ | 一个实例使用 VPN，多个实例以 noTun 模式同时运行 |

## 新增前端的步骤

1. 在 `protocol/easytier.rpc.json` 中定义新能力（如果需要）
2. 实现平台对应的 `BackendClient`（JNI/FFI 封装）
3. 复用或实现 `JsonRpcClient`（协议序列化层）
4. 实现平台对应的 `Adapter`（处理平台差异）
5. UI 层只调用 Adapter 的 typed API

## 版本记录

| 版本 | 日期 | 变更 |
|------|------|------|
| v0.1.0 | - | 初始协议和模型定义 |
| v0.2.0 | 2026-05-13 | Android 端 JSON-RPC 协议实现 + 适配层 |
| v0.3.0 | 2026-05-13 | Qt 端 JSON-RPC 协议实现 + 适配层 |
