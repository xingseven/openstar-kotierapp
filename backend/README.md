# Backend

这里放的是面向所有前端的统一后端边界，不是某个 UI 的实现细节。

**前端可以换，协议不换**——Android、Qt、以后如果有 Web 前端，都只通过同一套 JSON-RPC 协议和后端交互。

## 架构

```
前端 UI → Adapter → JsonRpcClient → BackendClient → EasyTier 引擎
           │            │              │
       平台适配层    协议序列化层     JNI/FFI 薄封装
```

## 目录结构

```
backend/
├── README.md              ← 本文档
├── DEVELOPMENT_PLAN.md    ← 开发计划
├── protocol/
│   ├── README.md          ← 协议说明
│   └── easytier.rpc.json  ← JSON-RPC 协议清单（机器可读）
├── core/
│   └── easytier.models.json  ← 共享数据模型（JSON Schema）
└── adapters/
    └── README.md          ← 适配层说明
```

### 各端的实现落地

- **Android**：`android/app/src/main/java/com/easytier/backend/`
  - `BackendClient.kt` — 完整接口（13 个方法，对齐协议）
  - `jni/JniBackendClient.kt` — JNI 实现
  - `JsonRpcClient.kt` — JSON-RPC 协议层
  - `AndroidAdapter.kt` — Android 适配层（VPN、TUN、监控）
  - `BackendNodeInfoUtils.kt` — 节点 JSON 解析工具
  - `OneClickConnectionCode.kt` — 联机码编解码工具
  - `CsvListUtils.kt` — CSV 解析工具

- **Qt**：`qt-easy-tier-master/SRC/backend/`
  - `BackendClient.h` / `FfiBackendClient.h/.cpp` — 完整接口（12 个方法）
  - `JsonRpcMessage.h/.cpp` — JSON-RPC 消息封装
  - `JsonRpcClient.h/.cpp` — JSON-RPC 协议层
  - `QtAdapter.h/.cpp` — Qt 适配层（含 Qt 信号槽）
  - `RuntimeService.h/.cpp` — Worker 线程管理
  - `RuntimeInfoParser.h/.cpp` — 运行态 JSON 解析
  - `RuntimeStateStore.h/.cpp` — 状态缓存管理
  - `OneClickRuntimeParser.h/.cpp` — 一键联机解析
  - `OneClickConfigBuilder.h/.cpp` — 一键联机构建
  - `OneClickConnectionCode.h/.cpp` — 联机码编解码

## 现状

✅ 协议定义完成（9 方法 + 4 事件 + 4 错误码）
✅ 数据模型定义完成
✅ JSON-RPC 协议层实现（Android + Qt）
✅ 适配层实现（Android + Qt）
✅ Android 已通过 `EasyTierService` 接入新架构
✅ Qt 各工具类已下沉到 backend 命名空间

## 新增前端的步骤

1. 在 `protocol/easytier.rpc.json` 中定义新能力（如果需要）
2. 实现平台对应的 `BackendClient`（JNI/FFI 封装）
3. 复用或实现 `JsonRpcClient`（协议序列化层）
4. 实现平台对应的 `Adapter`（处理平台差异）
5. UI 层只调用 Adapter 的 typed API

这样，前端换、协议不换。
