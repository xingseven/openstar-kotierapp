package com.easytier.data

enum class ConnectionType { DIRECT, RELAY, SERVER, UNKNOWN }

data class NodeInfo(
    val hostname: String,
    val virtualIp: String,
    val latencyMs: Int = 0,
    val protocol: String = "",
    val connectionType: String = "unknown",
    val isLocal: Boolean = false,
    val natType: String = "",
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val lossRate: Double = 0.0
) {
    val connTypeEnum: ConnectionType
        get() = when (connectionType) {
            "direct" -> ConnectionType.DIRECT
            "relay" -> ConnectionType.RELAY
            "server" -> ConnectionType.SERVER
            else -> ConnectionType.UNKNOWN
        }

    val latencyText: String
        get() = if (latencyMs <= 0) "-" else "${latencyMs}ms"

    val trafficText: String
        get() = "↓${formatBytes(rxBytes)} ↑${formatBytes(txBytes)}"

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
}
