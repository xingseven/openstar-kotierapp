package com.easytier.ui.pages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.easytier.ui.components.CompactTopBar
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.backend.collectProxyCidrsFromJson
import com.easytier.backend.decodeConnectionCode
import com.easytier.backend.encodeConnectionCode
import com.easytier.backend.generateRoomCredentials
import com.easytier.data.NetworkConfig
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.service.EasyTierService
import com.easytier.service.OneClickSessionSnapshot
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneClickPage() {
    val context = LocalContext.current as Activity
    val lifecycleOwner = context as LifecycleOwner
    val scope = rememberCoroutineScope()
    val repo = LocalSettingsRepository.current
    val runtimeState by EasyTierService.runtimeState.collectAsState()

    var isHostMode by rememberSaveable { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var hostSession by remember { mutableStateOf(repo.loadOneClickHostSession()) }
    var guestSession by remember { mutableStateOf(repo.loadOneClickGuestSession()) }
    var guestCode by rememberSaveable { mutableStateOf(repo.oneClickGuestCodeDraft) }
    var statusMessage by rememberSaveable { mutableStateOf("") }
    var statusIsError by rememberSaveable { mutableStateOf(false) }
    var runtimeReady by remember { mutableStateOf(false) }
    var pendingStartIsHost by remember { mutableStateOf<Boolean?>(null) }
    var pendingStartIp by remember { mutableStateOf("") }
    var pendingStartRoutes by remember { mutableStateOf<List<String>>(emptyList()) }

    val hostConfig = hostSession?.toNetworkConfig()
    val guestConfig = guestSession?.toNetworkConfig()
    val hostRunning = hostSession?.let { runtimeState.isConnected(it.instanceName) } == true
    val guestRunning = guestSession?.let { runtimeState.isConnected(it.instanceName) } == true
    val currentOneClickInstances = setOfNotNull(hostSession?.instanceName, guestSession?.instanceName)
    val otherRunningInstances = runtimeState.runningInstances - currentOneClickInstances

    val setLoading: (Boolean) -> Unit = { isLoading = it }
    val setStatus: (String, Boolean) -> Unit = { msg, err -> statusMessage = msg; statusIsError = err }

    fun saveHostSession(session: OneClickSessionSnapshot?) {
        hostSession = session
        repo.saveOneClickHostSession(session)
    }

    fun saveGuestSession(session: OneClickSessionSnapshot?) {
        guestSession = session
        repo.saveOneClickGuestSession(session)
    }

    suspend fun awaitVpnStartData(config: NetworkConfig): Pair<String, List<String>> {
        var assignedIp = if (!config.dhcp && config.ipv4.isNotBlank()) {
            NetworkConfig.vpnIpv4Address(config.ipv4)
        } else {
            ""
        }
        var routes = emptyList<String>()

        repeat(if (assignedIp.isEmpty()) 10 else 3) {
            val nodes = EasyTierService.collectNodeInfos(config.instanceName)
            val localNode = nodes.find { it.isLocal }
            if (localNode != null && localNode.virtualIp.isNotEmpty()) {
                assignedIp = localNode.virtualIp
            }

            routes = EasyTierService.collectNetworkInfoJson()
                ?.let { collectProxyCidrsFromJson(it, config.instanceName) }
                .orEmpty()

            if (assignedIp.isNotEmpty()) {
                return assignedIp to routes
            }
            delay(300)
        }

        return assignedIp.ifEmpty { "10.144.144.1" } to routes
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isHost = pendingStartIsHost ?: return@rememberLauncherForActivityResult
        val snapshot = if (isHost) hostSession else guestSession
        val cfg = snapshot?.toNetworkConfig() ?: return@rememberLauncherForActivityResult
        val assignedIp = pendingStartIp.ifEmpty {
            NetworkConfig.vpnIpv4Address(cfg.ipv4).ifEmpty { "10.144.144.1" }
        }
        val routes = pendingStartRoutes
        pendingStartIsHost = null
        pendingStartIp = ""
        pendingStartRoutes = emptyList()

        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                EasyTierService.startVpnService(context, cfg.instanceName, assignedIp, 24, routes)
                setLoading(false)
                setStatus(
                    if (isHost) "网络已启动，分享下方编码给好友" else "已成功加入网络 ${cfg.networkName}",
                    false
                )
            }
        } else {
            scope.launch {
                EasyTierService.stopNetwork(cfg.instanceName)
                if (isHost) {
                    saveHostSession(null)
                } else {
                    saveGuestSession(null)
                }
                setStatus("VPN 授权被拒绝", true); setLoading(false)
            }
        }
    }

    fun requestVpnAndStart(isHost: Boolean, config: NetworkConfig, assignedIp: String, routes: List<String>) {
        pendingStartIsHost = isHost
        pendingStartIp = assignedIp
        pendingStartRoutes = routes

        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            EasyTierService.startVpnService(context, config.instanceName, assignedIp, 24, routes)
            setLoading(false)
            setStatus(
                if (isHost) "网络已启动，分享下方编码给好友" else "已成功加入网络 ${config.networkName}",
                false
            )
        }
    }

    LaunchedEffect(Unit) {
        EasyTierService.refreshRuntimeState()
        runtimeReady = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    EasyTierService.refreshRuntimeState()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(runtimeReady, runtimeState.runningInstances, hostSession?.instanceName, guestSession?.instanceName) {
        if (!runtimeReady) {
            return@LaunchedEffect
        }

        suspend fun syncSession(
            session: OneClickSessionSnapshot?,
            save: (OneClickSessionSnapshot?) -> Unit,
        ) {
            if (session == null) return
            if (!runtimeState.runningInstances.contains(session.instanceName)) {
                if (!isLoading) {
                    save(null)
                }
                return
            }

            val virtualIp = EasyTierService.collectNodeInfos(session.instanceName)
                .find { it.isLocal }
                ?.virtualIp
                .orEmpty()

            if (virtualIp.isNotBlank() && virtualIp != session.virtualIp) {
                save(session.copy(virtualIp = virtualIp))
            }
        }

        syncSession(hostSession, ::saveHostSession)
        syncSession(guestSession, ::saveGuestSession)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { CompactTopBar("一键联机") }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 模式切换
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Box(Modifier.weight(1f)) {
                ModeButton(
                    selected = isHostMode,
                    icon = AppIcons.Lan,
                    label = "创建网络（房主）",
                    onClick = { isHostMode = true; statusMessage = "" }
                )
            }
            Box(Modifier.weight(1f)) {
                ModeButton(
                    selected = !isHostMode,
                    icon = AppIcons.GroupAdd,
                    label = "加入网络（房客）",
                    onClick = { isHostMode = false; statusMessage = "" }
                )
            }
        }
    }

            if (isHostMode) {
                HostMode(
                    isRunning = hostRunning,
                    isLoading = isLoading,
                    generatedCode = hostSession?.generatedCode.orEmpty(),
                    hostConfig = hostConfig,
                    virtualIp = hostSession?.virtualIp.orEmpty(),
                    onStart = {
                        scope.launch {
                            setLoading(true); setStatus("", false)
                            if (otherRunningInstances.isNotEmpty()) {
                                setLoading(false)
                                setStatus("请先停止网络页或其他已运行实例", true)
                                return@launch
                            }
                            if (guestRunning) {
                                setLoading(false)
                                setStatus("请先离开当前已加入的网络", true)
                                return@launch
                            }

                            val (netId, password) = generateRoomCredentials()
                            val config = NetworkConfig.createOneClickHostConfig(netId, password)
                            val result = EasyTierService.startNetwork(config)
                            if (!result.success) {
                                setLoading(false); setStatus("启动失败: ${result.errorMessage}", true); return@launch
                            }
                            val encoded = encodeConnectionCode(netId, password)
                            saveHostSession(OneClickSessionSnapshot.fromConfig(config, generatedCode = encoded))

                            val (assignedIp, routes) = awaitVpnStartData(config)
                            if (assignedIp.isNotBlank()) {
                                saveHostSession(
                                    OneClickSessionSnapshot.fromConfig(
                                        config,
                                        generatedCode = encoded,
                                        virtualIp = assignedIp,
                                    )
                                )
                            }

                            requestVpnAndStart(true, config, assignedIp, routes)
                        }
                    },
                    onStop = {
                        scope.launch {
                            setLoading(true)
                            EasyTierService.stopVpnService(context)
                            hostConfig?.let { EasyTierService.stopNetwork(it.instanceName) }
                            saveHostSession(null)
                            setLoading(false)
                            setStatus("网络已停止", false)
                        }
                    },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("code", hostSession?.generatedCode.orEmpty()))
                        setStatus("已复制到剪贴板", false)
                    }
                )
            } else {
                GuestMode(
                    guestCode = guestCode,
                    onGuestCodeChange = {
                        guestCode = it
                        repo.oneClickGuestCodeDraft = it
                    },
                    isRunning = guestRunning,
                    isLoading = isLoading,
                    hostConfig = guestConfig,
                    virtualIp = guestSession?.virtualIp.orEmpty(),
                    onJoin = {
                        scope.launch {
                            if (otherRunningInstances.isNotEmpty()) {
                                setStatus("请先停止网络页或其他已运行实例", true)
                                return@launch
                            }
                            if (hostRunning) {
                                setStatus("请先停止当前房主网络", true)
                                return@launch
                            }

                            val code = guestCode.trim()
                            if (code.isBlank()) {
                                setStatus("请输入联机编码", true); return@launch
                            }
                            setLoading(true); setStatus("", false)
                            try {
                                val (netId, password) = decodeConnectionCode(code)
                                if (netId.isEmpty() || password.isEmpty()) {
                                    setLoading(false); setStatus("联机码无效", true); return@launch
                                }
                                val config = NetworkConfig.createOneClickGuestConfig(netId, password)
                                val result = EasyTierService.startNetwork(config)
                                if (!result.success) {
                                    setLoading(false); setStatus("加入失败: ${result.errorMessage}", true); return@launch
                                }
                                saveGuestSession(OneClickSessionSnapshot.fromConfig(config))

                                val (assignedIp, routes) = awaitVpnStartData(config)
                                if (assignedIp.isNotBlank()) {
                                    saveGuestSession(
                                        OneClickSessionSnapshot.fromConfig(
                                            config,
                                            virtualIp = assignedIp,
                                        )
                                    )
                                }

                                requestVpnAndStart(false, config, assignedIp, routes)
                            } catch (e: Exception) {
                                android.util.Log.e("OneClick", "join failed", e)
                                setLoading(false); setStatus("编码解析失败: ${e.message}", true)
                            }
                        }
                    },
                    onLeave = {
                        scope.launch {
                            setLoading(true)
                            EasyTierService.stopVpnService(context)
                            guestConfig?.let { EasyTierService.stopNetwork(it.instanceName) }
                            saveGuestSession(null)
                            setLoading(false)
                            setStatus("网络已停止", false)
                        }
                    }
                )
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = if (statusIsError) Color(0xFFEF5350) else Color(0xFF4CAF50),
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// ── UI 子组件 ──

@Composable
private fun ModeButton(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "modeBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, label = "modeText"
    )
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = bgColor, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
        }
    }
}

