package com.easytier.backend.jni

import android.util.Log
import com.easytier.backend.BackendClient
import com.easytier.backend.BackendResult
import com.easytier.backend.collectNodeInfosFromJson
import com.easytier.backend.collectRunningInstanceNames
import com.easytier.backend.redactTomlSecrets
import com.easytier.backend.protocol.NetworkListResult
import com.easytier.backend.protocol.NetworkStateResult
import com.easytier.backend.protocol.NetworkSummary
import com.easytier.backend.protocol.PingResult
import com.easytier.backend.protocol.ProtocolLogEntry
import com.easytier.backend.protocol.ProtocolNodeInfo
import com.easytier.backend.protocol.ProtocolRouteInfo
import com.easytier.backend.protocol.SubscribeResult
import com.easytier.backend.protocol.ValidationResult
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import com.easytier.jni.EasyTierJNI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JniBackendClient : BackendClient {
    private var initialized = false
    private var backendVersion = "unknown"

    override fun initialize(): BackendResult {
        return try {
            @Suppress("UNUSED_EXPRESSION")
            EasyTierJNI
            initialized = true
            backendVersion = try {
                EasyTierJNI.getLastError().let {
                    if (it.isNullOrEmpty()) "2.x" else it
                }
            } catch (_: Exception) { "2.x" }
            Log.i(TAG, "EasyTier JNI initialized")
            BackendResult.ok()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "JNI init failed: ${e.message}")
            BackendResult.fail(e.message ?: "jni init failed")
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
            BackendResult.fail(e.message ?: "init failed")
        }
    }

    override fun parseConfig(tomlConfig: String): BackendResult {
        if (!initialized) return BackendResult.fail("not initialized")
        return try {
            if (EasyTierJNI.parseConfig(tomlConfig) == 0) {
                BackendResult.ok()
            } else {
                BackendResult.fail(EasyTierJNI.getLastError() ?: "parse config failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseConfig error", e)
            BackendResult.fail(e.message ?: "parse config error")
        }
    }

    override suspend fun ping(): PingResult = withContext(Dispatchers.IO) {
        if (!initialized) {
            return@withContext PingResult(ok = false, backendVersion = "uninitialized")
        }
        try {
            val infos = EasyTierJNI.collectNetworkInfos(1)
            PingResult(ok = infos != null, backendVersion = backendVersion)
        } catch (e: Exception) {
            Log.e(TAG, "ping error", e)
            PingResult(ok = false, backendVersion = "error: ${e.message}")
        }
    }

    override suspend fun validate(config: NetworkConfig): ValidationResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext ValidationResult(valid = false, errors = listOf("not initialized"))

        try {
            val toml = config.toToml()
            val result = EasyTierJNI.parseConfig(toml)
            if (result == 0) {
                ValidationResult(valid = true)
            } else {
                val err = EasyTierJNI.getLastError() ?: "unknown error"
                ValidationResult(valid = false, errors = listOf(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "validate error", e)
            ValidationResult(valid = false, errors = listOf(e.message ?: "exception"))
        }
    }

    override suspend fun startNetwork(config: NetworkConfig): BackendResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext BackendResult.fail("not initialized")

        try {
            val toml = config.toToml()
            val redactedToml = redactTomlSecrets(toml)
            Log.i(TAG, "启动网络实例: ${config.instanceName}")
            Log.i(TAG, "启动配置:\n$redactedToml")

            val parseResult = parseConfig(toml)
            if (!parseResult.success) {
                Log.e(TAG, "配置校验失败: ${config.instanceName}, ${parseResult.errorMessage}")
                return@withContext parseResult
            }

            Log.i(TAG, "配置校验通过: ${config.instanceName}")
            val result = EasyTierJNI.runNetworkInstance(toml)
            if (result == 0) {
                config.isRunning = true
                Log.i(TAG, "网络实例已启动: ${config.instanceName}")
                BackendResult.ok()
            } else {
                val errorMessage = EasyTierJNI.getLastError() ?: "unknown error"
                Log.e(TAG, "网络实例启动失败: ${config.instanceName}, $errorMessage")
                BackendResult.fail(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startNetwork error", e)
            BackendResult.fail(e.message ?: "exception")
        }
    }

    override suspend fun stopNetwork(instanceName: String): BackendResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext BackendResult.fail("not initialized")

        try {
            val infosJson = EasyTierJNI.collectNetworkInfos(16) ?: "{}"
            val namesToRetain = collectRunningInstanceNames(infosJson)
                .filter { it != instanceName }

            val result = if (namesToRetain.isEmpty()) {
                EasyTierJNI.stopAllInstances()
            } else {
                EasyTierJNI.retainNetworkInstance(namesToRetain.toTypedArray())
            }

            if (result == 0) {
                Log.i(TAG, "网络实例已停止: $instanceName")
                BackendResult.ok()
            } else {
                val errorMessage = EasyTierJNI.getLastError() ?: "unknown"
                Log.e(TAG, "网络实例停止失败: $instanceName, $errorMessage")
                BackendResult.fail(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopNetwork error", e)
            BackendResult.fail(e.message ?: "exception")
        }
    }

    override suspend fun stopAllNetworks(): BackendResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext BackendResult.fail("not initialized")

        try {
            if (EasyTierJNI.stopAllInstances() == 0) {
                BackendResult.ok()
            } else {
                BackendResult.fail(EasyTierJNI.getLastError() ?: "unknown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopAll error", e)
            BackendResult.fail(e.message ?: "exception")
        }
    }

    override suspend fun listNetworks(): NetworkListResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext NetworkListResult(instances = emptyList())

        try {
            val jsonStr = EasyTierJNI.collectNetworkInfos(16) ?: return@withContext NetworkListResult(instances = emptyList())
            val summaries = parseNetworkSummaries(jsonStr)
            NetworkListResult(instances = summaries)
        } catch (e: Exception) {
            Log.e(TAG, "listNetworks error", e)
            NetworkListResult(instances = emptyList())
        }
    }

    override suspend fun getNetworkState(instanceName: String): NetworkStateResult = withContext(Dispatchers.IO) {
        if (!initialized) {
            return@withContext NetworkStateResult(instanceName = instanceName, running = false)
        }

        try {
            val jsonStr = EasyTierJNI.collectNetworkInfos(16) ?: return@withContext NetworkStateResult(instanceName = instanceName, running = false)
            parseNetworkState(jsonStr, instanceName)
        } catch (e: Exception) {
            Log.e(TAG, "getNetworkState error", e)
            NetworkStateResult(instanceName = instanceName, running = false)
        }
    }

    override suspend fun collectNodeInfos(instanceName: String): List<NodeInfo> = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext emptyList()

        try {
            val jsonStr = EasyTierJNI.collectNetworkInfos(16) ?: return@withContext emptyList()
            Log.i(TAG, "collectNetworkInfos raw: $jsonStr")
            collectNodeInfosFromJson(jsonStr, instanceName)
        } catch (e: Exception) {
            Log.e(TAG, "collectNodeInfos error", e)
            emptyList()
        }
    }

    override suspend fun collectNetworkInfoJson(): String? = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext null

        try {
            EasyTierJNI.collectNetworkInfos(16)
        } catch (e: Exception) {
            Log.e(TAG, "collectNetworkInfoJson error", e)
            null
        }
    }

    override suspend fun vpnAttach(instanceName: String, tunFd: Int): BackendResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext BackendResult.fail("not initialized")

        try {
            val result = EasyTierJNI.setTunFd(instanceName, tunFd)
            if (result == 0) {
                Log.i(TAG, "TUN fd 已绑定: $instanceName, fd=$tunFd")
                BackendResult.ok()
            } else {
                val errorMessage = EasyTierJNI.getLastError() ?: "attach tun failed"
                Log.e(TAG, "TUN fd 绑定失败: $instanceName, $errorMessage")
                BackendResult.fail(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "vpnAttach error", e)
            BackendResult.fail(e.message ?: "exception")
        }
    }

    override suspend fun vpnDetach(instanceName: String): BackendResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext BackendResult.fail("not initialized")

        try {
            val result = EasyTierJNI.setTunFd(instanceName, -1)
            if (result == 0) {
                Log.i(TAG, "TUN fd 已解绑: $instanceName")
                BackendResult.ok()
            } else {
                val errorMessage = EasyTierJNI.getLastError() ?: "detach tun failed"
                Log.e(TAG, "TUN fd 解绑失败: $instanceName, $errorMessage")
                BackendResult.fail(errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "vpnDetach error", e)
            BackendResult.fail(e.message ?: "exception")
        }
    }

    override suspend fun logSubscribe(instanceName: String, tail: Int?): SubscribeResult = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext SubscribeResult(subscribed = false)

        try {
            val jsonStr = EasyTierJNI.collectNetworkInfos(tail ?: 16) ?: return@withContext SubscribeResult(subscribed = false)
            val hasInstance = collectRunningInstanceNames(jsonStr).contains(instanceName)
            SubscribeResult(
                subscribed = hasInstance,
                channel = if (hasInstance) "local:instance:$instanceName" else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "logSubscribe error", e)
            SubscribeResult(subscribed = false)
        }
    }

    private fun parseNetworkSummaries(jsonStr: String): List<NetworkSummary> {
        return try {
            val map = JSONObject(jsonStr).optJSONObject("map") ?: return emptyList()
            val summaries = mutableListOf<NetworkSummary>()
            map.keys().forEach { key ->
                val inst = map.optJSONObject(key) ?: return@forEach
                val nodeInfo = inst.optJSONObject("my_node_info")
                val peers = inst.optJSONArray("peers")
                val routes = inst.optJSONArray("routes")
                summaries.add(
                    NetworkSummary(
                        instanceName = key,
                        networkName = nodeInfo?.optString("hostname", null),
                        running = inst.has("my_node_info"),
                        virtualIpv4 = nodeInfo?.optJSONObject("virtual_ipv4")
                            ?.optJSONObject("address")?.optString("addr", null),
                        peerCount = peers?.length() ?: 0,
                        routeCount = routes?.length() ?: 0
                    )
                )
            }
            summaries
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNetworkState(jsonStr: String, instanceName: String): NetworkStateResult {
        return try {
            val map = JSONObject(jsonStr).optJSONObject("map") ?: return NetworkStateResult(instanceName, running = false)
            val inst = map.optJSONObject(instanceName) ?: return NetworkStateResult(instanceName, running = false)

            val hasNodeInfo = inst.has("my_node_info")
            val peers = inst.optJSONArray("peers") ?: JSONArray()
            val routes = inst.optJSONArray("routes") ?: JSONArray()
            val events = inst.optJSONArray("events") ?: JSONArray()

            val protocolNodes = mutableListOf<ProtocolNodeInfo>()
            for (i in 0 until peers.length()) {
                val peer = peers.optJSONObject(i) ?: continue
                protocolNodes.add(
                    ProtocolNodeInfo(
                        key = peer.optString("peer_id", "").toString(),
                        value = peer.toString()
                    )
                )
            }

            val protocolRoutes = mutableListOf<ProtocolRouteInfo>()
            for (i in 0 until routes.length()) {
                val route = routes.optJSONObject(i) ?: continue
                val cidrsArr = route.optJSONArray("proxy_cidrs")
                protocolRoutes.add(
                    ProtocolRouteInfo(
                        destination = route.optString("hostname", ""),
                        via = route.optString("peer_id", "").toString(),
                        proxyCidrs = cidrsArr?.toStringList() ?: emptyList()
                    )
                )
            }

            val logEntries = mutableListOf<ProtocolLogEntry>()
            for (i in 0 until events.length()) {
                val event = events.optJSONObject(i) ?: continue
                logEntries.add(
                    ProtocolLogEntry(
                        timestamp = event.optString("timestamp", ""),
                        level = event.optString("level", null),
                        message = event.optString("message", "")
                    )
                )
            }

            NetworkStateResult(
                instanceName = instanceName,
                running = hasNodeInfo,
                nodes = protocolNodes,
                routes = protocolRoutes,
                logTail = logEntries
            )
        } catch (_: Exception) {
            NetworkStateResult(instanceName, running = false)
        }
    }

    companion object {
        private const val TAG = "JniBackendClient"
    }
}

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).map { getString(it) }
