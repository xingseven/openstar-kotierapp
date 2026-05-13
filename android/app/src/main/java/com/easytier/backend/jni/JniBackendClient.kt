package com.easytier.backend.jni

import android.util.Log
import com.easytier.backend.BackendClient
import com.easytier.backend.BackendResult
import com.easytier.backend.collectNodeInfosFromJson
import com.easytier.backend.collectRunningInstanceNames
import com.easytier.backend.redactTomlSecrets
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import com.easytier.jni.EasyTierJNI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JniBackendClient : BackendClient {
    private var initialized = false

    override fun initialize(): BackendResult {
        return try {
            @Suppress("UNUSED_EXPRESSION")
            EasyTierJNI
            initialized = true
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

    companion object {
        private const val TAG = "JniBackendClient"
    }
}