package com.easytier.service

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.easytier.backend.BackendClient
import com.easytier.backend.BackendResult
import com.easytier.backend.redactTomlSecrets
import com.easytier.backend.jni.JniBackendClient
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import kotlinx.coroutines.*

/** EasyTier 核心服务 —— 封装 JNI 调用 + VPN 管理 + 监控 */
object EasyTierService {
    private const val TAG = "EasyTierService"
    private const val VPN_REQUEST_CODE = 10001
    private const val MONITOR_INTERVAL = 3000L

    private var initialized = false
    private val backend: BackendClient = JniBackendClient()
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
            val result = backend.initialize()
            initialized = result.success
            if (result.success) {
                Log.i(TAG, "Backend initialized")
            } else {
                Log.e(TAG, "Backend init failed: ${result.errorMessage}")
            }
            result.success
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
            initialized = false
            false
        }
    }

    suspend fun startNetwork(config: NetworkConfig): EasyTierResult {
        if (!initialized) return EasyTierResult.fail("not initialized")

        return try {
            val toml = config.toToml()
            val redactedToml = redactTomlSecrets(toml)
            LogService.info("启动网络实例: ${config.instanceName}", source = TAG)
            LogService.debug("启动配置:\n$redactedToml", source = TAG)

            val result = backend.startNetwork(config)
            if (result.success) {
                config.isRunning = true
                LogService.info("网络实例已启动: ${config.instanceName}", source = TAG)
                EasyTierResult.ok()
            } else {
                val err = result.errorMessage.ifEmpty { "unknown error" }
                LogService.error("网络实例启动失败: ${config.instanceName}, $err", source = TAG)
                EasyTierResult.fail(err)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startNetwork error", e)
            LogService.error("startNetwork 异常: ${config.instanceName}, ${e.message}", source = TAG)
            EasyTierResult.fail(e.message ?: "exception")
        }
    }

    suspend fun stopNetwork(instanceName: String): EasyTierResult {
        if (!initialized) return EasyTierResult.fail("not initialized")

        return try {
            val result = backend.stopNetwork(instanceName)

            if (result.success) {
                LogService.info("网络实例已停止: $instanceName", source = TAG)
                EasyTierResult.ok()
            } else {
                val err = result.errorMessage.ifEmpty { "unknown" }
                LogService.error("网络实例停止失败: $instanceName, $err", source = TAG)
                EasyTierResult.fail(err)
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopNetwork error", e)
            LogService.error("stopNetwork 异常: $instanceName, ${e.message}", source = TAG)
            EasyTierResult.fail(e.message ?: "exception")
        }
    }

    suspend fun stopAllNetworks(): Boolean {
        if (!initialized) return false
        return try {
            backend.stopAllNetworks().success
        } catch (e: Exception) {
            Log.e(TAG, "stopAll error", e)
            false
        }
    }

    suspend fun collectNodeInfos(instanceName: String): List<NodeInfo> {
        if (!initialized) return emptyList()

        return try {
            backend.collectNodeInfos(instanceName)
        } catch (e: Exception) {
            Log.e(TAG, "collectNodeInfos error", e)
            emptyList()
        }
    }

    suspend fun collectNetworkInfoJson(): String? {
        if (!initialized) return null

        return try {
            backend.collectNetworkInfoJson()
        } catch (e: Exception) {
            Log.e(TAG, "collectNetworkInfoJson error", e)
            null
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

}
