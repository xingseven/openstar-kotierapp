package com.easytier.service

import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.easytier.jni.EasyTierJNI
import kotlinx.coroutines.*
import org.json.JSONObject

class EasyTierVpnService : VpnService() {
    private var job: Job? = null

    companion object {
        private const val TAG = "EasyTierVpn"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val instanceName = intent?.getStringExtra("instance_name") ?: run {
            Log.w(TAG, "missing instance_name"); stopSelf(startId); return START_NOT_STICKY
        }
        val ipv4 = intent.getStringExtra("ipv4") ?: ""
        val prefix = intent.getIntExtra("prefix", 24)
        val extraRoutes = intent.getStringArrayListExtra("routes") ?: arrayListOf()

        Log.i(TAG, "starting VPN: $instanceName @ $ipv4/$prefix routes=$extraRoutes")

        job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val builder = Builder().apply {
                    setSession("EasyTier ($instanceName)")
                    setMtu(1500)
                    addAddress(ipv4, prefix)
                    // 自动添加虚拟子网路由，确保虚拟网络内其他节点的流量走 TUN 接口
                    try {
                        val networkAddr = computeNetworkAddress(ipv4, prefix)
                        addRoute(networkAddr, prefix)
                        Log.i(TAG, "added subnet route: $networkAddr/$prefix")
                    } catch (e: Exception) {
                        Log.w(TAG, "failed to add subnet route", e)
                    }
                    extraRoutes.forEach { route ->
                        val parts = route.split("/")
                        if (parts.size == 2) {
                            try { addRoute(parts[0], parts[1].toInt()) }
                            catch (_: Exception) {}
                        }
                    }
                    addDnsServer("223.5.5.5")
                }

                val pfd = builder.establish()
                if (pfd == null) {
                    Log.e(TAG, "establish returned null"); stopSelf(startId); return@launch
                }

                val fd = pfd.fd
                val result = EasyTierJNI.setTunFd(instanceName, fd)
                Log.i(TAG, "setTunFd result=$result")

                // 保持服务运行
                while (isActive) delay(5000)
            } catch (e: SecurityException) {
                Log.e(TAG, "VPN permission denied", e)
                stopSelf(startId)
            } catch (e: Exception) {
                Log.e(TAG, "VPN error", e)
                stopSelf(startId)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        Log.d(TAG, "VPN stopped")
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
