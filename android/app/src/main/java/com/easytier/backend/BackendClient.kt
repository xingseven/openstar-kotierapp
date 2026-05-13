package com.easytier.backend

import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo

data class BackendResult(val success: Boolean, val errorMessage: String = "") {
    companion object {
        fun ok() = BackendResult(true)
        fun fail(message: String) = BackendResult(false, message)
    }
}

interface BackendClient {
    fun initialize(): BackendResult
    fun parseConfig(tomlConfig: String): BackendResult
    suspend fun startNetwork(config: NetworkConfig): BackendResult
    suspend fun stopNetwork(instanceName: String): BackendResult
    suspend fun stopAllNetworks(): BackendResult
    suspend fun collectNodeInfos(instanceName: String): List<NodeInfo>
    suspend fun collectNetworkInfoJson(): String?
}