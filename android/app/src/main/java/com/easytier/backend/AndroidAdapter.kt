package com.easytier.backend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.easytier.backend.protocol.NetworkListResult
import com.easytier.backend.protocol.NetworkStateResult
import com.easytier.backend.protocol.NetworkSummary
import com.easytier.backend.protocol.PingResult
import com.easytier.backend.protocol.SubscribeResult
import com.easytier.backend.protocol.ValidationResult
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import com.easytier.service.EasyTierVpnService
import kotlinx.coroutines.*
import org.json.JSONObject

class AndroidAdapter(
    private val jsonRpcClient: JsonRpcClient,
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    fun ping(callback: (PingResult) -> Unit) {
        scope.launch {
            val request = """{"jsonrpc":"2.0","id":"1","method":"backend.ping","params":{}}"""
            val responseJson = jsonRpcClient.call(request)
            val result = parsePingResult(responseJson)
            withContext(Dispatchers.Main) { callback(result) }
        }
    }

    suspend fun ping(): PingResult {
        val request = """{"jsonrpc":"2.0","id":"1","method":"backend.ping","params":{}}"""
        val responseJson = jsonRpcClient.call(request)
        return parsePingResult(responseJson)
    }

    suspend fun startNetwork(config: NetworkConfig): BackendResult {
        val params = buildNetworkConfigParams(config)
        val request = """{"jsonrpc":"2.0","id":"2","method":"network.start","params":$params}"""
        val responseJson = jsonRpcClient.call(request)
        return parseSuccessResult(responseJson, config.instanceName)
    }

    suspend fun stopNetwork(instanceName: String): BackendResult {
        val request = """{"jsonrpc":"2.0","id":"3","method":"network.stop","params":{"instance_name":"$instanceName"}}"""
        val responseJson = jsonRpcClient.call(request)
        return parseSuccessResult(responseJson, instanceName)
    }

    suspend fun stopAllNetworks(): Boolean {
        val result = backendCall("network.stopAll", """{"instance_name":"all"}""")
        return result
    }

    suspend fun listNetworks(): NetworkListResult {
        val request = """{"jsonrpc":"2.0","id":"4","method":"network.list","params":{}}"""
        val responseJson = jsonRpcClient.call(request)
        return parseListResult(responseJson)
    }

    suspend fun getNetworkState(instanceName: String): NetworkStateResult {
        val request = """{"jsonrpc":"2.0","id":"5","method":"network.state","params":{"instance_name":"$instanceName"}}"""
        val responseJson = jsonRpcClient.call(request)
        return parseStateResult(responseJson, instanceName)
    }

    suspend fun collectNodeInfos(instanceName: String): List<NodeInfo> {
        val request = """{"jsonrpc":"2.0","id":"6","method":"network.state","params":{"instance_name":"$instanceName"}}"""
        val responseJson = jsonRpcClient.call(request)
        return parseNodeInfosFromState(responseJson, instanceName)
    }

    suspend fun vpnAttach(instanceName: String, tunFd: Int): BackendResult {
        val request = """{"jsonrpc":"2.0","id":"7","method":"vpn.attach","params":{"instance_name":"$instanceName","tun_fd":$tunFd}}"""
        val responseJson = jsonRpcClient.call(request)
        return parseSuccessResult(responseJson, instanceName)
    }

    suspend fun vpnDetach(instanceName: String): BackendResult {
        val request = """{"jsonrpc":"2.0","id":"8","method":"vpn.detach","params":{"instance_name":"$instanceName"}}"""
        val responseJson = jsonRpcClient.call(request)
        return parseSuccessResult(responseJson, instanceName)
    }

    suspend fun validate(config: NetworkConfig): ValidationResult {
        val params = buildNetworkConfigParams(config)
        val request = """{"jsonrpc":"2.0","id":"9","method":"network.validate","params":$params}"""
        val responseJson = jsonRpcClient.call(request)
        return parseValidateResult(responseJson)
    }

    fun createVpnPrepareIntent(): Intent? = VpnService.prepare(context)

    fun startVpnService(instanceName: String, ipv4: String, prefix: Int = 24, routes: List<String> = emptyList()) {
        val intent = Intent(context, EasyTierVpnService::class.java).apply {
            putExtra("instance_name", instanceName)
            putExtra("ipv4", ipv4)
            putExtra("prefix", prefix)
            putStringArrayListExtra("routes", ArrayList(routes))
        }
        context.startService(intent)
    }

    fun stopVpnService(): Boolean {
        val intent = Intent(context, EasyTierVpnService::class.java).apply {
            action = EasyTierVpnService.ACTION_STOP
        }
        return try {
            context.startService(intent)
            context.stopService(Intent(context, EasyTierVpnService::class.java))
            true
        } catch (e: Exception) {
            Log.w(TAG, "failed to deliver explicit VPN stop action, falling back to stopService", e)
            context.stopService(Intent(context, EasyTierVpnService::class.java))
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

    fun destroy() {
        stopMonitoring()
        scope.cancel()
    }

    private suspend fun backendCall(method: String, paramsJson: String): Boolean {
        return try {
            val request = """{"jsonrpc":"2.0","id":"x","method":"$method","params":$paramsJson}"""
            val responseJson = jsonRpcClient.call(request)
            !responseJson.contains("\"error\"")
        } catch (e: Exception) {
            false
        }
    }

    private fun buildNetworkConfigParams(config: NetworkConfig): String {
        return JSONObject(config.toJson()).toString()
    }

    companion object {
        private const val TAG = "AndroidAdapter"
        private const val MONITOR_INTERVAL = 3000L
    }
}

private fun parsePingResult(responseJson: String): PingResult {
    return try {
        val json = org.json.JSONObject(responseJson)
        if (json.has("error")) {
            PingResult(ok = false, backendVersion = "error")
        } else {
            val result = json.getJSONObject("result")
            PingResult(
                ok = result.getBoolean("ok"),
                backendVersion = result.getString("backend_version")
            )
        }
    } catch (e: Exception) {
        PingResult(ok = false, backendVersion = "parse error")
    }
}

private fun parseSuccessResult(responseJson: String, instanceName: String): BackendResult {
    return try {
        val json = org.json.JSONObject(responseJson)
        if (json.has("error")) {
            val err = json.getJSONObject("error")
            BackendResult.fail(err.optString("message", "unknown error"))
        } else {
            val result = json.getJSONObject("result")
            if (result.optBoolean("success", false)) {
                BackendResult.ok()
            } else {
                BackendResult.fail(result.optString("message", "failed"))
            }
        }
    } catch (e: Exception) {
        BackendResult.fail(e.message ?: "parse error")
    }
}

private fun parseListResult(responseJson: String): NetworkListResult {
    return try {
        val json = org.json.JSONObject(responseJson)
        if (json.has("error")) return NetworkListResult(instances = emptyList())
        val result = json.getJSONObject("result")
        NetworkListResult.fromJson(result)
    } catch (_: Exception) {
        NetworkListResult(instances = emptyList())
    }
}

private fun parseStateResult(responseJson: String, instanceName: String): NetworkStateResult {
    return try {
        val json = org.json.JSONObject(responseJson)
        if (json.has("error")) return NetworkStateResult(instanceName, running = false)
        val result = json.getJSONObject("result")
        NetworkStateResult.fromJson(result)
    } catch (_: Exception) {
        NetworkStateResult(instanceName, running = false)
    }
}

private fun parseValidateResult(responseJson: String): ValidationResult {
    return try {
        val json = org.json.JSONObject(responseJson)
        if (json.has("error")) return ValidationResult(valid = false, errors = listOf("protocol error"))
        val result = json.getJSONObject("result")
        ValidationResult.fromJson(result)
    } catch (e: Exception) {
        ValidationResult(valid = false, errors = listOf(e.message ?: "parse error"))
    }
}

private fun parseNodeInfosFromState(responseJson: String, instanceName: String): List<NodeInfo> {
    return try {
        val json = org.json.JSONObject(responseJson)
        if (json.has("error")) return emptyList()
        val result = json.getJSONObject("result")
        val nodesArr = result.optJSONArray("nodes") ?: return emptyList()
        val routesArr = result.optJSONArray("routes") ?: org.json.JSONArray()

        val nodeInfos = mutableListOf<NodeInfo>()
        for (i in 0 until nodesArr.length()) {
            val node = nodesArr.getJSONObject(i)
            nodeInfos.add(
                NodeInfo(
                    hostname = node.optString("key", "peer-$i"),
                    virtualIp = "",
                    isLocal = node.optString("key", "") == instanceName
                )
            )
        }
        nodeInfos
    } catch (_: Exception) {
        emptyList()
    }
}
