package com.easytier.data

import org.json.JSONArray
import org.json.JSONObject

import kotlin.random.Random

data class NetworkConfig(
    var instanceName: String = generateInstanceName(),
    var networkLabel: String = "",
    var isRunning: Boolean = false,
    var deviceType: String = "desktop",

    var hostname: String = "",
    var networkName: String = "",
    var networkSecret: String = "",
    var dhcp: Boolean = true,
    var ipv4: String = "",
    var latencyFirst: Boolean = false,
    var privateMode: Boolean = true,
    var servers: MutableList<String> = defaultServers().toMutableList(),

    var enableKcpProxy: Boolean = true,
    var disableKcpInput: Boolean = false,
    var noTun: Boolean = false, // 设为 false 才会触发 VPN 服务创建 TUN
    var enableQuicProxy: Boolean = false,
    var disableQuicInput: Boolean = false,
    var disableRelayKcp: Boolean = false,
    var disableRelayQuic: Boolean = false,
    var enableRelayForeignNetworkKcp: Boolean = false,
    var enableRelayForeignNetworkQuic: Boolean = false,
    var disableUdpHolePunching: Boolean = false,
    var disableTcpHolePunching: Boolean = false,
    var disableUpnp: Boolean = false,
    var needP2p: Boolean = false,
    var lazyP2p: Boolean = false,
    var p2pOnly: Boolean = false,
    var multiThread: Boolean = true,
    var useSmoltcp: Boolean = false,
    var devName: String = "",
    var mtu: Int = 0,
    var bindDevice: Boolean = true,
    var disableP2p: Boolean = false,
    var enableExitNode: Boolean = false,
    var systemForwarding: Boolean = false,
    var disableSymHolePunching: Boolean = false,
    var disableIpv6: Boolean = false,
    var relayAllPeerRpc: Boolean = false,
    var enableEncryption: Boolean = true,
    var acceptDns: Boolean = false,
    var enableUdpBroadcastRelay: Boolean = true,
    var defaultProtocol: String = "",
    var encryptionAlgorithm: String = "aes-gcm",

    var foreignNetworkWhitelistEnabled: Boolean = false,
    var foreignNetworkWhitelist: MutableList<String> = mutableListOf(),
    var listenAddresses: MutableList<String> =
        mutableListOf("tcp://0.0.0.0:11010", "udp://0.0.0.0:11010"),
    var proxyNetworks: MutableList<String> = mutableListOf(),
    var customRoutes: MutableList<String> = mutableListOf(),
    var exitNodes: MutableList<String> = mutableListOf()
) {
    fun toToml(): String = buildString {
        appendLine("""instance_name = "$instanceName"""")
        appendLine("""hostname = "$hostname"""")
        appendLine("dhcp = ${if (dhcp) "true" else "false"}")
        if (ipv4.isNotEmpty() && !dhcp) {
            appendLine("""ipv4 = "${normalizeStaticIpv4(ipv4)}"""")
        }

        if (listenAddresses.isNotEmpty()) {
            appendLine()
            appendLine("listeners = [")
            listenAddresses.forEach { appendLine(""""$it",""") }
            appendLine("]")
        }

        if (customRoutes.isNotEmpty()) {
            appendLine()
            appendLine("routes = [")
            customRoutes.forEach { appendLine(""""$it",""") }
            appendLine("]")
        }

        if (exitNodes.isNotEmpty()) {
            appendLine()
            appendLine("exit_nodes = [")
            exitNodes.forEach { appendLine(""""$it",""") }
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
        if (devName.isNotEmpty()) {
            appendLine("""dev_name = "$devName"""")
        }
        if (mtu > 0) {
            appendLine("mtu = $mtu")
        }

        if (foreignNetworkWhitelistEnabled) {
            appendLine("""foreign_network_whitelist = "${foreignNetworkWhitelist.joinToString(" ")}"""")
        }

        appendLine("enable_quic_proxy = ${if (enableQuicProxy) "true" else "false"}")
        appendLine("disable_quic_input = ${if (disableQuicInput) "true" else "false"}")
        appendLine("enable_kcp_proxy = ${if (enableKcpProxy) "true" else "false"}")
        appendLine("disable_kcp_input = ${if (disableKcpInput) "true" else "false"}")
        appendLine("disable_relay_kcp = ${if (disableRelayKcp) "true" else "false"}")
        appendLine("disable_relay_quic = ${if (disableRelayQuic) "true" else "false"}")
        appendLine("enable_relay_foreign_network_kcp = ${if (enableRelayForeignNetworkKcp) "true" else "false"}")
        appendLine("enable_relay_foreign_network_quic = ${if (enableRelayForeignNetworkQuic) "true" else "false"}")
        appendLine("bind_device = ${if (bindDevice) "true" else "false"}")
        appendLine("private_mode = ${if (privateMode) "true" else "false"}")
        appendLine("disable_p2p = ${if (disableP2p) "true" else "false"}")
        appendLine("need_p2p = ${if (needP2p) "true" else "false"}")
        appendLine("lazy_p2p = ${if (lazyP2p) "true" else "false"}")
        appendLine("p2p_only = ${if (p2pOnly) "true" else "false"}")
        appendLine("multi_thread = ${if (multiThread) "true" else "false"}")
        appendLine("accept_dns = ${if (acceptDns) "true" else "false"}")
        appendLine("enable_udp_broadcast_relay = ${if (enableUdpBroadcastRelay) "true" else "false"}")
        appendLine("disable_sym_hole_punching = ${if (disableSymHolePunching) "true" else "false"}")
        appendLine("relay_all_peer_rpc = ${if (relayAllPeerRpc) "true" else "false"}")
        appendLine("disable_udp_hole_punching = ${if (disableUdpHolePunching) "true" else "false"}")
        appendLine("disable_tcp_hole_punching = ${if (disableTcpHolePunching) "true" else "false"}")
        appendLine("disable_upnp = ${if (disableUpnp) "true" else "false"}")
        appendLine("proxy_forward_by_system = ${if (systemForwarding) "true" else "false"}")
        if (defaultProtocol.isNotEmpty()) {
            appendLine("""default_protocol = "$defaultProtocol"""")
        }
        appendLine("""encryption_algorithm = "$encryptionAlgorithm"""")
    }

    fun toJson(): Map<String, Any?> = mapOf(
        "instance_name" to instanceName,
        "network_label" to networkLabel,
        "is_running" to isRunning,
        "device_type" to deviceType,
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
        "disable_relay_kcp" to disableRelayKcp,
        "disable_relay_quic" to disableRelayQuic,
        "enable_relay_foreign_network_kcp" to enableRelayForeignNetworkKcp,
        "enable_relay_foreign_network_quic" to enableRelayForeignNetworkQuic,
        "disable_udp_hole_punching" to disableUdpHolePunching,
        "disable_tcp_hole_punching" to disableTcpHolePunching,
        "disable_upnp" to disableUpnp,
        "need_p2p" to needP2p,
        "lazy_p2p" to lazyP2p,
        "p2p_only" to p2pOnly,
        "multi_thread" to multiThread,
        "use_smoltcp" to useSmoltcp,
        "dev_name" to devName,
        "mtu" to mtu,
        "bind_device" to bindDevice,
        "disable_p2p" to disableP2p,
        "enable_exit_node" to enableExitNode,
        "system_forwarding" to systemForwarding,
        "disable_sym_hole_punching" to disableSymHolePunching,
        "disable_ipv6" to disableIpv6,
        "relay_all_peer_rpc" to relayAllPeerRpc,
        "enable_encryption" to enableEncryption,
        "accept_dns" to acceptDns,
        "enable_udp_broadcast_relay" to enableUdpBroadcastRelay,
        "default_protocol" to defaultProtocol,
        "encryption_algorithm" to encryptionAlgorithm,
        "foreign_network_whitelist_enabled" to foreignNetworkWhitelistEnabled,
        "foreign_network_whitelist" to foreignNetworkWhitelist,
        "listen_addresses" to listenAddresses,
        "proxy_networks" to proxyNetworks,
        "custom_routes" to customRoutes,
        "exit_nodes" to exitNodes
    )

    companion object {
        private const val PREFIX = "EasyTierET-"
        private const val ONE_CLICK_INSTANCE_NAME = "QtET-OneClick"
        private const val LEGACY_PUBLIC_SERVER = "wss://qtet-public.070219.xyz"
        private const val DEPRECATED_DEFAULT_SERVER = "tcp://183.230.36.171:11010"
        private val DEFAULT_SERVERS = listOf(
            "tcp://225284.xyz:11010"
        )
        private val ONE_CLICK_SERVERS = listOf(
            "tcp://us01.225284.xyz:11010",
            "tcp://225284.xyz:11010",
            "tcp://sjc1.clusters.zeabur.com:27773"
        )

        fun defaultServers(): List<String> = DEFAULT_SERVERS

        fun oneClickServers(): List<String> = ONE_CLICK_SERVERS

        fun oneClickInstanceName(): String = ONE_CLICK_INSTANCE_NAME

        val SUPPORTED_DEFAULT_PROTOCOLS: Set<String> = setOf(
            "",
            "udp",
            "tcp",
            "wg",
            "ws",
            "wss"
        )

        val SUPPORTED_ENCRYPTION_ALGORITHMS: Set<String> = setOf(
            "aes-gcm",
            "xor",
            "chacha20",
            "aes-gcm-256",
            "openssl-aes128-gcm",
            "openssl-aes256-gcm",
            "openssl-chacha20"
        )

        fun normalizeDefaultProtocol(value: String): String {
            val normalized = value.trim().lowercase()
            return if (normalized in SUPPORTED_DEFAULT_PROTOCOLS) normalized else ""
        }

        fun normalizeEncryptionAlgorithm(value: String): String {
            val normalized = value.trim().lowercase()
            return if (normalized in SUPPORTED_ENCRYPTION_ALGORITHMS) normalized else "aes-gcm"
        }

        fun fromJson(obj: JSONObject): NetworkConfig {
            return NetworkConfig(
                instanceName = obj.optString("instance_name", generateInstanceName()),
                networkLabel = obj.optString("network_label", ""),
                isRunning = false,
                deviceType = obj.optString("device_type", "desktop").ifBlank { "desktop" },
                hostname = obj.optString("hostname", ""),
                networkName = obj.optString("network_name", ""),
                networkSecret = obj.optString("network_secret", ""),
                dhcp = obj.optBoolean("dhcp", true),
                ipv4 = obj.optString("ipv4", ""),
                latencyFirst = obj.optBoolean("latency_first", false),
                privateMode = obj.optBoolean("private_mode", true),
                servers = normalizeServers(jsonArrayToStringList(obj.optJSONArray("servers"), defaultServers())),
                enableKcpProxy = obj.optBoolean("enable_kcp_proxy", true),
                disableKcpInput = obj.optBoolean("disable_kcp_input", false),
                noTun = obj.optBoolean("no_tun", false),
                enableQuicProxy = obj.optBoolean("enable_quic_proxy", false),
                disableQuicInput = obj.optBoolean("disable_quic_input", false),
                disableRelayKcp = obj.optBoolean("disable_relay_kcp", false),
                disableRelayQuic = obj.optBoolean("disable_relay_quic", false),
                enableRelayForeignNetworkKcp = obj.optBoolean("enable_relay_foreign_network_kcp", false),
                enableRelayForeignNetworkQuic = obj.optBoolean("enable_relay_foreign_network_quic", false),
                disableUdpHolePunching = obj.optBoolean("disable_udp_hole_punching", false),
                disableTcpHolePunching = obj.optBoolean("disable_tcp_hole_punching", false),
                disableUpnp = obj.optBoolean("disable_upnp", false),
                needP2p = obj.optBoolean("need_p2p", false),
                lazyP2p = obj.optBoolean("lazy_p2p", false),
                p2pOnly = obj.optBoolean("p2p_only", false),
                multiThread = obj.optBoolean("multi_thread", true),
                useSmoltcp = obj.optBoolean("use_smoltcp", false),
                devName = obj.optString("dev_name", ""),
                mtu = obj.optInt("mtu", 0).takeIf { it in 1..1380 } ?: 0,
                bindDevice = obj.optBoolean("bind_device", true),
                disableP2p = obj.optBoolean("disable_p2p", false),
                enableExitNode = obj.optBoolean("enable_exit_node", false),
                systemForwarding = obj.optBoolean("system_forwarding", false),
                disableSymHolePunching = obj.optBoolean("disable_sym_hole_punching", false),
                disableIpv6 = obj.optBoolean("disable_ipv6", false),
                relayAllPeerRpc = obj.optBoolean("relay_all_peer_rpc", false),
                enableEncryption = obj.optBoolean("enable_encryption", true),
                acceptDns = obj.optBoolean("accept_dns", false),
                enableUdpBroadcastRelay = obj.optBoolean("enable_udp_broadcast_relay", true),
                defaultProtocol = normalizeDefaultProtocol(obj.optString("default_protocol", "")),
                encryptionAlgorithm = normalizeEncryptionAlgorithm(obj.optString("encryption_algorithm", "aes-gcm")),
                foreignNetworkWhitelistEnabled = obj.optBoolean("foreign_network_whitelist_enabled", false),
                foreignNetworkWhitelist = jsonArrayToStringList(obj.optJSONArray("foreign_network_whitelist")),
                listenAddresses = jsonArrayToStringList(
                    obj.optJSONArray("listen_addresses"),
                    listOf("tcp://0.0.0.0:11010", "udp://0.0.0.0:11010")
                ),
                proxyNetworks = jsonArrayToStringList(obj.optJSONArray("proxy_networks")),
                customRoutes = jsonArrayToStringList(obj.optJSONArray("custom_routes")),
                exitNodes = jsonArrayToStringList(obj.optJSONArray("exit_nodes")),
            )
        }

        fun normalizeServers(servers: List<String>): MutableList<String> {
            val normalized = servers.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val legacyDefaultLike = normalized.isNotEmpty() && normalized.all {
                it == DEFAULT_SERVERS.first() || isDeprecatedDefaultServer(it)
            }

            if (normalized.isEmpty()) {
                return DEFAULT_SERVERS.toMutableList()
            }

            if (normalized.size == 1 && normalized[0] == LEGACY_PUBLIC_SERVER) {
                return DEFAULT_SERVERS.toMutableList()
            }

            if (legacyDefaultLike) {
                return DEFAULT_SERVERS.toMutableList()
            }

            return normalized.toMutableList()
        }

        fun isDeprecatedDefaultServer(url: String): Boolean = url.trim() == DEPRECATED_DEFAULT_SERVER

        fun generateInstanceName(): String {
            val suffix = (1..10).map { "0123456789abcdefghijklmnopqrstuvwxyz"[Random.nextInt(36)] }
                .joinToString("")
            return "$PREFIX$suffix"
        }

        fun normalizeStaticIpv4(value: String): String {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) {
                return ""
            }
            return if ('/' in trimmed) trimmed else "$trimmed/24"
        }

        fun vpnIpv4Address(value: String): String {
            return normalizeStaticIpv4(value).substringBefore('/')
        }

        fun createOneClickHostConfig(networkName: String, networkSecret: String): NetworkConfig {
            return NetworkConfig(
                instanceName = oneClickInstanceName(),
                hostname = "host",
                networkName = networkName,
                networkSecret = networkSecret,
                dhcp = false,
                privateMode = true,
                ipv4 = "11.45.14.1",
                servers = oneClickServers().toMutableList(),
            )
        }

        fun createOneClickGuestConfig(networkName: String, networkSecret: String): NetworkConfig {
            return NetworkConfig(
                instanceName = oneClickInstanceName(),
                hostname = "guest",
                networkName = networkName,
                networkSecret = networkSecret,
                dhcp = true,
                privateMode = true,
                servers = oneClickServers().toMutableList()
            )
        }

        private fun generateRandomSuffix(length: Int): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }

        private fun jsonArrayToStringList(array: JSONArray?, default: List<String> = emptyList()): MutableList<String> {
            if (array == null) return default.toMutableList()

            val values = mutableListOf<String>()
            for (index in 0 until array.length()) {
                val value = array.optString(index, "").trim()
                if (value.isNotEmpty()) values.add(value)
            }

            return if (values.isEmpty()) default.toMutableList() else values
        }
    }
}
