package com.easytier.backend

import com.easytier.data.NodeInfo
import org.json.JSONObject

fun redactTomlSecrets(toml: String): String {
    return toml.replace(Regex("""(?m)^(\s*network_secret\s*=\s*").*(")$"""), "$1***$2")
}

fun collectNodeInfosFromJson(jsonStr: String, instanceName: String): List<NodeInfo> {
    return try {
        val map = JSONObject(jsonStr).optJSONObject("map") ?: return emptyList()
        val nodes = mutableListOf<NodeInfo>()

        map.keys().forEach { key ->
            val value = map.optJSONObject(key) ?: return@forEach

            val nodeObj = value.optJSONObject("my_node_info")
            if (nodeObj != null) {
                val ipv4Inet = nodeObj.optJSONObject("virtual_ipv4")
                val virtualIp = parseIpv4InetToString(ipv4Inet)
                val hostname = nodeObj.optString("hostname", "")
                nodes.add(
                    NodeInfo(
                        hostname = hostname,
                        virtualIp = virtualIp,
                        isLocal = key == instanceName,
                    )
                )
            }

            val peers = value.optJSONArray("peers")
            val peerMap = mutableMapOf<Long, JSONObject>()
            if (peers != null) {
                for (i in 0 until peers.length()) {
                    val peer = peers.optJSONObject(i) ?: continue
                    val peerId = peer.optLong("peer_id", -1L)
                    if (peerId != -1L) peerMap[peerId] = peer
                }
            }

            val myPeerId = nodeObj?.optLong("peer_id", -1L) ?: -1L
            val routes = value.optJSONArray("routes")
            if (routes != null) {
                for (i in 0 until routes.length()) {
                    val route = routes.optJSONObject(i) ?: continue
                    val peerId = route.optLong("peer_id", -1L)
                    if (peerId == myPeerId || peerId == -1L) continue

                    val ipv4Addr = route.optJSONObject("ipv4_addr")
                    val virtualIp = parseIpv4InetToString(ipv4Addr)
                    val hostname = route.optString("hostname", "")
                    val latencyMs = route.optInt("path_latency", 0)

                    val featureFlag = route.optJSONObject("feature_flag")
                    val isPublicServer = featureFlag?.optBoolean("is_public_server", false) ?: false
                    val isDirectlyConnected = peerMap.containsKey(peerId)

                    val peer = peerMap[peerId]
                    var protocol = ""
                    var rxBytes = 0L
                    var txBytes = 0L
                    var lossRate = 0.0

                    if (peer != null) {
                        val conns = peer.optJSONArray("conns")
                        if (conns != null && conns.length() > 0) {
                            val conn = conns.optJSONObject(0)
                            if (conn != null) {
                                val tunnel = conn.optJSONObject("tunnel")
                                protocol = tunnel?.optString("tunnel_type", "")?.uppercase() ?: ""
                                rxBytes = conn.optLong("rx_bytes", 0)
                                txBytes = conn.optLong("tx_bytes", 0)
                                lossRate = conn.optDouble("loss_rate", 0.0)
                            }
                        }
                    }

                    nodes.add(
                        NodeInfo(
                            hostname = hostname.ifEmpty { "peer-$peerId" },
                            virtualIp = virtualIp,
                            isLocal = false,
                            latencyMs = latencyMs,
                            protocol = protocol,
                            connectionType = if (isPublicServer) "server" else if (isDirectlyConnected) "direct" else "relay",
                            rxBytes = rxBytes,
                            txBytes = txBytes,
                            lossRate = lossRate
                        )
                    )
                }
            }
        }

        nodes
    } catch (_: Exception) {
        emptyList()
    }
}

fun collectRunningInstanceNames(jsonStr: String): Set<String> {
    return try {
        val map = JSONObject(jsonStr).optJSONObject("map") ?: return emptySet()
        buildSet {
            map.keys().forEach { key -> add(key) }
        }
    } catch (_: Exception) {
        emptySet()
    }
}

fun collectProxyCidrsFromJson(jsonStr: String, instanceName: String): List<String> {
    return try {
        val map = JSONObject(jsonStr).optJSONObject("map") ?: return emptyList()
        val inst = map.optJSONObject(instanceName) ?: return emptyList()
        val routesArr = inst.optJSONArray("routes") ?: return emptyList()

        val routes = mutableListOf<String>()
        for (i in 0 until routesArr.length()) {
            val route = routesArr.optJSONObject(i) ?: continue
            val cidrs = route.optJSONArray("proxy_cidrs") ?: continue
            for (j in 0 until cidrs.length()) {
                val cidr = cidrs.optString(j, "").trim()
                if (cidr.isNotEmpty()) routes.add(cidr)
            }
        }

        routes
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseIpv4InetToString(inet: JSONObject?): String {
    if (inet == null) return ""
    val addrObj = inet.optJSONObject("address")
    val rawAddr = addrObj?.optLong("addr", 0) ?: inet.optLong("address", 0)
    if (rawAddr == 0L) return ""
    return intToIp(rawAddr.toInt())
}

private fun intToIp(addr: Int): String {
    return "${(addr shr 24) and 0xFF}.${(addr shr 16) and 0xFF}.${(addr shr 8) and 0xFF}.${addr and 0xFF}"
}