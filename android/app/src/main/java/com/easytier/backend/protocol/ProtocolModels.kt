package com.easytier.backend.protocol

import org.json.JSONArray
import org.json.JSONObject

data class PingResult(
    val ok: Boolean,
    val backendVersion: String
) {
    companion object {
        fun fromJson(json: JSONObject): PingResult = PingResult(
            ok = json.getBoolean("ok"),
            backendVersion = json.getString("backend_version")
        )
    }
}

data class NetworkSummary(
    val instanceName: String,
    val networkName: String?,
    val running: Boolean,
    val virtualIpv4: String?,
    val peerCount: Int?,
    val routeCount: Int?
) {
    companion object {
        fun fromJson(json: JSONObject): NetworkSummary = NetworkSummary(
            instanceName = json.getString("instance_name"),
            networkName = json.optString("network_name", null),
            running = json.getBoolean("running"),
            virtualIpv4 = json.optString("virtual_ipv4", null),
            peerCount = if (json.has("peer_count")) json.getInt("peer_count") else null,
            routeCount = if (json.has("route_count")) json.getInt("route_count") else null
        )
    }
}

data class ValidationResult(
    val valid: Boolean,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): ValidationResult = ValidationResult(
            valid = json.getBoolean("valid"),
            warnings = json.optJSONArray("warnings")?.toStringList() ?: emptyList(),
            errors = json.getJSONArray("errors")?.toStringList() ?: emptyList()
        )
    }
}

data class NetworkStartResult(
    val success: Boolean,
    val instanceName: String,
    val message: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): NetworkStartResult = NetworkStartResult(
            success = json.getBoolean("success"),
            instanceName = json.getString("instance_name"),
            message = json.optString("message", null)
        )
    }
}

data class NetworkListResult(
    val instances: List<NetworkSummary>
) {
    companion object {
        fun fromJson(json: JSONObject): NetworkListResult {
            val arr = json.getJSONArray("instances")
            val instances = (0 until arr.length()).map { arr.getJSONObject(it) }.map(NetworkSummary::fromJson)
            return NetworkListResult(instances = instances)
        }
    }
}

data class ProtocolNodeInfo(
    val key: String,
    val value: String
) {
    companion object {
        fun fromJson(json: JSONObject): ProtocolNodeInfo = ProtocolNodeInfo(
            key = json.getString("key"),
            value = json.getString("value")
        )
    }
}

data class ProtocolRouteInfo(
    val destination: String,
    val via: String,
    val proxyCidrs: List<String> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): ProtocolRouteInfo = ProtocolRouteInfo(
            destination = json.getString("destination"),
            via = json.getString("via"),
            proxyCidrs = json.optJSONArray("proxy_cidrs")?.toStringList() ?: emptyList()
        )
    }
}

data class ProtocolLogEntry(
    val timestamp: String,
    val level: String?,
    val message: String
) {
    companion object {
        fun fromJson(json: JSONObject): ProtocolLogEntry = ProtocolLogEntry(
            timestamp = json.getString("timestamp"),
            level = json.optString("level", null),
            message = json.getString("message")
        )
    }
}

data class NetworkStateResult(
    val instanceName: String,
    val running: Boolean,
    val nodes: List<ProtocolNodeInfo> = emptyList(),
    val routes: List<ProtocolRouteInfo> = emptyList(),
    val logTail: List<ProtocolLogEntry> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): NetworkStateResult {
            val nodesArr = json.optJSONArray("nodes")
            val routesArr = json.optJSONArray("routes")
            val logArr = json.optJSONArray("log_tail")
            return NetworkStateResult(
                instanceName = json.getString("instance_name"),
                running = json.getBoolean("running"),
                nodes = nodesArr?.toList()?.map { ProtocolNodeInfo.fromJson(it as JSONObject) } ?: emptyList(),
                routes = routesArr?.toList()?.map { ProtocolRouteInfo.fromJson(it as JSONObject) } ?: emptyList(),
                logTail = logArr?.toList()?.map { ProtocolLogEntry.fromJson(it as JSONObject) } ?: emptyList()
            )
        }
    }
}

data class SubscribeResult(
    val subscribed: Boolean,
    val channel: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): SubscribeResult = SubscribeResult(
            subscribed = json.getBoolean("subscribed"),
            channel = json.optString("channel", null)
        )
    }
}

data class InstanceChangedEvent(
    val instanceName: String,
    val action: String,
    val state: NetworkSummary? = null
) {
    companion object {
        fun fromJson(json: JSONObject): InstanceChangedEvent = InstanceChangedEvent(
            instanceName = json.getString("instance_name"),
            action = json.getString("action"),
            state = json.optJSONObject("state")?.let { NetworkSummary.fromJson(it) }
        )
    }
}

data class NodeChangedEvent(
    val instanceName: String,
    val nodes: List<ProtocolNodeInfo>
) {
    companion object {
        fun fromJson(json: JSONObject): NodeChangedEvent = NodeChangedEvent(
            instanceName = json.getString("instance_name"),
            nodes = json.getJSONArray("nodes")?.toList()?.map { ProtocolNodeInfo.fromJson(it as JSONObject) } ?: emptyList()
        )
    }
}

data class RouteChangedEvent(
    val instanceName: String,
    val routes: List<ProtocolRouteInfo>
) {
    companion object {
        fun fromJson(json: JSONObject): RouteChangedEvent = RouteChangedEvent(
            instanceName = json.getString("instance_name"),
            routes = json.getJSONArray("routes")?.toList()?.map { ProtocolRouteInfo.fromJson(it as JSONObject) } ?: emptyList()
        )
    }
}

data class LogEvent(
    val instanceName: String,
    val entry: ProtocolLogEntry
) {
    companion object {
        fun fromJson(json: JSONObject): LogEvent = LogEvent(
            instanceName = json.getString("instance_name"),
            entry = ProtocolLogEntry.fromJson(json.getJSONObject("entry"))
        )
    }
}

data class JsonRpcErrorInfo(
    val code: Int,
    val message: String,
    val data: JSONObject? = null
) {
    companion object {
        const val BACKEND_UNAVAILABLE = -32001
        const val VALIDATION_FAILED = -32002
        const val NETWORK_START_FAILED = -32003
        const val NETWORK_STOP_FAILED = -32004
    }
}

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).map { getString(it) }

private fun JSONArray.toList(): List<Any> =
    (0 until length()).map { get(it) }
