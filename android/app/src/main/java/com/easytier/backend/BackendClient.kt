package com.easytier.backend

import com.easytier.backend.protocol.NetworkListResult
import com.easytier.backend.protocol.NetworkStateResult
import com.easytier.backend.protocol.NetworkSummary
import com.easytier.backend.protocol.PingResult
import com.easytier.backend.protocol.SubscribeResult
import com.easytier.backend.protocol.ValidationResult
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

    suspend fun ping(): PingResult
    suspend fun validate(config: NetworkConfig): ValidationResult
    suspend fun startNetwork(config: NetworkConfig): BackendResult
    suspend fun stopNetwork(instanceName: String): BackendResult
    suspend fun stopAllNetworks(): BackendResult
    suspend fun listNetworks(): NetworkListResult
    suspend fun getNetworkState(instanceName: String): NetworkStateResult
    suspend fun collectNodeInfos(instanceName: String): List<NodeInfo>
    suspend fun collectNetworkInfoJson(): String?
    suspend fun vpnAttach(instanceName: String, tunFd: Int): BackendResult
    suspend fun vpnDetach(instanceName: String): BackendResult
    suspend fun logSubscribe(instanceName: String, tail: Int? = null): SubscribeResult
}