@Composable
private fun HostMode(
    isRunning: Boolean, isLoading: Boolean, generatedCode: String,
    hostConfig: NetworkConfig?, virtualIp: String,
    onStart: () -> Unit, onStop: () -> Unit, onCopy: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIcon(
                if (isRunning) AppIcons.Wifi else AppIcons.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(if (isRunning) "网络运行中" else "点击下方按钮创建网络", fontWeight = FontWeight.SemiBold)
            if (isRunning && hostConfig != null) {
                Text("网络: ${hostConfig.networkName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (virtualIp.isNotBlank()) {
                    Text(
                        "虚拟 IP: $virtualIp",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { if (isRunning) onStop() else onStart() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = if (isRunning) ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                         else ButtonDefaults.buttonColors()
            ) {
                AppIcon(if (isRunning) AppIcons.Stop else AppIcons.Play, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(when { isLoading && isRunning -> "停止中..."; isLoading -> "启动中..."
                     isRunning -> "停止网络"; else -> "启动网络" })
            }
        }
    }

    if (generatedCode.isNotEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("联机编码", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onCopy) {
                        AppIcon(AppIcons.Copy, contentDescription = "复制", modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SelectionContainer { Text(generatedCode, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(10.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                Text("将此编码发送给好友，对方在一键联机页面选择\"加入网络\"并粘贴即可", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GuestMode(
    guestCode: String, onGuestCodeChange: (String) -> Unit,
    isRunning: Boolean, isLoading: Boolean, hostConfig: NetworkConfig?,
    virtualIp: String,
    onJoin: () -> Unit, onLeave: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("输入联机编码", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = guestCode, onValueChange = onGuestCodeChange,
                placeholder = { Text("粘贴房主分享的编码") },
                modifier = Modifier.fillMaxWidth().height(88.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onJoin, enabled = !isLoading && !isRunning,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                AppIcon(AppIcons.Login, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isLoading) "加入中..." else "加入网络")
            }
        }
    }

    if (isRunning && hostConfig != null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("已加入网络", fontWeight = FontWeight.SemiBold)
                    Text(hostConfig.networkName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (virtualIp.isNotBlank()) {
                        Text(
                            "本机虚拟 IP: $virtualIp",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                TextButton(onClick = onLeave) { Text("离开") }
            }
        }
    }
}

