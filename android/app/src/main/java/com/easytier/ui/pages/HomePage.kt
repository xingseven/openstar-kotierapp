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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FileUpload
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
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
    NavItem("网络", R.drawable.ic_nav_network),
    NavItem("联机", R.drawable.ic_nav_online),
    NavItem("服务器", R.drawable.ic_nav_server),
    NavItem("设置", R.drawable.ic_nav_setup),
)

private data class DeviceIconOption(
    val type: String,
    val label: String,
    val iconRes: Int,
)

private val deviceIconOptions = listOf(
    DeviceIconOption(type = "phone", label = "手机", iconRes = R.drawable.phone),
    DeviceIconOption(type = "desktop", label = "台式机", iconRes = R.drawable.computer),
    DeviceIconOption(type = "laptop", label = "笔记本", iconRes = R.drawable.laptop),
    DeviceIconOption(type = "server", label = "服务器", iconRes = R.drawable.server),
    DeviceIconOption(type = "nas", label = "NAS", iconRes = R.drawable.nas),
)

private fun resolveDeviceIconRes(deviceType: String): Int {
    return deviceIconOptions.firstOrNull { it.type == deviceType }?.iconRes ?: R.drawable.computer
}

@Composable
fun HomePage() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showLogPage by remember { mutableStateOf(false) }
    val stateHolder = rememberSaveableStateHolder()
    val view = LocalView.current
    val colorScheme = MaterialTheme.colorScheme
    val background = colorScheme.background
    val topBarChromeColor = colorScheme.surfaceVariant
    val bottomBarColor = colorScheme.surface
    val isDashboard = !showLogPage && selectedIndex == 0

    BackHandler(enabled = showLogPage) {
        if (showLogPage) showLogPage = false
    }

    LaunchedEffect(selectedIndex, showLogPage, background, topBarChromeColor, view) {
        val activity = view.context as? Activity ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(activity.window, view)
        if (isDashboard) {
            activity.window.statusBarColor = Color(0xFF1F6FFF).toArgb()
            controller.isAppearanceLightStatusBars = false
        } else {
            activity.window.statusBarColor = topBarChromeColor.toArgb()
            val lightBars = topBarChromeColor.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = lightBars
        }
        activity.window.navigationBarColor = bottomBarColor.toArgb()
        controller.isAppearanceLightNavigationBars = bottomBarColor.luminance() > 0.5f
    }

    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = bottomBarColor,
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
                        val selected = selectedIndex == index
                        val tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
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
            color = background,
        ) {
            when {
                showLogPage -> LogPage(onBack = { showLogPage = false })
                else -> stateHolder.SaveableStateProvider("tab_$selectedIndex") {
                    when (selectedIndex) {
                        0 -> DashboardScreen(
                            onOpenOneClick = { selectedIndex = 1 },
                            onOpenServers = { selectedIndex = 2 },
                        )

                        1 -> Column(Modifier.statusBarsPadding()) { OneClickPage() }
                        2 -> Column(Modifier.statusBarsPadding()) { ServersPage() }
                        3 -> Column(Modifier.statusBarsPadding()) { SettingsPage(onNavigateToLog = { showLogPage = true }) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
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
    var editingConfigInstanceName by remember { mutableStateOf<String?>(null) }
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

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonStr = inputStream?.bufferedReader()?.use { it.readText() } ?: return@launch
                val importedConfig = com.easytier.data.NetworkConfigImport.fromText(jsonStr) ?: return@launch
                importedConfig.instanceName = NetworkConfig.generateInstanceName()
                val updatedConfigs = repo.loadNetworkConfigs().toMutableList()
                updatedConfigs.add(importedConfig)
                repo.saveNetworkConfigs(updatedConfigs)
                configs = updatedConfigs
                Toast.makeText(context, "配置导入成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startConfig(cfg: NetworkConfig) {
        if (cfg.networkName.isBlank()) {
            Toast.makeText(context, "该节点未填写“网络名称”。请点右侧“三点”→“修改配置”后填写。", Toast.LENGTH_SHORT).show()
            editingConfigInstanceName = cfg.instanceName
            showNetworkConfigDialog = true
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
    val avgLossRate = peerNodes.map { it.lossRate }.filter { it > 0.0 }.average().takeIf { !it.isNaN() } ?: 0.0
    val totalRx = nodes.sumOf { it.rxBytes }
    val totalTx = nodes.sumOf { it.txBytes }

    if (showAddNodeDialog) {
        var deviceName by remember { mutableStateOf("") }
        var networkName by remember { mutableStateOf("") }
        var networkSecret by remember { mutableStateOf("") }
        var selectedDeviceType by remember { mutableStateOf("desktop") }

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
                    deviceType = selectedDeviceType
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
            Text(
                "设备图标",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(deviceIconOptions) { option ->
                    val selected = selectedDeviceType == option.type
                    OutlinedButton(
                        onClick = { selectedDeviceType = option.type },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Image(
                                painter = painterResource(id = option.iconRes),
                                contentDescription = option.label,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(option.label, fontSize = 11.sp)
                        }
                    }
                }
            }
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

    // 网络配置弹窗（可编辑）
    if (showNetworkConfigDialog) {
        val cfg = editingConfigInstanceName
            ?.let { targetInstance -> configs.firstOrNull { it.instanceName == targetInstance } }
            ?: activeConfig
        if (cfg == null) {
            showNetworkConfigDialog = false
            editingConfigInstanceName = null
        } else {
            var showAdvanced by remember { mutableStateOf(false) }
            var secretVisible by remember { mutableStateOf(false) }
            var editLabel by remember { mutableStateOf(cfg.networkLabel) }
            var editHostname by remember { mutableStateOf(cfg.hostname) }
            var editName by remember { mutableStateOf(cfg.networkName) }
            var editSecret by remember { mutableStateOf(cfg.networkSecret) }
            var editDhcp by remember { mutableStateOf(cfg.dhcp) }
            var editIpv4 by remember { mutableStateOf(cfg.ipv4) }
            var editEncryption by remember { mutableStateOf(cfg.enableEncryption) }
            var editDisableP2p by remember { mutableStateOf(cfg.disableP2p) }
            var editLatencyFirst by remember { mutableStateOf(cfg.latencyFirst) }
            var editPrivateMode by remember { mutableStateOf(cfg.privateMode) }
            var editNoTun by remember { mutableStateOf(cfg.noTun) }
            var editDisableIpv6 by remember { mutableStateOf(cfg.disableIpv6) }
            var editDisableUdpHp by remember { mutableStateOf(cfg.disableUdpHolePunching) }
            var editDisableTcpHp by remember { mutableStateOf(cfg.disableTcpHolePunching) }

            AppDialog(
                title = "编辑配置",
                onDismissRequest = {
                    showNetworkConfigDialog = false
                    editingConfigInstanceName = null
                },
                confirmText = "保存",
                icon = Icons.Rounded.Wifi,
                onConfirm = {
                    val updatedConfigs = repo.loadNetworkConfigs().toMutableList()
                    val idx = updatedConfigs.indexOfFirst { it.instanceName == cfg.instanceName }
                    if (idx >= 0) {
                        updatedConfigs[idx] = updatedConfigs[idx].apply {
                            networkLabel = editLabel
                            hostname = editHostname
                            networkName = editName
                            networkSecret = editSecret
                            dhcp = editDhcp
                            ipv4 = editIpv4
                            enableEncryption = editEncryption
                            disableP2p = editDisableP2p
                            latencyFirst = editLatencyFirst
                            privateMode = editPrivateMode
                            noTun = editNoTun
                            disableIpv6 = editDisableIpv6
                            disableUdpHolePunching = editDisableUdpHp
                            disableTcpHolePunching = editDisableTcpHp
                        }
                        repo.saveNetworkConfigs(updatedConfigs)
                        configs = updatedConfigs
                    }
                    showNetworkConfigDialog = false
                    editingConfigInstanceName = null
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("配置标签") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editHostname,
                        onValueChange = { editHostname = it },
                        label = { Text("本机主机名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("网络名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editSecret,
                        onValueChange = { editSecret = it },
                        label = { Text("网络密钥") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { secretVisible = !secretVisible }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    if (secretVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    )

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("DHCP 自动分配", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Switch(checked = editDhcp, onCheckedChange = { editDhcp = it })
                    }
                    if (!editDhcp) {
                        OutlinedTextField(
                            value = editIpv4,
                            onValueChange = { editIpv4 = it },
                            label = { Text("静态 IPv4") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "收起详情" else "展开详情", fontSize = 13.sp)
                    }
                    if (showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SwitchRow("启用加密", editEncryption) { editEncryption = it }
                            SwitchRow("禁用 P2P", editDisableP2p) { editDisableP2p = it }
                            SwitchRow("低延迟优先", editLatencyFirst) { editLatencyFirst = it }
                            SwitchRow("私有模式", editPrivateMode) { editPrivateMode = it }
                            SwitchRow("无 TUN 模式", editNoTun) { editNoTun = it }
                            SwitchRow("禁用 IPv6", editDisableIpv6) { editDisableIpv6 = it }
                            SwitchRow("禁用 UDP 打洞", editDisableUdpHp) { editDisableUdpHp = it }
                            SwitchRow("禁用 TCP 打洞", editDisableTcpHp) { editDisableTcpHp = it }
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
                    placeholder = { Text("服务器地址", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
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
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) { Text("添加", fontSize = 12.sp) }
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
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (topLatency > 0) "最快 ${topLatency}ms  ·  丢包 ${"%.1f".format(avgLossRate)}%" else "等待网络数据...",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("连接状态", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                            color = MaterialTheme.colorScheme.onSurface,
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
                        Text(
                            if (currentDownloadRate > 0f || currentUploadRate > 0f) {
                                "实时 ↓ ${formatSpeed(currentDownloadRate)} / ↑ ${formatSpeed(currentUploadRate)}"
                            } else {
                                "实时速率等待中..."
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("延迟状态", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            MetricActionItem(
                                icon = Icons.Rounded.Wifi,
                                title = "网络配置",
                                onClick = {
                                    editingConfigInstanceName = activeConfig?.instanceName
                                    showNetworkConfigDialog = true
                                },
                            )
                            MetricActionItem(
                                icon = Icons.Rounded.Dns,
                                title = "节点管理",
                                onClick = { showServerManagerDialog = true },
                            )
                            MetricActionItem(
                                icon = Icons.Rounded.FileUpload,
                                title = "导入",
                                onClick = { importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "节点列表",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
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
                                onEdit = {
                                    editingConfigInstanceName = cfg.instanceName
                                    showNetworkConfigDialog = true
                                },
                                onDelete = {
                                    val updated = configs.toMutableList()
                                    updated.removeAt(index)
                                    repo.saveNetworkConfigs(updated)
                                    configs = updated
                                },
                                enabled = switchingInstance == null || switchingInstance != cfg.instanceName,
                            )
                            if (index < configs.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            }
                        }
                    }

                    if (isRunning) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
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
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
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
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                .background(
                    if (node.isLocal) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (node.hostname.isBlank()) "未命名节点" else node.hostname,
                color = MaterialTheme.colorScheme.onSurface,
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
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (node.isLocal) "本机" else "在线",
            color = MaterialTheme.colorScheme.primary,
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = resolveDeviceIconRes(config.deviceType)),
                contentDescription = "设备图标",
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.networkLabel.ifBlank { config.hostname.ifBlank { "未命名节点" } },
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "网络: ${config.networkName.ifBlank { "未设置" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)),
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
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
