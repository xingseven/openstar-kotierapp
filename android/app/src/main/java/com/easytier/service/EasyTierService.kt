package com.easytier.service

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import com.easytier.jni.EasyTierJNI
import kotlinx.coroutines.*
import org.json.JSONObject

/** EasyTier 核心服务 —— 封装 JNI 调用 + VPN 管理 + 监控 */
object EasyTierService {
    private const val TAG = "EasyTierService"
    private const val VPN_REQUEST_CODE = 10001
    private const val MONITOR_INTERVAL = 3000L

    private var initialized = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    data class EasyTierResult(val success: Boolean, val errorMessage: String = "") {
        companion object {
            fun ok() = EasyTierResult(true)
            fun fail(msg: String) = EasyTierResult(false, msg)
        }
    }

    fun initialize(): Boolean {
        return try {
            // 触发 EasyTierJNI 的 init 块，加载原生库
            @Suppress("UNUSED_EXPRESSION")
            EasyTierJNI
            initialized = true
            Log.i(TAG, "EasyTier JNI initialized")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI init failed: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
            false
        }
    }

    fun parseConfig(tomlConfig: String): Boolean {
        if (!initialized) return false
        return try {
            EasyTierJNI.parseConfig(tomlConfig) == 0
        } catch (e: Exception) {
            Log.e(TAG, "parseConfig error", e)
            false
        }
    }

    suspend fun startNetwork(config: NetworkConfig): EasyTierResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext EasyTierResult.fail("not initialized")

        try {
            val toml = config.toToml()
            val redactedToml = redactTomlSecrets(toml)
            LogService.info("启动网络实例: ${config.instanceName}", source = TAG)
            LogService.debug("启动配置:\n$redactedToml", source = TAG)

            if (!parseConfig(toml)) {
                val err = EasyTierJNI.getLastError() ?: "parse config failed"
                LogService.error("配置校验失败: ${config.instanceName}, $err", source = TAG)
                return@withContext EasyTierResult.fail(err)
            }

            LogService.info("配置校验通过: ${config.instanceName}", source = TAG)
            val result = EasyTierJNI.runNetworkInstance(toml)
            if (result == 0) {
                config.isRunning = true
                LogService.info("网络实例已启动: ${config.instanceName}", source = TAG)
                EasyTierResult.ok()
            } else {
                val err = EasyTierJNI.getLastError() ?: "unknown error"
                LogService.error("网络实例启动失败: ${config.instanceName}, $err", source = TAG)
                EasyTierResult.fail(err)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startNetwork error", e)
            LogService.error("startNetwork 异常: ${config.instanceName}, ${e.message}", source = TAG)
            EasyTierResult.fail(e.message ?: "exception")
        }
    }

    suspend fun stopNetwork(instanceName: String): EasyTierResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext EasyTierResult.fail("not initialized")

        try {
            // stopOthers = retain none → stop all
            // 这里更精确的做法：只停指定实例
            // collect → filter → retain
            val infosJson = EasyTierJNI.collectNetworkInfos(16) ?: "{}"
            val map = try {
                JSONObject(infosJson).optJSONObject("map")
            } catch (_: Exception) { null }

            val namesToRetain = mutableListOf<String>()
            map?.keys()?.forEach { key ->
                if (key != instanceName) namesToRetain.add(key)
            }

            val result = if (namesToRetain.isEmpty()) {
                EasyTierJNI.stopAllInstances()
            } else {
                EasyTierJNI.retainNetworkInstance(namesToRetain.toTypedArray())
            }

            if (result == 0) {
                LogService.info("网络实例已停止: $instanceName", source = TAG)
                EasyTierResult.ok()
            } else {
                val err = EasyTierJNI.getLastError() ?: "unknown"
                LogService.error("网络实例停止失败: $instanceName, $err", source = TAG)
                EasyTierResult.fail(err)
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopNetwork error", e)
            LogService.error("stopNetwork 异常: $instanceName, ${e.message}", source = TAG)
            EasyTierResult.fail(e.message ?: "exception")
        }
    }

