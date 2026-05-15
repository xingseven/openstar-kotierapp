package com.easytier.data

import org.json.JSONObject

private data class ProxyNetworkTomlEntry(
    val cidr: String,
    val mappedCidr: String? = null,
)

private data class TomlImportState(
    var instanceName: String? = null,
    var hostname: String? = null,
    var dhcp: Boolean? = null,
    var ipv4: String? = null,
    var networkName: String? = null,
    var networkSecret: String? = null,
    val listeners: MutableList<String> = mutableListOf(),
    val peers: MutableList<String> = mutableListOf(),
    val proxyNetworks: MutableList<ProxyNetworkTomlEntry> = mutableListOf(),
    val routes: MutableList<String> = mutableListOf(),
    val exitNodes: MutableList<String> = mutableListOf(),
    val flags: MutableMap<String, String> = linkedMapOf(),
)

object NetworkConfigImport {
    fun fromText(content: String): NetworkConfig {
        val trimmed = content.trimStart()
        return if (trimmed.startsWith("{")) {
            NetworkConfig.fromJson(JSONObject(content))
        } else {
            fromToml(content)
        }
    }

    fun fromToml(content: String): NetworkConfig {
        val state = parseToml(content)
        val cfg = NetworkConfig()

        cfg.instanceName = state.instanceName?.takeIf { it.isNotBlank() } ?: NetworkConfig.generateInstanceName()
        cfg.networkLabel = cfg.networkLabel.ifBlank { state.networkName.orEmpty() }
        cfg.isRunning = false
        cfg.hostname = state.hostname.orEmpty()
        cfg.networkName = state.networkName.orEmpty()
        cfg.networkSecret = state.networkSecret.orEmpty()
        cfg.dhcp = state.dhcp ?: true
        cfg.ipv4 = state.ipv4.orEmpty().substringBefore('/')
        cfg.servers = NetworkConfig.normalizeServers(state.peers).toMutableList()
        cfg.listenAddresses = state.listeners.toMutableList().ifEmpty {
            mutableListOf("tcp://0.0.0.0:11010", "udp://0.0.0.0:11010")
        }
        cfg.customRoutes = state.routes.toMutableList()
        cfg.exitNodes = state.exitNodes.toMutableList()
        cfg.proxyNetworks = state.proxyNetworks.map { entry ->
            entry.mappedCidr?.let { "${entry.cidr}->${it}" } ?: entry.cidr
        }.toMutableList()

        cfg.defaultProtocol = NetworkConfig.normalizeDefaultProtocol(
            state.flags["default_protocol"].orEmpty().ifBlank { "tcp" }
        )
        cfg.encryptionAlgorithm = NetworkConfig.normalizeEncryptionAlgorithm(
            state.flags["encryption_algorithm"].orEmpty().ifBlank { "aes-gcm" }
        )
        cfg.latencyFirst = parseBoolean(state.flags["latency_first"], false)
        cfg.privateMode = parseBoolean(state.flags["private_mode"], false)
        cfg.noTun = parseBoolean(state.flags["no_tun"], false)
        cfg.enableQuicProxy = parseBoolean(state.flags["enable_quic_proxy"], false)
        cfg.disableQuicInput = parseBoolean(state.flags["disable_quic_input"], false)
        cfg.enableKcpProxy = parseBoolean(state.flags["enable_kcp_proxy"], false)
        cfg.disableKcpInput = parseBoolean(state.flags["disable_kcp_input"], false)
        cfg.disableRelayKcp = parseBoolean(state.flags["disable_relay_kcp"], false)
        cfg.disableRelayQuic = parseBoolean(state.flags["disable_relay_quic"], false)
        cfg.enableRelayForeignNetworkKcp = parseBoolean(state.flags["enable_relay_foreign_network_kcp"], false)
        cfg.enableRelayForeignNetworkQuic = parseBoolean(state.flags["enable_relay_foreign_network_quic"], false)
        cfg.disableP2p = parseBoolean(state.flags["disable_p2p"], false)
        cfg.p2pOnly = parseBoolean(state.flags["p2p_only"], false)
        cfg.lazyP2p = parseBoolean(state.flags["lazy_p2p"], false)
        cfg.needP2p = parseBoolean(state.flags["need_p2p"], false)
        cfg.multiThread = parseBoolean(state.flags["multi_thread"], true)
        cfg.useSmoltcp = parseBoolean(state.flags["use_smoltcp"], false)
        cfg.bindDevice = parseBoolean(state.flags["bind_device"], true)
        cfg.enableExitNode = parseBoolean(state.flags["enable_exit_node"], false)
        cfg.relayAllPeerRpc = parseBoolean(state.flags["relay_all_peer_rpc"], false)
        cfg.systemForwarding = parseBoolean(state.flags["proxy_forward_by_system"], false)
        cfg.enableEncryption = parseBoolean(state.flags["enable_encryption"], true)
        cfg.acceptDns = parseBoolean(state.flags["accept_dns"], false)
        cfg.disableUdpHolePunching = parseBoolean(state.flags["disable_udp_hole_punching"], false)
        cfg.disableTcpHolePunching = parseBoolean(state.flags["disable_tcp_hole_punching"], false)
        cfg.disableUpnp = parseBoolean(state.flags["disable_upnp"], false)
        cfg.disableSymHolePunching = parseBoolean(state.flags["disable_sym_hole_punching"], false)
        cfg.disableIpv6 = !parseBoolean(state.flags["enable_ipv6"], true)
        cfg.enableUdpBroadcastRelay = parseBoolean(state.flags["enable_udp_broadcast_relay"], false)
        cfg.devName = state.flags["dev_name"].orEmpty()
        cfg.mtu = state.flags["mtu"]?.toIntOrNull()?.takeIf { it in 1..1380 } ?: 0

        val relayWhitelistRaw = state.flags["relay_network_whitelist"]
            ?: state.flags["foreign_network_whitelist"]
        if (!relayWhitelistRaw.isNullOrBlank() && relayWhitelistRaw != "*") {
            cfg.foreignNetworkWhitelistEnabled = true
            cfg.foreignNetworkWhitelist = relayWhitelistRaw
                .split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
        } else {
            cfg.foreignNetworkWhitelistEnabled = false
            cfg.foreignNetworkWhitelist = mutableListOf()
        }

        return cfg
    }

