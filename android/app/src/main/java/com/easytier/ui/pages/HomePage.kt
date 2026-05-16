package com.easytier.ui.pages

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.easytier.data.NetworkConfig
import com.easytier.data.NodeInfo
import com.easytier.service.EasyTierService
import com.easytier.ui.components.AppDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NavItem(
    val label: String,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem("首页", Icons.Rounded.Home),
    NavItem("网络", Icons.Rounded.Wifi),
    NavItem("服务器", Icons.Rounded.Dns),
    NavItem("我的", Icons.Rounded.Person),
)

@Composable
fun HomePage() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showOneClickPage by rememberSaveable { mutableStateOf(false) }
    var showLogPage by remember { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background
    val isDashboard = !showOneClickPage && !showLogPage && selectedIndex == 0

    BackHandler(enabled = showOneClickPage || showLogPage) {
        when {
            showLogPage -> showLogPage = false
            showOneClickPage -> showOneClickPage = false
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
                        val selected = !showOneClickPage && selectedIndex == index
                        val tint = if (selected) Color(0xFF1F6FFF) else Color(0xFF98A2B3)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    showOneClickPage = false
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
                                    imageVector = item.icon,
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
                showOneClickPage -> OneClickPage()
                else -> stateHolder.SaveableStateProvider("tab_$selectedIndex") {
                    when (selectedIndex) {
                        0 -> DashboardScreen(
                            onOpenNetwork = { selectedIndex = 1 },
                            onOpenOneClick = { showOneClickPage = true },
                            onOpenServers = { selectedIndex = 2 },
                        )

                        1 -> NetworkConfigPage()
                        2 -> ServersPage()
                        3 -> SettingsPage(onNavigateToLog = { showLogPage = true })
                    }
                }
            }
        }
    }
}

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
    var isNodeSwitching by remember { mutableStateOf(false) }
    var pendingVpnConfig by remember { mutableStateOf<NetworkConfig?>(null) }

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

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val cfg = pendingVpnConfig ?: return@rememberLauncherForActivityResult
        pendingVpnConfig = null
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                startVpnForConfig(context, cfg)
                EasyTierService.refreshRuntimeState()
            }
        } else {
            Toast.makeText(context, "VPN 授权被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    fun startConfig(cfg: NetworkConfig) {
        if (cfg.networkName.isBlank()) {
            Toast.makeText(context, "请先到网络页配置网络", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            isNodeSwitching = true
            if (runtimeState.runningInstances.contains(cfg.instanceName)) {
                EasyTierService.stopNetwork(cfg.instanceName)
                EasyTierService.refreshRuntimeState()
                isNodeSwitching = false
                return@launch
            }

            val result = EasyTierService.startNetwork(cfg)
            if (!result.success) {
                Toast.makeText(context, "启动失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                isNodeSwitching = false
                return@launch
            }

            if (cfg.noTun) {
                EasyTierService.refreshRuntimeState()
                isNodeSwitching = false
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
            isNodeSwitching = false
        }
    }

    val localNode = nodes.firstOrNull { it.isLocal }
    val peerNodes = nodes.filterNot { it.isLocal }
    val totalDeviceCount = nodes.size
    val onlineCount = nodes.count { it.virtualIp.isNotBlank() }
    val peerCount = peerNodes.size
    val avgLatency = peerNodes.map { it.latencyMs }.filter { it > 0 }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val topLatency = peerNodes.map { it.latencyMs }.filter { it > 0 }.minOrNull() ?: 0
    val totalRx = nodes.sumOf { it.rxBytes }
    val totalTx = nodes.sumOf { it.txBytes }
    val onlineRatio = if (totalDeviceCount <= 0) 0f else (onlineCount.toFloat() / totalDeviceCount.toFloat()).coerceIn(0f, 1f)

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
                    )
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 16.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "内网穿透",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "站内互联和设备在线率实时监控",
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1AFFFFFF)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { onlineRatio },
                                        modifier = Modifier.size(90.dp),
                                        strokeWidth = 8.dp,
                                        color = Color(0xFF67E8F9),
                                        trackColor = Color.White.copy(alpha = 0.26f),
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = onlineCount.toString(),
                                            color = Color.White,
                                            fontSize = 29.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "在线设备",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1.2f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1AFFFFFF)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = localNode?.hostname?.ifBlank { "本机" } ?: "本机",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "延迟",
                                    color = Color.White.copy(alpha = 0.86f),
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (avgLatency > 0) "${avgLatency}ms" else "--",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
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
                            fontSize = 30.sp,
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
                                onClick = onOpenNetwork,
                            )
                            MetricActionItem(
                                icon = Icons.Rounded.Speed,
                                title = "一键联机",
                                onClick = onOpenOneClick,
                            )
                            MetricActionItem(
                                icon = Icons.Rounded.Dns,
                                title = "节点管理",
                                onClick = onOpenServers,
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
                            modifier = Modifier.clickable { showAddNodeDialog = true },
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
                                enabled = !isNodeSwitching,
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
    enabled: Boolean = true,
) {
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
        Button(
            onClick = onStart,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF1F6FFF),
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = if (isRunning) "关闭" else "开启",
                fontSize = 12.sp,
            )
        }
    }
}
