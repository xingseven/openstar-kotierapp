# 局域网互访问题排查记录

## 背景

用户反馈的现象是：

1. 手机和 PC 已进入同一 EasyTier 网络。
2. 手机和 PC 节点在运行态里能互相看见。
3. 双方虚拟 IP 可以互通，说明基础组网已成功。
4. 但双方各自后面的局域网设备没有正常显示，或者无法通过对方的局域网段访问到设备。

这类现象说明问题不在“是否进网”，而在“局域网段是否被正确发布和安装”。

## 先说结论

- PC 端“能看到对方局域网”并不是自动扫描出来的，而是依赖对端把自己的子网作为 `proxy_network` 发布出去。
- 运行态里真正传播给其他节点的是 `routes[*].proxy_cidrs`。
- Android 旧实现里，页面填写的高级网络配置在 `AndroidAdapter -> JsonRpcClient` 这条 JSON-RPC 边界上被截断了，只传了少量基础字段。
- 因此 Android 虽然能作为普通节点入网，但经常不会把自己的局域网段真正发布给后端，其他节点自然也拿不到这条 LAN 路由。
- 这条问题修复后，LAN 路由可以正常发布；但 Android 受 `VpnService` 限制，mDNS、SSDP、广播发现仍不一定可靠，所以验收时应优先测试直连局域网 IP，而不是只看“网络邻居”或发现列表。

## PC 端原理

### 配置层

PC 端的配置对象在 `qt-easy-tier-master/SRC/networkconf.cpp` 中生成 TOML。

其中：

- `m_proxyNetworks` 会输出为 `[[proxy_network]]`。
- `m_customRoutes` 会输出为 `routes = [...]`。

这两个概念不同：

- `proxy_network` 表示“把本机后面的局域网段代理出去，供其他节点访问”。
- `routes` 表示“本机自己需要导入 VPN 的额外路由”。

也就是说，真正决定“别人能不能看到我后面的 LAN”的，是 `proxy_network`，不是普通 `routes`。

### 运行态层

PC 端运行态解析在 `qt-easy-tier-master/SRC/backend/RuntimeInfoParser.cpp`。

它会读取运行态 JSON 里的 `routes`。每条 route 里如果带有 `proxy_cidrs`，就说明某个节点对外发布了自己的局域网段。

因此 PC 端看到对方 LAN 的逻辑链路是：

```text
对端配置 proxy_network
  -> EasyTier 运行态生成 route.proxy_cidrs
  -> 本端读取 routes
  -> 本端获得对端 LAN 路由
```

## Android 旧问题链路

Android split 版的实际启动链路是：

```text
UI Page
  -> EasyTierService
  -> AndroidAdapter
  -> JsonRpcClient
  -> BackendClient
  -> JniBackendClient
  -> NetworkConfig.toToml()
  -> EasyTierJNI
```

问题出在 `AndroidAdapter` 和 `backend/JsonRpcClient` 之间。

旧实现只传了这些字段：

- `instance_name`
- `network_name`
- `network_secret`
- `servers`
- `dhcp`
- `latency_first`
- `no_tun`
- `use_smoltcp`
- `ipv4`

这意味着下面这些在页面里已经填写和保存的配置，启动时会被静默丢掉：

- `proxy_networks`
- `custom_routes`
- `system_forwarding`
- `enable_udp_broadcast_relay`
- `foreign_network_whitelist`
- `listen_addresses`
- 以及其他高级开关字段

于是就出现了一个典型错觉：

- 页面里看起来已经配置了“代理网络 CIDR”。
- 但真正启动网络实例时，这个字段没有进入 backend，也没有进入最终 TOML。
- 最终效果就是“节点互通，但对方 LAN 根本没有被发布出来”。

## 本次修复

本次修复做了两件事：

### 1. AndroidAdapter 改为发送完整配置

`android_backup/app/src/main/java/com/easytier/backend/AndroidAdapter.kt`

旧逻辑是手写 JSON，只挑少数字段发给 backend。

新逻辑改为直接发送：

```kotlin
JSONObject(config.toJson()).toString()
```

这样页面侧已有的完整 `NetworkConfig` 会完整进入 JSON-RPC 边界。

### 2. JsonRpcClient 改为按完整配置反序列化

`backend/src/main/java/com/easytier/backend/JsonRpcClient.kt`

旧逻辑是手工从 `params` 中读少量字段。

新逻辑改为直接：

```kotlin
NetworkConfig.fromJson(params)
```

这样 `proxy_networks`、`custom_routes`、`system_forwarding`、`enable_udp_broadcast_relay` 等字段都能被恢复，再由 `NetworkConfig.toToml()` 正确输出到 EasyTier 配置中。

## 修复后的影响

### 已解决的部分

- Android 端现在可以正确把 `proxy_networks` 传到 backend。
- backend 可以按完整配置生成 TOML。
- 其他节点可以正常收到 Android 发布出来的 `proxy_cidrs`。
- “节点互通但 LAN 路由压根没发布” 这条根因已经修掉。

### 仍然存在的边界

- Android 系统对广播发现流量的处理仍有限制。
- 即使路由已经正确安装，某些依赖 mDNS、SSDP、广播包的应用，仍可能无法像传统局域网那样稳定出现在设备发现列表中。
- 因此不要把“发现列表里没出现设备”直接等同于“LAN 路由没通”。

## 推荐验证方法

1. 手机和 PC 两端分别在配置里填写各自真实的局域网网段到“代理网络 CIDR”。
2. 重新启动组网实例，确保新配置实际生效。
3. 检查运行态里是否已经出现对端发布的 `proxy_cidrs`。
4. 优先测试直接访问对端局域网 IP，例如 ping 某台内网设备或直接访问其服务端口。
5. 不要只依赖“网络邻居”“局域网发现列表”“设备扫描页”来判断是否成功。

## 涉及文件

- `android_backup/app/src/main/java/com/easytier/backend/AndroidAdapter.kt`
- `backend/src/main/java/com/easytier/backend/JsonRpcClient.kt`
- `backend/src/main/java/com/easytier/data/NetworkConfig.kt`
- `qt-easy-tier-master/SRC/networkconf.cpp`
- `qt-easy-tier-master/SRC/backend/RuntimeInfoParser.cpp`

## 一句话总结

这次问题的核心，不是手机没有进网，而是 Android 端在启动实例时把高级 LAN 路由配置丢在了 JSON-RPC 边界上，导致局域网段没有真正发布出去。