# EasyTier 后端统一协议

本协议定义 EasyTier Mobile 后端与前端之间的通信规范。基于 **JSON-RPC 2.0** 风格设计，通过纯 JSON 字符串交互。

**设计原则：前端可以换，协议不换。**

---

## 目录

- [1. 传输层](#1-传输层)
- [2. 消息格式](#2-消息格式)
- [3. 方法清单](#3-方法清单)
- [4. 事件清单](#4-事件清单)
- [5. 错误码](#5-错误码)
- [6. 平台边界](#6-平台边界)
- [7. 完整调用示例](#7-完整调用示例)
- [8. 版本与兼容性](#8-版本与兼容性)

---

## 1. 传输层

当前协议为本地调用设计,后端与前端的通信方式由各平台适配层决定：

| 平台 | 传输方式 | 说明 |
|------|----------|------|
| Android | 函数调用（同一进程） | 通过 `JsonRpcClient.call(json) → json` 直接调用 |
| Qt | 函数调用（同一进程） | 通过 `JsonRpcClient.call(json) → json` 直接调用 |
| Web / 外部前端 | HTTP / WebSocket | 可暴露本地 HTTP 端点，通过 POST 发送 JSON-RPC 请求 |

协议本身**不绑定传输层**，只要能收发 JSON 字符串即可。

---

## 2. 消息格式

### 2.1 请求

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "method": "network.start",
  "params": {
    "instance_name": "EasyTierET-x1",
    "network_name": "mynet",
    "network_secret": "mypass",
    "servers": ["tcp://225284.xyz:11010"]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `jsonrpc` | string | 是 | 固定为 `"2.0"` |
| `id` | string | 推荐 | 请求标识，响应会回传相同的 id。推荐格式 `"req-xxx"`。可为 `null`（Notification）|
| `method` | string | 是 | 要调用的方法名 |
| `params` | object | 否 | 方法参数，不传参用 `{}` |

### 2.2 成功响应

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "result": {
    "success": true,
    "instance_name": "EasyTierET-x1"
  }
}
```

### 2.3 错误响应

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "error": {
    "code": -32003,
    "message": "network start failed",
    "data": {
      "reason": "parse config failed"
    }
  }
}
```

### 2.4 标准 JSON-RPC 错误码

| code | 含义 | 说明 |
|------|------|------|
| -32700 | Parse error | 请求 JSON 语法错误 |
| -32600 | Invalid Request | 请求不是合法的 JSON-RPC 消息 |
| -32601 | Method not found | 方法名不存在 |
| -32602 | Invalid params | 参数无效 |
| -32603 | Internal error | 后端内部错误 |

---

## 3. 方法清单

协议共定义 **9 个方法**，按功能分组。

---

### 3.1 `backend.ping` — 后端心跳

检查后端引擎是否可用，同时返回版本号。

**请求参数：** 无（传 `{}`）

**响应：**

```json
{
  "ok": true,
  "backend_version": "2.x"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `ok` | boolean | 后端是否可用 |
| `backend_version` | string | 后端引擎版本号 |

---

### 3.2 `network.validate` — 校验配置

验证网络配置是否正确，不实际启动。

**请求参数：**

```json
{
  "instance_name": "EasyTierET-x1",
  "network_name": "mynet",
  "network_secret": "mypass",
  "servers": ["tcp://225284.xyz:11010"],
  "dhcp": true,
  "latency_first": false
}
```

**响应：**

```json
{
  "valid": true,
  "warnings": [],
  "errors": []
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `valid` | boolean | 配置是否有效 |
| `warnings` | string[] | 警告信息列表（非致命） |
| `errors` | string[] | 错误信息列表（致命） |

---

### 3.3 `network.start` — 启动网络

根据配置创建并启动一个网络实例。每个实例由 `instance_name` 唯一标识。

**请求参数：**

```json
{
  "instance_name": "EasyTierET-x1",
  "network_name": "mynet",
  "network_secret": "mypass",
  "servers": ["tcp://225284.xyz:11010"],
  "dhcp": true,
  "ipv4": "10.144.144.1/24",
  "latency_first": false,
  "no_tun": false,
  "use_smoltcp": false
}
```

**响应：**

```json
{
  "success": true,
  "instance_name": "EasyTierET-x1",
  "message": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | boolean | 是否成功启动 |
| `instance_name` | string | 实例名称 |
| `message` | string? | 失败时的错误描述 |

---

### 3.4 `network.stop` — 停止网络

停止指定的网络实例。

**请求参数：**

```json
{
  "instance_name": "EasyTierET-x1"
}
```

**响应：**

```json
{
  "success": true,
  "instance_name": "EasyTierET-x1",
  "message": null
}
```

---

### 3.5 `network.list` — 列举网络

列出所有运行中的网络实例。

**请求参数：** 无（传 `{}`）

**响应：**

```json
{
  "instances": [
    {
      "instance_name": "EasyTierET-x1",
      "network_name": "mynet",
      "running": true,
      "virtual_ipv4": "10.144.144.1",
      "peer_count": 3,
      "route_count": 5
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `instance_name` | string | 实例名称 |
| `network_name` | string? | 网络名称 |
| `running` | boolean | 是否运行中 |
| `virtual_ipv4` | string? | 分配的虚拟 IPv4 地址 |
| `peer_count` | integer | 对等节点数量 |
| `route_count` | integer | 路由条目数量 |

---

### 3.6 `network.state` — 查询网络状态

获取指定网络实例的详细状态，包括节点列表、路由信息和日志尾部。

**请求参数：**

```json
{
  "instance_name": "EasyTierET-x1"
}
```

**响应：**

```json
{
  "instance_name": "EasyTierET-x1",
  "running": true,
  "nodes": [
    {
      "key": "peer_id_123",
      "value": "{...原始 JSON...}"
    }
  ],
  "routes": [
    {
      "destination": "peer-hostname",
      "via": "peer_id_456",
      "proxy_cidrs": ["10.0.0.0/24"]
    }
  ],
  "log_tail": [
    {
      "timestamp": "2026-05-13T12:00:00Z",
      "level": "INFO",
      "message": "peer connected"
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `instance_name` | string | 实例名称 |
| `running` | boolean | 是否运行中 |
| `nodes` | NodeInfo[] | 对等节点列表（key 为 peer_id，value 为节点完整 JSON） |
| `routes` | RouteInfo[] | 路由列表 |
| `log_tail` | LogEntry[] | 最近的事件日志条目 |

---

### 3.7 `vpn.attach` — 绑定 TUN 设备

将 TUN 设备的文件描述符绑定到指定网络实例。**仅 Android 平台使用。**

**请求参数：**

```json
{
  "instance_name": "EasyTierET-x1",
  "tun_fd": 42
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `instance_name` | string | 实例名称 |
| `tun_fd` | integer | TUN 设备的文件描述符 |

**响应：**

```json
{
  "success": true,
  "instance_name": "EasyTierET-x1",
  "message": null
}
```

---

### 3.8 `vpn.detach` — 解绑 TUN 设备

将 TUN 设备从网络实例上解绑。**仅 Android 平台使用。**

**请求参数：**

```json
{
  "instance_name": "EasyTierET-x1"
}
```

**响应：**

```json
{
  "success": true,
  "instance_name": "EasyTierET-x1",
  "message": null
}
```

---

### 3.9 `log.subscribe` — 订阅日志

检查指定实例是否存在，并返回订阅通道信息。

**请求参数：**

```json
{
  "instance_name": "EasyTierET-x1",
  "tail": 16
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `instance_name` | string | 是 | 实例名称 |
| `tail` | integer | 否 | 日志获取行数上限，默认 16 |

**响应：**

```json
{
  "subscribed": true,
  "channel": "local:instance:EasyTierET-x1"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `subscribed` | boolean | 是否订阅成功 |
| `channel` | string? | 日志通道标识 |

---

## 4. 事件清单

事件用于后端→前端的主动推送。当前为协议预留定义，具体的事件通道机制由各平台适配层实现（如 Android 的协程通道、Qt 的信号槽）。

### 4.1 `network.instance_changed`

当实例状态（启动/停止）变化时触发。

```json
{
  "instance_name": "EasyTierET-x1",
  "action": "started",
  "state": {
    "instance_name": "EasyTierET-x1",
    "running": true
  }
}
```

### 4.2 `network.node_changed`

当节点列表变化时触发。

```json
{
  "instance_name": "EasyTierET-x1",
  "nodes": [
    {"key": "peer_1", "value": "{...}"}
  ]
}
```

### 4.3 `network.route_changed`

当路由表变化时触发。

```json
{
  "instance_name": "EasyTierET-x1",
  "routes": [
    {"destination": "peer-a", "via": "peer_id_1", "proxy_cidrs": []}
  ]
}
```

### 4.4 `network.log`

当有新日志时触发。

```json
{
  "instance_name": "EasyTierET-x1",
  "entry": {
    "timestamp": "2026-05-13T12:00:00Z",
    "level": "INFO",
    "message": "peer connected"
  }
}
```

---

## 5. 错误码

### 业务错误码（`-320xx` 范围）

| code | name | message | 说明 |
|------|------|---------|------|
| -32001 | `backend_unavailable` | backend is not available | 后端引擎未初始化或不可用 |
| -32002 | `validation_failed` | config validation failed | 配置校验失败 |
| -32003 | `network_start_failed` | network start failed | 网络启动失败 |
| -32004 | `network_stop_failed` | network stop failed | 网络停止失败 |

### 错误响应示例

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "error": {
    "code": -32002,
    "message": "config validation failed",
    "data": {
      "reason": "empty network_secret"
    }
  }
}
```

---

## 6. 平台边界

各平台只在自己的适配层处理平台特有逻辑：

| 能力 | Android | Qt | 共享后端 |
|------|---------|----|----------|
| VpnService 授权 | ✅ | ❌ | ❌ |
| TUN 文件描述符管理 | ✅ | ❌ | ❌ |
| 前台服务 / 保活 | ✅ | ❌ | ❌ |
| 桌面窗口 / 托盘 | ❌ | ✅ | ❌ |
| 配置文件位置管理 | ❌ | ✅ | ❌ |
| 网络实例生命周期 | ❌ | ❌ | ✅ |
| 配置校验 | ❌ | ❌ | ✅ |
| 状态查询 | ❌ | ❌ | ✅ |
| 节点 / 路由解析 | ❌ | ❌ | ✅ |

---

## 7. 完整调用示例

### 7.1 Android（Kotlin）—— 通过 Adapter 调用

```kotlin
// AndroidAdapter 封装了所有协议细节，前端只需调 typed API
val adapter = AndroidAdapter(jsonRpcClient, context)

// 1. ping 检查后端
val ping = adapter.ping()
if (!ping.ok) { /* 后端不可用 */ }

// 2. 配置并启动网络
val config = NetworkConfig().apply {
    instanceName = "EasyTierET-x1"
    networkName = "mynet"
    networkSecret = "mypass"
    servers = mutableListOf("tcp://225284.xyz:11010")
}
val result = adapter.startNetwork(config)
if (!result.success) { /* 启动失败 */ }

// 3. 绑定 TUN（VpnService 授权后）
adapter.vpnAttach("EasyTierET-x1", tunFd)

// 4. 监控节点
adapter.startMonitoring("EasyTierET-x1") { nodes ->
    // 每 3 秒回调一次节点列表
    nodes.forEach { node ->
        println("${node.hostname} @ ${node.virtualIp}")
    }
}
```

### 7.2 Android（Kotlin）—— 直接通过 JSON-RPC 调用

```kotlin
// 不经过 Adapter，直接发 JSON 字符串
val request = """
{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "network.start",
  "params": {
    "instance_name": "EasyTierET-x1",
    "network_name": "mynet",
    "network_secret": "mypass",
    "servers": ["tcp://225284.xyz:11010"]
  }
}
""".trimIndent()

val response = jsonRpcClient.call(request)
// response: {"jsonrpc":"2.0","id":"1","result":{"success":true,"instance_name":"EasyTierET-x1"}}
```

### 7.3 Qt（C++）—— 通过 Adapter 调用

```cpp
auto adapter = new QtAdapter(backend, this);

// 1. ping
auto ping = adapter.ping();
if (!ping.ok) { /* 后端不可用 */ }

// 2. 启动网络
adapter->startNetwork("EasyTierET-x1", tomlConfig);

// 3. 监听状态变化
connect(adapter, &QtAdapter::networkStarted, this,
    [](const QString &name, bool success) {
        if (success) {
            qDebug() << "网络已启动:" << name;
        }
    });

// 4. 查询状态
auto state = adapter->getNetworkState("EasyTierET-x1");
for (const auto &node : state.nodes) {
    qDebug() << "节点:" << QString::fromStdString(node.key);
}
```

### 7.4 Qt（C++）—— 直接通过 JSON-RPC 调用

```cpp
QString request = JsonRpcMessage::createRequest(
    "1", "network.start",
    {{"instance_name", "EasyTierET-x1"},
     {"network_name", "mynet"},
     {"network_secret", "mypass"}}
);

QString response = jsonRpcClient.call(request);
// response: {"jsonrpc":"2.0","id":"1","result":{"success":true,"instance_name":"EasyTierET-x1"}}
```

### 7.5 Web 前端（TypeScript）—— 通过 HTTP

```typescript
async function callBackend(method: string, params: object): Promise<any> {
  const res = await fetch('http://localhost:9876/rpc', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: crypto.randomUUID(),
      method,
      params
    })
  });
  const data = await res.json();
  if (data.error) throw new Error(data.error.message);
  return data.result;
}

// 使用
const ping = await callBackend('backend.ping', {});
console.log(ping.backend_version); // "2.x"

const result = await callBackend('network.start', {
  instance_name: 'Web-x1',
  network_name: 'mynet',
  network_secret: 'mypass',
  servers: ['tcp://225284.xyz:11010']
});
```

---

## 8. 版本与兼容性

### 协议版本

当前协议版本：**0.1.0**

版本号格式：`major.minor.patch`

| 版本号变更 | 含义 |
|-----------|------|
| major 增加 | 向后不兼容的协议变更（如删除/重命名方法、修改必填字段） |
| minor 增加 | 向后兼容的新增（如新增方法、新增可选字段） |
| patch 增加 | 不影响协议的修正（如文档修改、错误码描述修正） |

### 兼容性保证

- 新增方法不会影响现有方法
- 新增可选字段不会影响现有请求
- 响应中新增字段不会影响现有解析逻辑（consumer 应忽略未知字段）
- 删除/重命名方法需升 major 版本

### 机器可读定义

协议和能力的数据模型都有对应的机器可读 JSON Schema 文件：

| 文件 | 内容 |
|------|------|
| `protocol/easytier.rpc.json` | 协议入口：方法清单、事件清单、错误码、消息信封 |
| `core/easytier.models.json` | 所有请求/响应/事件的 JSON Schema 定义 |

这两个文件是协议的权威定义源，本文档是对它们的补充说明。