    suspend fun stopAllNetworks(): Boolean = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext false
        try {
            EasyTierJNI.stopAllInstances() == 0
        } catch (e: Exception) {
            Log.e(TAG, "stopAll error", e)
            false
        }
    }

    suspend fun collectNodeInfos(instanceName: String): List<NodeInfo> = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext emptyList()

        try {
            val jsonStr = EasyTierJNI.collectNetworkInfos(16) ?: return@withContext emptyList()

            // 调试：打印原始 JSON
            Log.i(TAG, "collectNetworkInfos raw: $jsonStr")

            val map = JSONObject(jsonStr).optJSONObject("map") ?: return@withContext emptyList()

            Log.i(TAG, "map keys (${map.length()}): ${map.keys().asSequence().toList()}")

            val nodes = mutableListOf<NodeInfo>()
            map.keys().forEach { key ->
                val value = map.optJSONObject(key) ?: return@forEach

                // 本地节点信息 (my_node_info 包含 virtual_ipv4, hostname 等)
                val nodeObj = value.optJSONObject("my_node_info")
                if (nodeObj != null) {
                    val ipv4Inet = nodeObj.optJSONObject("virtual_ipv4")
                    val virtualIp = parseIpv4InetToString(ipv4Inet)
                    val hostname = nodeObj.optString("hostname", "")
                    nodes.add(NodeInfo(
                        hostname = hostname,
                        virtualIp = virtualIp,
                        isLocal = key == instanceName,
                    ))
                }

                // 对端节点信息 (peers 数组包含连接状态)
                val peers = value.optJSONArray("peers")
                val peerMap = mutableMapOf<Long, JSONObject>()
                if (peers != null) {
                    for (i in 0 until peers.length()) {
                        val peer = peers.optJSONObject(i) ?: continue
                        val peerId = peer.optLong("peer_id", -1L)
                        if (peerId != -1L) peerMap[peerId] = peer
                    }
                }

                // 遍历 routes 构建其他节点信息
                val myPeerId = nodeObj?.optLong("peer_id", -1L) ?: -1L
                val routes = value.optJSONArray("routes")
                Log.i(TAG, "本机 peerId=$myPeerId, peers=${peerMap.size}, routes=${routes?.length() ?: 0}")
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

                        var protocol = ""
                        var connectionType = if (isPublicServer) "server" else if (isDirectlyConnected) "direct" else "relay"
                        var rxBytes = 0L
                        var txBytes = 0L
                        var lossRate = 0.0

                        val peer = peerMap[peerId]
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

                        nodes.add(NodeInfo(
                            hostname = hostname.ifEmpty { "peer-$peerId" },
                            virtualIp = virtualIp,
                            isLocal = false,
                            latencyMs = latencyMs,
                            protocol = protocol,
                            connectionType = connectionType,
                            rxBytes = rxBytes,
                            txBytes = txBytes,
                            lossRate = lossRate
                        ))
                    }
                }
            }

            nodes
        } catch (e: Exception) {
            Log.e(TAG, "collectNodeInfos error", e)
            emptyList()
        }
    }

    fun startMonitoring(instanceName: String, onNodes: (List<NodeInfo>) -> Unit) {
        stopMonitoring()
        monitorJob = scope.launch {
            while (isActive) {
                val nodes = collectNodeInfos(instanceName)
                withContext(Dispatchers.Main) { onNodes(nodes) }
                delay(MONITOR_INTERVAL)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /** 请求 VPN 授权（需要在 Activity 中处理 onActivityResult） */
    fun createVpnPrepareIntent(activity: Activity): Intent? =
        VpnService.prepare(activity)

    /** 启动 VPN 后台服务 */
    fun startVpnService(activity: Activity, instanceName: String, ipv4: String, prefix: Int = 24, routes: List<String> = emptyList()) {
        val intent = Intent(activity, EasyTierVpnService::class.java).apply {
            putExtra("instance_name", instanceName)
            putExtra("ipv4", ipv4)
            putExtra("prefix", prefix)
            putStringArrayListExtra("routes", ArrayList(routes))
        }
        activity.startService(intent)
    }

    /** 停止 VPN */
    fun stopVpnService(activity: Activity) {
        activity.stopService(Intent(activity, EasyTierVpnService::class.java))
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

    private fun redactTomlSecrets(toml: String): String {
        return toml.replace(Regex("""(?m)^(\s*network_secret\s*=\s*").*(")$"""), "$1***$2")
    }
}
