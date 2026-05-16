package com.easytier.ui.pages

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.easytier.data.ServerEntry
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.easytier.R
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import com.easytier.service.EasyTierService
import com.easytier.ui.components.AppDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NavItem(
    val label: String,
    val iconRes: Int,
)

private val navItems = listOf(
    NavItem("首页", R.drawable.ic_nav_network),
    NavItem("联机", R.drawable.ic_nav_online),
    NavItem("服务器", R.drawable.ic_nav_server),
    NavItem("我的", R.drawable.ic_nav_setup),
)

@Composable
fun HomePage() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showNetworkPage by rememberSaveable { mutableStateOf(false) }
    var showLogPage by remember { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background
    val isDashboard = !showNetworkPage && !showLogPage && selectedIndex == 0

    BackHandler(enabled = showNetworkPage || showLogPage) {
        when {
            showLogPage -> showLogPage = false
            showNetworkPage -> showNetworkPage = false
        }
    }

    LaunchedEffect(isDashboard, background, view) {
        val activity = view.context as? Activity ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(activity.window, view)
        if (isDashboard) {
            activity.window.statusBarColor = Color(0xFF1F6FFF).toArgb()
            activity.window.navigationBarColor = Color.White.toArgb()
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = true
        } else {
            activity.window.statusBarColor = Color.Transparent.toArgb()
            activity.window.navigationBarColor = background.toArgb()
            val lightBars = background.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = lightBars
            controller.isAppearanceLightNavigationBars = lightBars
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F4F8),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navItems.forEachIndexed { index, item ->
                        val selected = !showNetworkPage && selectedIndex == index
                        val tint = if (selected) Color(0xFF1F6FFF) else Color(0xFF98A2B3)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    showNetworkPage = false
                                    showLogPage = false
                                    selectedIndex = index
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.label,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = tint,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            color = Color(0xFFF2F4F8),
        ) {
            when {
                showLogPage -> LogPage(onBack = { showLogPage = false })
                showNetworkPage -> NetworkConfigPage()
                else -> stateHolder.SaveableStateProvider("tab_$selectedIndex") {
                    when (selectedIndex) {
                        0 -> DashboardScreen(
                            onOpenNetwork = { showNetworkPage = true },
                            onOpenOneClick = { selectedIndex = 1 },
                            onOpenServers = { selectedIndex = 2 },
                        )

                        1 -> OneClickPage()
                        2 -> ServersPage()
                        3 -> SettingsPage(onNavigateToLog = { showLogPage = true })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    onOpenNetwork: () -> Unit,
    onOpenOneClick: () -> Unit,
    onOpenServers: () -> Unit,
) {
    val context = LocalContext.current as Activity
    val repo = LocalSettingsRepository.current
    val scope = rememberCoroutineScope()
    val runtimeState by EasyTierService.runtimeState.collectAsState()

    var configs by remember { mutableStateOf(repo.loadNetworkConfigs()) }
    var nodes by remember { mutableStateOf<List<NodeInfo>>(emptyList()) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showNetworkConfigDialog by remember { mutableStateOf(false) }
    var showServerManagerDialog by remember { mutableStateOf(false) }
    var switchingInstance by remember { mutableStateOf<String?>(null) }
    var pendingVpnConfig by remember { mutableStateOf<NetworkConfig?>(null) }

    var downloadHistory by remember { mutableStateOf(List(30) { 0f }) }
    var uploadHistory by remember { mutableStateOf(List(30) { 0f }) }
    var currentDownloadRate by remember { mutableStateOf(0f) }
    var currentUploadRate by remember { mutableStateOf(0f) }

    val activeConfig = remember(configs, runtimeState.runningInstances) {
        val runningName = runtimeState.runningInstances.firstOrNull()
        configs.firstOrNull { it.instanceName == runningName } ?: configs.firstOrNull()
    }

    val isRunning = activeConfig?.instanceName?.let { runtimeState.runningInstances.contains(it) } == true

    LaunchedEffect(Unit) {
        EasyTierService.refreshRuntimeState()
        configs = repo.loadNetworkConfigs()
    }

    LaunchedEffect(activeConfig?.instanceName, isRunning) {
        val instanceName = activeConfig?.instanceName
        if (!isRunning || instanceName.isNullOrBlank()) {
            nodes = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            nodes = EasyTierService.collectNodeInfos(instanceName)
            delay(3000)
        }
    }

    // 实时网速追踪
    LaunchedEffect(activeConfig?.instanceName, isRunning) {
        val instanceName = activeConfig?.instanceName
        if (!isRunning || instanceName.isNullOrBlank()) {
            downloadHistory = List(30) { 0f }
            uploadHistory = List(30) { 0f }
            currentDownloadRate = 0f
            currentUploadRate = 0f
            return@LaunchedEffect
        }
        delay(500)
        val initialNodes = EasyTierService.collectNodeInfos(instanceName)
        var prevRx = initialNodes.sumOf { it.rxBytes }
        var prevTx = initialNodes.sumOf { it.txBytes }
        while (true) {
            delay(2000)
            val currentNodes = EasyTierService.collectNodeInfos(instanceName)
            val curRx = currentNodes.sumOf { it.rxBytes }
            val curTx = currentNodes.sumOf { it.txBytes }
            val rxRate = ((curRx - prevRx).toFloat() / 2f).coerceAtLeast(0f)
            val txRate = ((curTx - prevTx).toFloat() / 2f).coerceAtLeast(0f)
            prevRx = curRx
            prevTx = curTx
            currentDownloadRate = rxRate
            currentUploadRate = txRate
            downloadHistory = (downloadHistory.drop(1) + rxRate).toList()
            uploadHistory = (uploadHistory.drop(1) + txRate).toList()
        }
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val cfg = pendingVpnConfig ?: return@rememberLauncherForActivityResult
        pendingVpnConfig = null
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                startVpnForConfig(context, cfg)
                EasyTierService.refreshRuntimeState()
                switchingInstance = null
            }
        } else {
            switchingInstance = null
            Toast.makeText(context, "VPN 授权被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    fun startConfig(cfg: NetworkConfig) {
        if (cfg.networkName.isBlank()) {
            Toast.makeText(context, "请先到网络页配置网络", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            switchingInstance = cfg.instanceName

            // 点击已在运行的节点 → 关闭它
            if (runtimeState.runningInstances.contains(cfg.instanceName)) {
                EasyTierService.stopNetwork(cfg.instanceName)
                EasyTierService.refreshRuntimeState()
                switchingInstance = null
                return@launch
            }

            // 先停掉其他运行中的实例，保证只运行一个
            for (name in runtimeState.runningInstances) {
                EasyTierService.stopNetwork(name)
            }

            val result = EasyTierService.startNetwork(cfg)
            if (!result.success) {
                Toast.makeText(context, "启动失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                switchingInstance = null
                return@launch
            }

            if (cfg.noTun) {
                EasyTierService.refreshRuntimeState()
                switchingInstance = null
                return@launch
            }

            val intent: Intent? = VpnService.prepare(context)
            if (intent != null) {
                pendingVpnConfig = cfg
                vpnLauncher.launch(intent)
            } else {
                startVpnForConfig(context, cfg)
            }
            EasyTierService.refreshRuntimeState()
            switchingInstance = null
        }
    }

    val localNode = nodes.firstOrNull { it.isLocal }
    val peerNodes = nodes.filterNot { it.isLocal }
    val peerCount = peerNodes.size
    val avgLatency = peerNodes.map { it.latencyMs }.filter { it > 0 }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val topLatency = peerNodes.map { it.latencyMs }.filter { it > 0 }.minOrNull() ?: 0
    val totalRx = nodes.sumOf { it.rxBytes }
    val totalTx = nodes.sumOf { it.txBytes }

    if (showAddNodeDialog) {
        var deviceName by remember { mutableStateOf("") }
        var networkName by remember { mutableStateOf("") }
        var networkSecret by remember { mutableStateOf("") }

        AppDialog(
            title = "添加节点",
            onDismissRequest = { showAddNodeDialog = false },
            confirmText = "添加",
            confirmEnabled = networkName.isNotBlank() && networkSecret.isNotBlank(),
            onConfirm = {
                val network = networkName.trim()
                val secret = networkSecret.trim()
                if (network.isBlank() || secret.isBlank()) {
                    return@AppDialog
                }
                val updatedConfigs = repo.loadNetworkConfigs().toMutableList()
                val newConfig = NetworkConfig().apply {
                    hostname = deviceName.trim()
                    networkLabel = deviceName.trim().ifBlank { network }
                    networkName = network
                    networkSecret = secret
                    this.isRunning = false
                }
                updatedConfigs.add(newConfig)
                repo.saveNetworkConfigs(updatedConfigs)
                configs = updatedConfigs

                showAddNodeDialog = false
                Toast.makeText(context, "设备配置已添加", Toast.LENGTH_SHORT).show()
            },
        ) {
            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("设备名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = networkName,
                onValueChange = { networkName = it },
                label = { Text("网络名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = networkSecret,
                onValueChange = { networkSecret = it },
                label = { Text("网络密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // 网络配置弹窗（只读查看）
    if (showNetworkConfigDialog) {
        val cfg = activeConfig
        if (cfg == null) {
            showNetworkConfigDialog = false
        } else {
            var showAdvanced by remember { mutableStateOf(false) }
            var secretVisible by remember { mutableStateOf(false) }

            BasicAlertDialog(onDismissRequest = { showNetworkConfigDialog = false }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("网络配置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ConfigInfoRow("配置标签", cfg.networkLabel.ifBlank { "--" })
                            ConfigInfoRow("本机主机名", cfg.hostname.ifBlank { "--" })
                            ConfigInfoRow("网络名称", cfg.networkName.ifBlank { "--" })
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("网络密钥", color = Color(0xFF98A2B3), fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (secretVisible) cfg.networkSecret else "••••••••",
                                    color = Color(0xFF111827),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                IconButton(
                                    onClick = { secretVisible = !secretVisible },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        if (secretVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            ConfigInfoRow("DHCP 自动分配", if (cfg.dhcp) "是" else "否")
                            if (!cfg.dhcp && cfg.ipv4.isNotBlank()) {
                                ConfigInfoRow("静态 IPv4", cfg.ipv4)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEFF2F6))
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "收起详情" else "展开详情", fontSize = 13.sp)
                        }
                        AnimatedVisibility(visible = showAdvanced) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ReadOnlySwitchRow("启用加密", cfg.enableEncryption)
                                ReadOnlySwitchRow("禁用 P2P", cfg.disableP2p)
                                ReadOnlySwitchRow("低延迟优先", cfg.latencyFirst)
                                ReadOnlySwitchRow("私有模式", cfg.privateMode)
                                ReadOnlySwitchRow("无 TUN 模式", cfg.noTun)
                                ReadOnlySwitchRow("禁用 IPv6", cfg.disableIpv6)
                                ReadOnlySwitchRow("禁用 UDP 打洞", cfg.disableUdpHolePunching)
                                ReadOnlySwitchRow("禁用 TCP 打洞", cfg.disableTcpHolePunching)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { showNetworkConfigDialog = false }) {
                                Text("关闭")
                            }
                        }
                    }
                }
            }
        }
    }

    // 节点管理弹窗
    if (showServerManagerDialog) {
        var servers by remember { mutableStateOf(repo.loadFavoriteServers()) }
        var newUrl by remember { mutableStateOf("") }

        AppDialog(
            title = "节点管理",
            onDismissRequest = { showServerManagerDialog = false },
            confirmText = "确定",
            onConfirm = {
                repo.saveFavoriteServers(servers)
                showServerManagerDialog = false
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    placeholder = { Text("服务器地址") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newUrl.isNotBlank()) {
                            servers = (servers + ServerEntry(
                                name = newUrl.trim(),
                                url = newUrl.trim()
                            )).toMutableList()
                            newUrl = ""
                        }
                    },
                    shape = RoundedCornerShape(499.5.dp),
                    modifier = Modifier.height(46.dp),
                ) { Text("添加") }
            }
            if (servers.isEmpty()) {
                Text("暂无服务器", color = Color(0xFF98A2B3), fontSize = 12.sp)
            } else {
                servers.forEachIndexed { idx, server ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                server.url,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            )
                            IconButton(onClick = {
                                servers = servers.toMutableList().also { it.removeAt(idx) }
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp,
                        ),
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1F6FFF), Color(0xFF2A8CFF)),
                        ),
                    ),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_beijing),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .requiredSize(width = 300.dp, height = 220.dp)
                        .align(Alignment.TopEnd)
                        .offset(y = 80.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .statusBarsPadding()
                        .padding(top = 6.dp, bottom = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Kotier内网穿透",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "组网互联和设备在线率实时监控",
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xCC2F80E9)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = "连接节点",
                                    color = Color.White.copy(alpha = 0.86f),
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = peerCount.toString(),
                                    color = Color.White,
                                    fontSize = 50.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 52.sp,
                                )
                                Text(
                                    text = localNode?.hostname?.ifBlank { "本机" } ?: "本机",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "延迟",
                                    color = Color.White.copy(alpha = 0.86f),
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (avgLatency > 0) "${avgLatency}ms" else "--",
                                    color = Color.White,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                colors = CardDefaults.cardColors(containerColor = Color(0xCC2F80E9)),
                            ) {
                                NetworkChart(
                                    downloadSamples = downloadHistory,
                                    uploadSamples = uploadHistory,
                                    downloadRate = currentDownloadRate,
                                    uploadRate = currentUploadRate,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    }
                }
            }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("连接状态", color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isRunning) "网络实例: ${activeConfig?.networkLabel?.ifBlank { activeConfig?.instanceName } ?: "--"}" else "当前未连接",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (localNode?.virtualIp.isNullOrBlank()) "--" else localNode?.virtualIp.orEmpty(),
                            color = Color(0xFF111827),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            "下载 ${NodeInfo.formatBytes(totalRx)} / 上传 ${NodeInfo.formatBytes(totalTx)}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("延迟状态", color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = if (topLatency > 0) "最快 ${topLatency}ms" else "暂无",
                                color = Color(0xFF1F6FFF),
                                fontSize = 11.sp,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (avgLatency > 0) "平均 ${avgLatency}ms" else "暂无延迟数据",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MetricActionItem(
                                icon = Icons.Rounded.Wifi,
                                title = "网络配置",
                                onClick = { showNetworkConfigDialog = true },
                            )
                            MetricActionItem(
                                icon = Icons.Rounded.Speed,
                                title = "一键联机",
                                onClick = onOpenOneClick,
                            )
                            MetricActionItem(
                                icon = Icons.Rounded.Dns,
                                title = "节点管理",
                                onClick = { showServerManagerDialog = true },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "设备列表",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { showAddNodeDialog = true },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color(0xFF1F6FFF),
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "添加节点",
                                fontSize = 12.sp,
                                color = Color(0xFF1F6FFF),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (configs.isEmpty()) {
                        Text(
                            text = "暂无配置，点击右上角添加节点",
                            color = Color(0xFF98A2B3),
                            fontSize = 12.sp,
                        )
                    } else {
                        configs.forEachIndexed { index, cfg ->
                            val isCfgRunning = runtimeState.runningInstances.contains(cfg.instanceName)
                            ConfigRow(
                                config = cfg,
                                isRunning = isCfgRunning,
                                onStart = { startConfig(cfg) },
                                onEdit = onOpenNetwork,
                                onDelete = {
                                    val updated = configs.toMutableList()
                                    updated.removeAt(index)
                                    repo.saveNetworkConfigs(updated)
                                    configs = updated
                                },
                                enabled = switchingInstance == null || switchingInstance != cfg.instanceName,
                            )
                            if (index < configs.lastIndex) {
                                HorizontalDivider(color = Color(0xFFEFF2F6))
                            }
                        }
                    }

                    if (isRunning) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFFEFF2F6),
                        )
                        Text(
                            text = "在线节点",
                            color = Color(0xFF98A2B3),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (nodes.isEmpty()) {
                            Text(
                                text = "等待节点数据...",
                                color = Color(0xFF98A2B3),
                                fontSize = 12.sp,
                            )
                        } else {
                            nodes.forEachIndexed { index, item ->
                                DeviceRow(item)
                                if (index < nodes.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFEFF2F6))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun startVpnForConfig(context: Activity, cfg: NetworkConfig) {
    var assignedIp = if (!cfg.dhcp && cfg.ipv4.isNotBlank()) {
        NetworkConfig.vpnIpv4Address(cfg.ipv4)
    } else {
        ""
    }

    if (assignedIp.isBlank()) {
        repeat(20) {
            val currentNodes = EasyTierService.collectNodeInfos(cfg.instanceName)
            val local = currentNodes.find { it.isLocal }
            if (local != null && local.virtualIp.isNotBlank()) {
                assignedIp = local.virtualIp
                return@repeat
            }
            delay(300)
        }
    }

    EasyTierService.startVpnService(
        context,
        cfg.instanceName,
        assignedIp.ifBlank { NetworkConfig.vpnIpv4Address(cfg.ipv4).ifBlank { "10.144.144.1" } },
        24,
        emptyList(),
    )
}

@Composable
private fun MetricActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFFF0F5FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = Color(0xFF1F6FFF), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 11.sp, color = Color(0xFF98A2B3))
    }
}

@Composable
private fun DeviceRow(node: NodeInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(if (node.isLocal) Color(0xFFEAF3FF) else Color(0xFFF5F8FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = Color(0xFF1F6FFF),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (node.hostname.isBlank()) "未命名节点" else node.hostname,
                color = Color(0xFF111827),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = buildString {
                append(if (node.virtualIp.isBlank()) "IP --" else "IP ${node.virtualIp}")
                if (node.latencyMs > 0) {
                    append(" / ${node.latencyMs}ms")
                }
                if (node.connectionType.isNotBlank() && node.connectionType != "unknown") {
                    append(" / ${node.connectionType}")
                }
            }
            Text(text = detail, color = Color(0xFF98A2B3), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            text = if (node.isLocal) "本机" else "在线",
            color = Color(0xFF1F6FFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConfigRow(
    config: NetworkConfig,
    isRunning: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F8FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = Color(0xFF1F6FFF),
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.networkLabel.ifBlank { config.hostname.ifBlank { "未命名节点" } },
                color = Color(0xFF111827),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "网络: ${config.networkName}",
                color = Color(0xFF98A2B3),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多操作",
                    tint = Color(0xFF98A2B3),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("修改配置") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除配置", color = Color(0xFFE53E3E)) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
        Switch(
            checked = isRunning,
            onCheckedChange = { onStart() },
            enabled = enabled,
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun ConfigInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color(0xFF98A2B3), fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = Color(0xFF111827),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReadOnlySwitchRow(label: String, checked: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NetworkChart(
    downloadSamples: List<Float>,
    uploadSamples: List<Float>,
    downloadRate: Float,
    uploadRate: Float,
    modifier: Modifier = Modifier,
) {
    val maxValue = remember(downloadSamples, uploadSamples) {
        (downloadSamples + uploadSamples).maxOrNull()?.coerceAtLeast(1f) ?: 1f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = "↓ ${formatSpeed(downloadRate)}",
                color = Color(0xFF67E8F9),
                fontSize = 9.sp,
            )
            Text(
                text = "↑ ${formatSpeed(uploadRate)}",
                color = Color(0xFF4ADE80),
                fontSize = 9.sp,
            )
        }
        Spacer(Modifier.height(2.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            val w = size.width
            val h = size.height
            val p = 4f

            val gridColor = Color.White.copy(alpha = 0.1f)
            for (i in 0..2) {
                val y = p + (h - p * 2) * i / 2
                drawLine(gridColor, Offset(p, y), Offset(w - p, y), strokeWidth = 0.5f)
            }

            fun drawLinePath(
                samples: List<Float>,
                lineColor: Color,
                fillColor: Color,
            ) {
                if (samples.size < 2) return
                val stepX = (w - p * 2) / (samples.size - 1)
                val path = Path()
                samples.forEachIndexed { index, value ->
                    val x = p + index * stepX
                    val ratio = (value / maxValue).coerceIn(0f, 1f)
                    val y = h - p - (h - p * 2) * ratio
                    if (index == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(p + (samples.size - 1) * stepX, h - p)
                    lineTo(p, h - p)
                    close()
                }
                drawPath(fillPath, fillColor)
                drawPath(path, lineColor, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            drawLinePath(downloadSamples, Color(0xFF67E8F9), Color(0xFF67E8F9).copy(alpha = 0.12f))
            drawLinePath(uploadSamples, Color(0xFF4ADE80), Color(0xFF4ADE80).copy(alpha = 0.12f))
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "实时网速",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
        )
    }
}

private fun formatSpeed(bytesPerSec: Float): String {
    return when {
        bytesPerSec >= 1_000_000 -> String.format("%.1f MB/s", bytesPerSec / 1_000_000)
        bytesPerSec >= 1_000 -> String.format("%.1f KB/s", bytesPerSec / 1_000)
        bytesPerSec > 0 -> String.format("%.0f B/s", bytesPerSec)
        else -> "0 B/s"
    }
}
