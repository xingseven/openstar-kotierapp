package com.easytier.data

import kotlin.random.Random

data class NetworkConfig(
    var instanceName: String = generateInstanceName(),
    var networkLabel: String = "",
    var isRunning: Boolean = false,

    var hostname: String = "",
    var networkName: String = "",
    var networkSecret: String = "",
    var dhcp: Boolean = true,
    var ipv4: String = "",
    var latencyFirst: Boolean = false,
    var privateMode: Boolean = true,
    var servers: MutableList<String> = mutableListOf("wss://qtet-public.070219.xyz"),

    var enableKcpProxy: Boolean = true,
    var disableKcpInput: Boolean = false,
    var noTun: Boolean = false, // 设为 false 才会触发 VPN 服务创建 TUN
    var enableQuicProxy: Boolean = false,
    var disableQuicInput: Boolean = false,
    var disableUdpHolePunching: Boolean = false,
    var multiThread: Boolean = true,
    var useSmoltcp: Boolean = false,
    var bindDevice: Boolean = true,
    var disableP2p: Boolean = false,
    var enableExitNode: Boolean = false,
    var systemForwarding: Boolean = false,
    var disableSymHolePunching: Boolean = false,
    var disableIpv6: Boolean = false,
    var relayAllPeerRpc: Boolean = false,
    var enableEncryption: Boolean = true,
    var acceptDns: Boolean = false,

    var foreignNetworkWhitelistEnabled: Boolean = false,
    var foreignNetworkWhitelist: MutableList<String> = mutableListOf(),
    var listenAddresses: MutableList<String> =
        mutableListOf("tcp://0.0.0.0:11010", "udp://0.0.0.0:11010"),
    var proxyNetworks: MutableList<String> = mutableListOf()
) {
    fun toToml(): String = buildString {
        appendLine("""instance_name = "$instanceName"""")
        appendLine("""hostname = "$hostname"""")
        appendLine("dhcp = ${if (dhcp) "true" else "false"}")
        if (ipv4.isNotEmpty() && !dhcp) {
            appendLine("""ipv4 = "$ipv4"""")
        }

        if (listenAddresses.isNotEmpty()) {
            appendLine()
            appendLine("listeners = [")
            listenAddresses.forEach { appendLine(""""$it",""") }
            appendLine("]")
        }

        appendLine()
        appendLine("[network_identity]")
        appendLine("""network_name = "$networkName"""")
        appendLine("""network_secret = "$networkSecret"""")

        servers.forEach { server ->
            appendLine()
            appendLine("[[peer]]")
            appendLine("""uri = "$server"""")
        }

        proxyNetworks.forEach { cidr ->
            appendLine()
            appendLine("[[proxy_network]]")
            appendLine("""cidr = "$cidr"""")
        }

        appendLine()
        appendLine("[flags]")
        appendLine("enable_encryption = ${if (enableEncryption) "true" else "false"}")
        appendLine("enable_ipv6 = ${if (!disableIpv6) "true" else "false"}")
        appendLine("latency_first = ${if (latencyFirst) "true" else "false"}")
        appendLine("enable_exit_node = ${if (enableExitNode) "true" else "false"}")
        appendLine("no_tun = ${if (noTun) "true" else "false"}")
        appendLine("use_smoltcp = ${if (useSmoltcp) "true" else "false"}")

        if (foreignNetworkWhitelistEnabled) {
            appendLine("""foreign_network_whitelist = "${foreignNetworkWhitelist.joinToString(" ")}"""")
        }

        appendLine("enable_quic_proxy = ${if (enableQuicProxy) "true" else "false"}")
        appendLine("disable_quic_input = ${if (disableQuicInput) "true" else "false"}")
        appendLine("enable_kcp_proxy = ${if (enableKcpProxy) "true" else "false"}")
        appendLine("disable_kcp_input = ${if (disableKcpInput) "true" else "false"}")
        appendLine("bind_device = ${if (bindDevice) "true" else "false"}")
        appendLine("private_mode = ${if (privateMode) "true" else "false"}")
        appendLine("disable_p2p = ${if (disableP2p) "true" else "false"}")
        appendLine("multi_thread = ${if (multiThread) "true" else "false"}")
        appendLine("accept_dns = ${if (acceptDns) "true" else "false"}")
        appendLine("disable_sym_hole_punching = ${if (disableSymHolePunching) "true" else "false"}")
        appendLine("relay_all_peer_rpc = ${if (relayAllPeerRpc) "true" else "false"}")
        appendLine("disable_udp_hole_punching = ${if (disableUdpHolePunching) "true" else "false"}")
        appendLine("proxy_forward_by_system = ${if (systemForwarding) "true" else "false"}")
    }

    fun toJson(): Map<String, Any?> = mapOf(
        "instance_name" to instanceName,
        "network_label" to networkLabel,
        "is_running" to isRunning,
        "hostname" to hostname,
        "network_name" to networkName,
        "network_secret" to networkSecret,
        "dhcp" to dhcp,
        "ipv4" to ipv4,
        "latency_first" to latencyFirst,
        "private_mode" to privateMode,
        "servers" to servers,
        "enable_kcp_proxy" to enableKcpProxy,
        "disable_kcp_input" to disableKcpInput,
        "no_tun" to noTun,
        "enable_quic_proxy" to enableQuicProxy,
        "disable_quic_input" to disableQuicInput,
        "disable_udp_hole_punching" to disableUdpHolePunching,
        "multi_thread" to multiThread,
        "use_smoltcp" to useSmoltcp,
        "bind_device" to bindDevice,
        "disable_p2p" to disableP2p,
        "enable_exit_node" to enableExitNode,
        "system_forwarding" to systemForwarding,
        "disable_sym_hole_punching" to disableSymHolePunching,
        "disable_ipv6" to disableIpv6,
        "relay_all_peer_rpc" to relayAllPeerRpc,
        "enable_encryption" to enableEncryption,
        "accept_dns" to acceptDns,
        "foreign_network_whitelist_enabled" to foreignNetworkWhitelistEnabled,
        "foreign_network_whitelist" to foreignNetworkWhitelist,
        "listen_addresses" to listenAddresses,
        "proxy_networks" to proxyNetworks
    )

    companion object {
        private const val PREFIX = "EasyTierET-"

        fun generateInstanceName(): String {
            val suffix = (1..10).map { "0123456789abcdefghijklmnopqrstuvwxyz"[Random.nextInt(36)] }
                .joinToString("")
            return "$PREFIX$suffix"
        }
    }
}