    private fun parseToml(content: String): TomlImportState {
        val state = TomlImportState()
        var section = ""
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            var line = stripTomlComment(lines[i]).trim()
            if (line.isEmpty()) {
                i++
                continue
            }

            when (line) {
                "[network_identity]" -> {
                    section = "network_identity"
                    i++
                    continue
                }
                "[[peer]]" -> {
                    section = "peer"
                    i++
                    continue
                }
                "[[proxy_network]]" -> {
                    section = "proxy_network"
                    state.proxyNetworks.add(ProxyNetworkTomlEntry(cidr = ""))
                    i++
                    continue
                }
                "[flags]" -> {
                    section = "flags"
                    i++
                    continue
                }
            }

            if (!line.contains('=')) {
                i++
                continue
            }

            val key = line.substringBefore('=').trim()
            var value = line.substringAfter('=').trim()
            if (value.startsWith("[") && !value.contains("]")) {
                val builder = StringBuilder(value)
                while (!builder.contains("]") && i + 1 < lines.size) {
                    i++
                    builder.append('\n').append(stripTomlComment(lines[i]).trim())
                }
                value = builder.toString()
            }

            when (section) {
                "" -> when (key) {
                    "instance_name", "inst_name" -> state.instanceName = parseTomlString(value)
                    "hostname" -> state.hostname = parseTomlString(value)
                    "dhcp" -> state.dhcp = parseBoolean(value, true)
                    "ipv4", "virtual_ipv4" -> state.ipv4 = parseTomlString(value)
                    "listeners", "listener_urls" -> state.listeners.addAll(parseTomlStringArray(value))
                    "routes" -> state.routes.addAll(parseTomlStringArray(value))
                    "exit_nodes" -> state.exitNodes.addAll(parseTomlStringArray(value))
                    "network", "network_name" -> state.networkName = parseTomlString(value)
                    "network_secret" -> state.networkSecret = parseTomlString(value)
                }
                "network_identity" -> when (key) {
                    "network_name" -> state.networkName = parseTomlString(value)
                    "network_secret" -> state.networkSecret = parseTomlString(value)
                }
                "peer" -> if (key == "uri") {
                    state.peers.add(parseTomlString(value))
                }
                "proxy_network" -> if (state.proxyNetworks.isNotEmpty()) {
                    val lastIndex = state.proxyNetworks.lastIndex
                    val current = state.proxyNetworks[lastIndex]
                    when (key) {
                        "cidr" -> state.proxyNetworks[lastIndex] = current.copy(cidr = parseTomlString(value))
                        "mapped_cidr" -> state.proxyNetworks[lastIndex] = current.copy(mappedCidr = parseTomlString(value))
                    }
                }
                "flags" -> state.flags[key] = value.trim()
            }

            i++
        }

        state.proxyNetworks.removeAll { it.cidr.isBlank() }
        return state
    }

    private fun stripTomlComment(line: String): String {
        var inString = false
        val result = StringBuilder()
        var prev = '\u0000'
        for (ch in line) {
            if (ch == '"' && prev != '\\') {
                inString = !inString
            }
            if (!inString && ch == '#') {
                break
            }
            result.append(ch)
            prev = ch
        }
        return result.toString()
    }

    private fun parseTomlString(value: String): String {
        val trimmed = value.trim()
        return trimmed.removeSurrounding("\"").removeSurrounding("'").trim()
    }

    private fun parseTomlStringArray(value: String): List<String> {
        val trimmed = value.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return emptyList()
        }
        val inner = trimmed.substring(1, trimmed.length - 1)
        if (inner.isBlank()) return emptyList()
        return inner.split(',')
            .map { parseTomlString(it) }
            .filter { it.isNotEmpty() }
    }

    private fun parseBoolean(value: String?, default: Boolean): Boolean {
        return when (value?.trim()?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> default
        }
    }
}
