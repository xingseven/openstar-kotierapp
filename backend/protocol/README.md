# Protocol

统一后端协议建议采用 JSON-RPC 风格的本地调用协议。

当前协议已经有机器可读文件：`easytier.rpc.json`。它是协议入口，`../core/easytier.models.json` 则放共享模型。

这样做的原因有两个：

1. 现有代码已经大量使用 JSON 表示配置和状态，迁移成本低。
2. 前端语言不同也没关系，只要会发 JSON，就能接同一套后端。

## 建议的消息外形

### 请求

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "method": "network.start",
  "params": {
    "instance_name": "EasyTierET-x1",
    "network_name": "qinan12",
    "network_secret": "qinan1212.",
    "servers": ["tcp://225284.xyz:11010", "tcp://183.230.36.171:11010"]
  }
}
```

### 响应

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

### 错误

```json
{
  "jsonrpc": "2.0",
  "id": "req-001",
  "error": {
    "code": -32001,
    "message": "network start failed",
    "data": {
      "reason": "parse config failed"
    }
  }
}
```

## 先收敛的能力

- `backend.ping`
- `network.validate`
- `network.start`
- `network.stop`
- `network.list`
- `network.state`
- `vpn.attach`
- `vpn.detach`
- `log.subscribe`

## 事件流

后续如果要做实时节点刷新，建议单独给事件通道，不把轮询塞进 UI。

推荐事件：

- `network.instance_changed`
- `network.node_changed`
- `network.route_changed`
- `network.log`

## 平台边界

- Android 负责 VpnService、TUN 文件描述符、权限申请。
- Qt 负责桌面窗口、配置文件位置、托盘和桌面交互。
- 共享后端只关心配置、状态、生命周期和协议，不直接碰 UI。