package com.easytier.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.util.Log
import com.easytier.backend.AndroidAdapter
import com.easytier.backend.BackendClient
import com.easytier.backend.BackendResult
import com.easytier.backend.collectRunningInstanceNames
import com.easytier.backend.JsonRpcClient
import com.easytier.backend.jni.JniBackendClient
import com.easytier.backend.protocol.PingResult
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object EasyTierService {
    private const val TAG = "EasyTierService"
    private const val MONITOR_INTERVAL = 3000L

    private var initialized = false
    private val backend: BackendClient = JniBackendClient()
    private val jsonRpcClient: JsonRpcClient = JsonRpcClient(backend)
    private var adapter: AndroidAdapter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var appContext: Context? = null
    private var connectivityManager: ConnectivityManager? = null
    private var vpnNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private val _runtimeState = MutableStateFlow(RuntimeState())
    val runtimeState: StateFlow<RuntimeState> = _runtimeState.asStateFlow()

    data class RuntimeState(
        val runningInstances: Set<String> = emptySet(),
        val activeVpnInstanceName: String? = null,
    ) {
        fun isConnected(instanceName: String, requireVpn: Boolean = true): Boolean {
            val networkRunning = runningInstances.contains(instanceName)
            return networkRunning && (!requireVpn || activeVpnInstanceName == instanceName)
        }
    }

    data class EasyTierResult(val success: Boolean, val errorMessage: String = "") {
        companion object {
            fun ok() = EasyTierResult(true)
            fun fail(msg: String) = EasyTierResult(false, msg)
        }
    }

    private fun getAdapter(activity: Activity): AndroidAdapter {
        val existing = adapter
        if (existing != null) return existing
        val created = AndroidAdapter(jsonRpcClient, activity.applicationContext)
        adapter = created
        return created
    }

    fun initialize(context: Context? = null): Boolean {
        if (context != null) {
            appContext = context.applicationContext
            registerVpnNetworkCallback(context.applicationContext)
        }

        return try {
            val result = backend.initialize()
            initialized = result.success
            if (result.success) {
                Log.i(TAG, "Backend initialized")
                scope.launch {
                    // 清理上次运行残留的网络实例
                    backend.stopAllNetworks()
                    refreshRuntimeState()
                }
            } else {
                Log.e(TAG, "Backend init failed: ${result.errorMessage}")
                _runtimeState.value = RuntimeState()
            }
            result.success
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
            initialized = false
            _runtimeState.value = RuntimeState()
            false
        }
    }

    suspend fun ping(): PingResult {
        if (!initialized) return PingResult(ok = false, backendVersion = "uninitialized")
        return backend.ping()
    }

    suspend fun startNetwork(config: NetworkConfig): EasyTierResult {
        if (!initialized) return EasyTierResult.fail("not initialized")

        return try {
            val result = backend.startNetwork(config)
            if (result.success) {
                config.isRunning = true
                refreshRuntimeState()
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
                if (_runtimeState.value.activeVpnInstanceName == instanceName) {
                    notifyVpnStopped(instanceName)
                }
                refreshRuntimeState()
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
            val stopped = backend.stopAllNetworks().success
            if (stopped) {
                _runtimeState.value = RuntimeState()
            }
            stopped
        } catch (e: Exception) {
            Log.e(TAG, "stopAll error", e)
            false
        }
    }

    suspend fun refreshRuntimeState(): RuntimeState {
        if (!initialized) {
            _runtimeState.value = RuntimeState()
            return _runtimeState.value
        }

        val runningInstances = backend.collectNetworkInfoJson()
            ?.let(::collectRunningInstanceNames)
            .orEmpty()

        _runtimeState.update { current ->
            current.copy(
                runningInstances = runningInstances,
                activeVpnInstanceName = current.activeVpnInstanceName?.takeIf { it in runningInstances }
            )
        }

        return _runtimeState.value
    }

    fun notifyVpnStarted(instanceName: String) {
        _runtimeState.update {
            it.copy(activeVpnInstanceName = instanceName)
        }
    }

    fun notifyVpnStopped(instanceName: String? = _runtimeState.value.activeVpnInstanceName) {
        _runtimeState.update { current ->
            if (instanceName == null || current.activeVpnInstanceName == instanceName) {
                current.copy(activeVpnInstanceName = null)
            } else {
                current
            }
        }
    }

    fun handleVpnRevoked(instanceName: String?) {
        notifyVpnStopped(instanceName)
        if (instanceName.isNullOrBlank() || !initialized) {
            return
        }

        scope.launch {
            stopNetwork(instanceName)
            refreshRuntimeState()
        }
    }

    private fun registerVpnNetworkCallback(context: Context) {
        if (vpnNetworkCallback != null) {
            return
        }

        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    refreshRuntimeState()
                }
            }

            override fun onLost(network: Network) {
                val activeInstanceName = _runtimeState.value.activeVpnInstanceName ?: return
                LogService.warn("检测到系统 VPN 网络断开: $activeInstanceName", source = TAG)
                notifyVpnStopped(activeInstanceName)
                scope.launch {
                    stopNetwork(activeInstanceName)
                    refreshRuntimeState()
                }
            }
        }

        manager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build(),
            callback
        )

        connectivityManager = manager
        vpnNetworkCallback = callback
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

    fun createVpnPrepareIntent(activity: Activity): Intent? =
        getAdapter(activity).createVpnPrepareIntent()

    fun startVpnService(activity: Activity, instanceName: String, ipv4: String, prefix: Int = 24, routes: List<String> = emptyList()) {
        getAdapter(activity).startVpnService(instanceName, ipv4, prefix, routes)
    }

    fun stopVpnService(activity: Activity, instanceName: String? = null) {
        if (instanceName != null && _runtimeState.value.activeVpnInstanceName != instanceName) {
            Log.d(TAG, "skip stopVpnService: $instanceName is not the active VPN instance")
            return
        }
        val existing = adapter
        if (existing != null) {
            existing.stopVpnService()
        } else {
            activity.stopService(Intent(activity, EasyTierVpnService::class.java))
        }
    }

    fun isVpnInUseByOther(instanceName: String): Boolean {
        val active = _runtimeState.value.activeVpnInstanceName
        return active != null && active != instanceName
    }

    fun getActiveVpnInstanceName(): String? = _runtimeState.value.activeVpnInstanceName
}
