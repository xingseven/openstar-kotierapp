package com.easytier.backend

import android.util.Log
import com.easytier.backend.protocol.JsonRpcError
import com.easytier.backend.protocol.JsonRpcErrorInfo
import com.easytier.backend.protocol.JsonRpcMessage
import com.easytier.backend.protocol.JsonRpcParser
import com.easytier.backend.protocol.JsonRpcRequest
import com.easytier.backend.protocol.JsonRpcResponse
import com.easytier.backend.protocol.NetworkListResult
import com.easytier.backend.protocol.NetworkStateResult
import com.easytier.data.NetworkConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

class JsonRpcClient(private val backend: BackendClient) {

    suspend fun call(requestJson: String): String {
        val message = JsonRpcParser.parse(requestJson)
        if (message !is JsonRpcRequest) {
            return JsonRpcError(
                id = null,
                code = -32600,
                message = "invalid request"
            ).toJson().toString()
        }

        val reqId = message.id
        return try {
            val resultJson = route(message)
            JsonRpcResponse(id = reqId, result = resultJson).toJson().toString()
        } catch (e: ProtocolException) {
            JsonRpcError(id = reqId, code = e.code, message = e.message ?: "protocol error").toJson().toString()
        } catch (e: Exception) {
            Log.e(TAG, "JSON-RPC call failed: ${message.method}", e)
            JsonRpcError(id = reqId, code = -32603, message = "internal error: ${e.message}").toJson().toString()
        }
    }

    private suspend fun route(request: JsonRpcRequest): Any? {
        val params = request.params ?: JSONObject()
        return when (request.method) {
            "backend.ping" -> {
                val result = backend.ping()
                JSONObject().apply {
                    put("ok", result.ok)
                    put("backend_version", result.backendVersion)
                }
            }
            "network.validate" -> {
                val config = networkConfigFromParams(params)
                if (config == null) {
                    throw ProtocolException(JsonRpcErrorInfo.VALIDATION_FAILED, "invalid params: missing network config")
                }
                val result = backend.validate(config)
                JSONObject().apply {
                    put("valid", result.valid)
                    put("errors", result.errors)
                    put("warnings", result.warnings)
                }
            }
            "network.start" -> {
                val config = networkConfigFromParams(params)
                if (config == null) {
                    throw ProtocolException(JsonRpcErrorInfo.VALIDATION_FAILED, "invalid params: missing network config")
                }
                val result = backend.startNetwork(config)
                JSONObject().apply {
                    put("success", result.success)
                    put("instance_name", config.instanceName)
                    if (!result.success) put("message", result.errorMessage)
                }
            }
            "network.stop" -> {
                val instanceName = params.optString("instance_name", "")
                if (instanceName.isEmpty()) {
                    throw ProtocolException(JsonRpcErrorInfo.NETWORK_STOP_FAILED, "invalid params: missing instance_name")
                }
                val result = backend.stopNetwork(instanceName)
                JSONObject().apply {
                    put("success", result.success)
                    put("instance_name", instanceName)
                    if (!result.success) put("message", result.errorMessage)
                }
            }
            "network.list" -> {
                val result = backend.listNetworks()
                val instancesArr = org.json.JSONArray()
                result.instances.forEach { summary ->
                    instancesArr.put(JSONObject().apply {
                        put("instance_name", summary.instanceName)
                        summary.networkName?.let { put("network_name", it) }
                        put("running", summary.running)
                        summary.virtualIpv4?.let { put("virtual_ipv4", it) }
                        summary.peerCount?.let { put("peer_count", it) }
                        summary.routeCount?.let { put("route_count", it) }
                    })
                }
                JSONObject().apply { put("instances", instancesArr) }
            }
            "network.state" -> {
                val instanceName = params.optString("instance_name", "")
                if (instanceName.isEmpty()) {
                    throw ProtocolException(-32602, "invalid params: missing instance_name")
                }
                val result = backend.getNetworkState(instanceName)
                JSONObject().apply {
                    put("instance_name", result.instanceName)
                    put("running", result.running)
                    put("nodes", org.json.JSONArray(result.nodes.map { n ->
                        JSONObject().apply { put("key", n.key); put("value", n.value) }
                    }))
                    put("routes", org.json.JSONArray(result.routes.map { r ->
                        JSONObject().apply {
                            put("destination", r.destination)
                            put("via", r.via)
                            put("proxy_cidrs", org.json.JSONArray(r.proxyCidrs))
                        }
                    }))
                    put("log_tail", org.json.JSONArray(result.logTail.map { l ->
                        JSONObject().apply {
                            put("timestamp", l.timestamp)
                            l.level?.let { put("level", it) }
                            put("message", l.message)
                        }
                    }))
                }
            }
            "vpn.attach" -> {
                val instanceName = params.optString("instance_name", "")
                val tunFd = params.optInt("tun_fd", -1)
                if (instanceName.isEmpty() || tunFd < 0) {
                    throw ProtocolException(-32602, "invalid params: need instance_name and tun_fd")
                }
                val result = backend.vpnAttach(instanceName, tunFd)
                JSONObject().apply {
                    put("success", result.success)
                    put("instance_name", instanceName)
                    if (!result.success) put("message", result.errorMessage)
                }
            }
            "vpn.detach" -> {
                val instanceName = params.optString("instance_name", "")
                if (instanceName.isEmpty()) {
                    throw ProtocolException(-32602, "invalid params: missing instance_name")
                }
                val result = backend.vpnDetach(instanceName)
                JSONObject().apply {
                    put("success", result.success)
                    put("instance_name", instanceName)
                    if (!result.success) put("message", result.errorMessage)
                }
            }
            "log.subscribe" -> {
                val instanceName = params.optString("instance_name", "")
                if (instanceName.isEmpty()) {
                    throw ProtocolException(-32602, "invalid params: missing instance_name")
                }
                val tail = if (params.has("tail")) params.getInt("tail") else null
                val result = backend.logSubscribe(instanceName, tail)
                JSONObject().apply {
                    put("subscribed", result.subscribed)
                    result.channel?.let { put("channel", it) }
                }
            }
            else -> throw ProtocolException(-32601, "method not found: ${request.method}")
        }
    }

    private fun networkConfigFromParams(params: JSONObject): NetworkConfig? {
        return try {
            val config = NetworkConfig()
            params.optString("instance_name", "").ifNotEmpty { config.instanceName = it }
            params.optString("network_name", "").ifNotEmpty { config.networkName = it }
            params.optString("network_secret", "").ifNotEmpty { config.networkSecret = it }
            params.optString("ipv4", "").ifNotEmpty { config.ipv4 = it }

            val serversArr = params.optJSONArray("servers")
            if (serversArr != null) {
                config.servers = (0 until serversArr.length()).map { serversArr.getString(it) }.toMutableList()
            }
            if (params.has("dhcp")) config.dhcp = params.getBoolean("dhcp")
            if (params.has("latency_first")) config.latencyFirst = params.getBoolean("latency_first")
            if (params.has("no_tun")) config.noTun = params.getBoolean("no_tun")
            if (params.has("use_smoltcp")) config.useSmoltcp = params.getBoolean("use_smoltcp")
            config
        } catch (e: Exception) {
            Log.w(TAG, "failed to parse NetworkConfig from params", e)
            null
        }
    }

    class ProtocolException(val code: Int, msg: String) : Exception(msg)

    companion object {
        private const val TAG = "JsonRpcClient"
    }
}

private fun String.ifNotEmpty(action: (String) -> Unit) {
    if (isNotEmpty()) action(this)
}
