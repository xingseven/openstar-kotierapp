package com.easytier.service

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.util.Log
import com.easytier.jni.EasyTierJNI
import kotlinx.coroutines.*

class EasyTierVpnService : VpnService() {
    private var job: Job? = null
    private var pfd: android.os.ParcelFileDescriptor? = null

    companion object {
        private const val TAG = "EasyTierVpn"
        private val DISCOVERY_ROUTES = listOf(
            "224.0.0.251/32",
            "224.0.0.252/32",
            "239.255.255.250/32",
            "255.255.255.255/32"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val instanceName = intent?.getStringExtra("instance_name") ?: run {
            Log.w(TAG, "missing instance_name"); stopSelf(startId); return START_NOT_STICKY
        }
        val ipv4 = intent.getStringExtra("ipv4") ?: ""
        val prefix = intent.getIntExtra("prefix", 24)
        val extraRoutes = intent.getStringArrayListExtra("routes") ?: arrayListOf()

        Log.i(TAG, "starting VPN: $instanceName @ $ipv4/$prefix routes=$extraRoutes")
        LogService.info("启动 VPN: $instanceName @ $ipv4/$prefix routes=$extraRoutes", source = TAG)

        job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val builder = Builder().apply {
                    setSession("EasyTier ($instanceName)")
                    setMtu(1500)
                    addAddress(ipv4, prefix)
                    try {
                        addDisallowedApplication(packageName)
                        Log.i(TAG, "disallowed self package from VPN: $packageName")
                        LogService.info("已将宿主包排除出 VPN: $packageName", source = TAG)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w(TAG, "failed to disallow self package", e)
                        LogService.warn("排除宿主包失败: ${e.message}", source = TAG)
                    }
                    // 自动添加虚拟子网路由，确保虚拟网络内其他节点的流量走 TUN 接口
                    try {
                        val networkAddr = computeNetworkAddress(ipv4, prefix)
                        addRoute(networkAddr, prefix)
                        Log.i(TAG, "added subnet route: $networkAddr/$prefix")
                        LogService.info("已添加子网路由: $networkAddr/$prefix", source = TAG)
                    } catch (e: Exception) {
                        Log.w(TAG, "failed to add subnet route", e)
                        LogService.warn("添加子网路由失败: ${e.message}", source = TAG)
                    }
                    (extraRoutes + DISCOVERY_ROUTES).distinct().forEach { route ->
                        val parts = route.split("/")
                        if (parts.size == 2) {
                            try {
                                addRoute(parts[0], parts[1].toInt())
                                LogService.debug("已添加额外路由: $route", source = TAG)
                            }
                            catch (e: Exception) {
                                LogService.warn("添加额外路由失败: $route, ${e.message}", source = TAG)
                            }
                        }
                    }
                    addDnsServer("223.5.5.5")
                }

                val pfdResult = builder.establish()
                if (pfdResult == null) {
                    Log.e(TAG, "establish returned null")
                    LogService.error("VPN establish 返回 null", source = TAG)
                    stopSelf(startId)
                    return@launch
                }
                pfd = pfdResult

                val fd = pfdResult.detachFd()
                val result = EasyTierJNI.setTunFd(instanceName, fd)
                Log.i(TAG, "setTunFd result=$result")
                if (result == 0) {
                    LogService.info("setTunFd 成功: instance=$instanceName fd=$fd", source = TAG)
                } else {
                    val errorMessage = EasyTierJNI.getLastError().orEmpty()
                    Log.e(TAG, "setTunFd failed: $errorMessage")
                    LogService.error("setTunFd 失败: code=$result error=$errorMessage", source = TAG)
                    stopSelf(startId)
                    return@launch
                }

                // 保持服务运行
                while (isActive) delay(5000)
            } catch (e: SecurityException) {
                Log.e(TAG, "VPN permission denied", e)
                LogService.error("VPN 权限被拒绝: ${e.message}", source = TAG)
                stopSelf(startId)
            } catch (e: Exception) {
                Log.e(TAG, "VPN error", e)
                LogService.error("VPN 异常: ${e.message}", source = TAG)
                stopSelf(startId)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        try { pfd?.close() } catch (e: Exception) {}
        pfd = null
        Log.d(TAG, "VPN stopped")
        LogService.info("VPN 已停止", source = TAG)
    }

    /** 根据 IP 和前缀长度计算网络地址（如 10.126.126.3/24 → 10.126.126.0） */
    private fun computeNetworkAddress(ipv4: String, prefix: Int): String {
        val parts = ipv4.split(".").map { it.toInt() }
        val ipInt = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        val networkInt = ipInt and mask
        return "${(networkInt ushr 24) and 0xFF}.${(networkInt ushr 16) and 0xFF}.${(networkInt ushr 8) and 0xFF}.${networkInt and 0xFF}"
    }
}
