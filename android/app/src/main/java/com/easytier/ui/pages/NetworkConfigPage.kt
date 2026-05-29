package com.easytier.ui.pages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import com.easytier.backend.csvToMutableStringList
import com.easytier.backend.collectProxyCidrsFromJson
import com.easytier.data.NetworkConfig
import com.easytier.data.NetworkConfigImport
import com.easytier.ui.components.AppDialog
import com.easytier.ui.components.CompactTopBar
import com.easytier.data.NodeInfo
import com.easytier.service.EasyTierService
import com.easytier.service.LogService
import com.easytier.service.SettingsRepository
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.ui.components.NodeInfoCard
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkConfigPage() {
    val context = LocalContext.current as Activity
    val lifecycleOwner = context as LifecycleOwner
    val scope = rememberCoroutineScope()
    val repo = LocalSettingsRepository.current
    val runtimeState by EasyTierService.runtimeState.collectAsState()
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var configs by remember { mutableStateOf(repo.loadNetworkConfigs()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var nodes by remember { mutableStateOf<List<NodeInfo>>(emptyList()) }
    var showAdvanced by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isStopping by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showVpnConflictDialog by remember { mutableStateOf(false) }
    var protocolDropdownExpanded by remember { mutableStateOf(false) }
    var encryptionDropdownExpanded by remember { mutableStateOf(false) }

    // 触发 Compose 重绘 —— 每次修改配置后调用
    fun forceRecompose() {
        configs = ArrayList(configs)
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun updateSelectedConfig(mutator: (NetworkConfig) -> Unit) {
        if (selectedIndex !in configs.indices) return
        val updated = configs.toMutableList()
        mutator(updated[selectedIndex])
        configs = updated
        repo.saveNetworkConfigs(updated)
    }

    // 配置持久化
    fun saveConfigs() {
        repo.saveNetworkConfigs(configs)
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

    // VPN 授权相关
    var startPendingConfig by remember { mutableStateOf<NetworkConfig?>(null) }
    var startPendingIp by remember { mutableStateOf("") }
    var startPendingRoutes by remember { mutableStateOf<List<String>>(emptyList()) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("NetworkConfig", "VPN permission result: ${result.resultCode}")
        val config = startPendingConfig ?: return@rememberLauncherForActivityResult
        val assignedIp = startPendingIp
        val routes = startPendingRoutes
        startPendingConfig = null; startPendingIp = ""; startPendingRoutes = emptyList()
        if (result.resultCode == Activity.RESULT_OK) {
            android.util.Log.i("NetworkConfig", "VPN OK, IP=$assignedIp, routes=$routes")
            EasyTierService.startVpnService(context, config.instanceName,
                assignedIp.ifEmpty { NetworkConfig.vpnIpv4Address(config.ipv4).ifEmpty { "10.144.144.1" } }, 24, routes)
            isRunning = true
            config.isRunning = true
            forceRecompose()
            isLoading = false
            isStopping = false
        } else {
            scope.launch {
                EasyTierService.stopNetwork(config.instanceName)
            }
            config.isRunning = false
            isRunning = false
            forceRecompose()
            showToast("VPN 授权被拒绝")
            isLoading = false
            isStopping = false
        }
    }

    lateinit var bindConfig: (Int) -> Unit
    lateinit var importConfigFromUri: (Uri) -> Unit

    // 文件导入相关
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importConfigFromUri(it) }
    }

    importConfigFromUri = importConfigFromUri@{ uri ->
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                showToast("无法读取文件")
                return@importConfigFromUri
            }
            val jsonStr = inputStream.bufferedReader().use { it.readText() }

            val importedConfig = NetworkConfigImport.fromText(jsonStr)

            importedConfig.instanceName = NetworkConfig.generateInstanceName()
            importedConfig.isRunning = false
            importedConfig.defaultProtocol = NetworkConfig.normalizeDefaultProtocol(importedConfig.defaultProtocol)
            importedConfig.encryptionAlgorithm = NetworkConfig.normalizeEncryptionAlgorithm(importedConfig.encryptionAlgorithm)

            configs = (configs + importedConfig).toMutableList()
            selectedIndex = configs.size - 1
            saveConfigs()
            bindConfig(selectedIndex)

            showToast("配置已成功导入")
            LogService.info("从文件导入配置: ${importedConfig.networkLabel.ifEmpty { importedConfig.instanceName }}", source = "NetworkConfig")
        } catch (e: Exception) {
            showToast("导入失败: ${e.message}")
            android.util.Log.e("NetworkConfig", "Import failed", e)
        }
    }

    // 表单字段状态
    var labelText by remember { mutableStateOf("") }
    var hostnameText by remember { mutableStateOf("") }
    var networkNameText by remember { mutableStateOf("") }
    var networkSecretText by remember { mutableStateOf("") }
    var ipv4Text by remember { mutableStateOf("") }
    var proxyNetworksText by remember { mutableStateOf("") }
    var customRoutesText by remember { mutableStateOf("") }
    var exitNodesText by remember { mutableStateOf("") }
    var whitelistText by remember { mutableStateOf("") }
    var listenAddressesText by remember { mutableStateOf("") }
    var devNameText by remember { mutableStateOf("") }
    var mtuText by remember { mutableStateOf("") }
    var defaultProtocolText by remember { mutableStateOf("") }
    var encryptionAlgorithmText by remember { mutableStateOf("aes-gcm") }
    var networkSecretVisible by remember { mutableStateOf(false) }
    var dhcpEnabled by remember { mutableStateOf(true) }
    var latencyFirstEnabled by remember { mutableStateOf(false) }
    var privateModeEnabled by remember { mutableStateOf(true) }
    var noTunEnabled by remember { mutableStateOf(false) }
    var p2pOnlyEnabled by remember { mutableStateOf(false) }
    var disableP2pEnabled by remember { mutableStateOf(false) }
    var disableUdpHolePunchingEnabled by remember { mutableStateOf(false) }
    var disableTcpHolePunchingEnabled by remember { mutableStateOf(false) }
    var disableUpnpEnabled by remember { mutableStateOf(false) }
    var systemForwardingEnabled by remember { mutableStateOf(false) }
    var disableIpv6Enabled by remember { mutableStateOf(false) }
    var enableEncryptionEnabled by remember { mutableStateOf(true) }
    var acceptDnsEnabled by remember { mutableStateOf(false) }
    var enableUdpBroadcastRelayEnabled by remember { mutableStateOf(true) }
    var foreignNetworkWhitelistEnabled by remember { mutableStateOf(false) }

    // 选择配置时绑定表单
    bindConfig = bindConfig@{ index ->
        if (index < 0 || index >= configs.size) return@bindConfig
        val cfg = configs[index]
        labelText = cfg.networkLabel
        hostnameText = cfg.hostname
        networkNameText = cfg.networkName
        networkSecretText = cfg.networkSecret
        ipv4Text = cfg.ipv4
        proxyNetworksText = cfg.proxyNetworks.joinToString(", ")
        customRoutesText = cfg.customRoutes.joinToString(", ")
        exitNodesText = cfg.exitNodes.joinToString(", ")
        whitelistText = cfg.foreignNetworkWhitelist.joinToString(", ")
        listenAddressesText = cfg.listenAddresses.joinToString(", ")
        devNameText = cfg.devName
        mtuText = if (cfg.mtu > 0) cfg.mtu.toString() else ""
        defaultProtocolText = NetworkConfig.normalizeDefaultProtocol(cfg.defaultProtocol)
        encryptionAlgorithmText = NetworkConfig.normalizeEncryptionAlgorithm(cfg.encryptionAlgorithm)
        networkSecretVisible = false
        dhcpEnabled = cfg.dhcp
        latencyFirstEnabled = cfg.latencyFirst
        privateModeEnabled = cfg.privateMode
        noTunEnabled = cfg.noTun
        p2pOnlyEnabled = cfg.p2pOnly
        disableP2pEnabled = cfg.disableP2p && !cfg.p2pOnly
        disableUdpHolePunchingEnabled = cfg.disableUdpHolePunching
        disableTcpHolePunchingEnabled = cfg.disableTcpHolePunching
        disableUpnpEnabled = cfg.disableUpnp
        systemForwardingEnabled = cfg.systemForwarding
        disableIpv6Enabled = cfg.disableIpv6
        enableEncryptionEnabled = cfg.enableEncryption
        acceptDnsEnabled = cfg.acceptDns
        enableUdpBroadcastRelayEnabled = cfg.enableUdpBroadcastRelay
        foreignNetworkWhitelistEnabled = cfg.foreignNetworkWhitelistEnabled
        isRunning = cfg.isRunning
        showAdvanced = false
    }

    fun saveCurrentConfig() {
        if (selectedIndex < 0 || selectedIndex >= configs.size) return
        val cfg = configs[selectedIndex]
        cfg.networkLabel = labelText
        cfg.hostname = hostnameText
        cfg.networkName = networkNameText
        cfg.networkSecret = networkSecretText
        cfg.dhcp = dhcpEnabled
        cfg.ipv4 = ipv4Text
        cfg.latencyFirst = latencyFirstEnabled
        cfg.privateMode = privateModeEnabled
        cfg.noTun = noTunEnabled
        cfg.p2pOnly = p2pOnlyEnabled
        cfg.disableP2p = disableP2pEnabled && !p2pOnlyEnabled
        cfg.disableUdpHolePunching = disableUdpHolePunchingEnabled
        cfg.disableTcpHolePunching = disableTcpHolePunchingEnabled
        cfg.disableUpnp = disableUpnpEnabled
        cfg.systemForwarding = systemForwardingEnabled
        cfg.disableIpv6 = disableIpv6Enabled
        cfg.enableEncryption = enableEncryptionEnabled
        cfg.acceptDns = acceptDnsEnabled
        cfg.enableUdpBroadcastRelay = enableUdpBroadcastRelayEnabled
        cfg.foreignNetworkWhitelistEnabled = foreignNetworkWhitelistEnabled
        cfg.proxyNetworks = csvToMutableStringList(proxyNetworksText)
        cfg.customRoutes = csvToMutableStringList(customRoutesText)
        cfg.exitNodes = csvToMutableStringList(exitNodesText)
        cfg.foreignNetworkWhitelist = csvToMutableStringList(whitelistText)
        cfg.listenAddresses = csvToMutableStringList(listenAddressesText)
        cfg.devName = devNameText.trim()
        cfg.mtu = mtuText.toIntOrNull()?.takeIf { it in 1..1380 } ?: 0
        cfg.defaultProtocol = NetworkConfig.normalizeDefaultProtocol(defaultProtocolText)
        cfg.encryptionAlgorithm = NetworkConfig.normalizeEncryptionAlgorithm(encryptionAlgorithmText)
        defaultProtocolText = cfg.defaultProtocol
        encryptionAlgorithmText = cfg.encryptionAlgorithm
        saveConfigs()
    }

    fun copyVirtualIp(ip: String) {
        if (ip.isBlank()) return
        clipboard.setPrimaryClip(ClipData.newPlainText("easytier_virtual_ip", ip))
        Toast.makeText(context, "虚拟 IP 已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    fun shareVirtualIp(node: NodeInfo) {
        if (node.virtualIp.isBlank()) return
        val shareText = buildString {
            appendLine("EasyTier 直连地址")
            appendLine("设备: ${node.hostname.ifEmpty { "本机" }}")
            appendLine("虚拟 IP: ${node.virtualIp}")
            append("说明: 可在需要手动直连的场景中使用该地址。")
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享直连地址"))
    }

    // 初始化绑定
    LaunchedEffect(selectedIndex) {
        if (configs.isNotEmpty()) bindConfig(selectedIndex)
    }

    // 页面重建时同步 backend / VPN 实际运行态
    LaunchedEffect(Unit) {
        EasyTierService.refreshRuntimeState()
    }

    LaunchedEffect(runtimeState, selectedIndex, configs.size, isLoading) {
        // isLoading 期间正在执行启动/停止操作，本地状态优先，不允许 runtimeState 覆盖
        if (isLoading) return@LaunchedEffect
        var changed = false
        configs.forEach { cfg ->
            val daemonRunning = runtimeState.runningInstances.contains(cfg.instanceName)
            if (cfg.isRunning != daemonRunning) {
                cfg.isRunning = daemonRunning
                changed = true
            }
        }
        if (changed) {
            forceRecompose()
        }
        isRunning = if (selectedIndex in configs.indices) {
            configs[selectedIndex].isRunning
        } else {
            false
        }
    }

    // 节点定时监控
    LaunchedEffect(isRunning, selectedIndex) {
        if (isRunning && selectedIndex >= 0 && selectedIndex < configs.size) {
            while (true) {
                val instanceName = configs[selectedIndex].instanceName
                nodes = EasyTierService.collectNodeInfos(instanceName)
                delay(3000)
            }
        }
    }

    // 添加配置
    fun addConfig() {
        saveCurrentConfig()
        val newCfg = NetworkConfig()
        configs = (configs + newCfg).toMutableList()
        selectedIndex = configs.size - 1
        saveConfigs()
    }

    fun deleteConfig() {
        if (selectedIndex < 0 || selectedIndex >= configs.size) return
        LogService.info("删除网络配置: ${configs[selectedIndex].instanceName}", source = "NetworkConfig")
        configs = configs.toMutableList().also { it.removeAt(selectedIndex) }
        if (configs.isEmpty()) {
            val newCfg = NetworkConfig()
            configs = mutableListOf(newCfg)
        }
        selectedIndex = 0.coerceAtMost(configs.size - 1)
        saveConfigs()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CompactTopBar(title = "网络配置") {
                    IconButton(onClick = { addConfig() }) { AppIcon(AppIcons.Add, contentDescription = "新建配置") }
                    IconButton(onClick = { importFileLauncher.launch(arrayOf("application/json", "text/plain", "application/toml", "text/x-toml", "*/*")) }) { AppIcon(AppIcons.Login, contentDescription = "导入配置") }
                    IconButton(onClick = { deleteConfig() }) { AppIcon(AppIcons.Delete, contentDescription = "删除配置") }
                    IconButton(onClick = {
                        saveCurrentConfig()
                        LogService.info("配置已保存 (${configs.size} 个)", source = "NetworkConfig")
                    }) { AppIcon(AppIcons.Save, contentDescription = "保存配置") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 配置标签页
            if (configs.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(configs.withIndex().toList()) { (index, cfg) ->
                        val label = cfg.networkLabel.ifEmpty { "配置 ${index + 1}" }
                        val isChipRunning = cfg.isRunning
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            FilterChip(
                                selected = index == selectedIndex,
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    if (isChipRunning) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                                                    CircleShape
                                                )
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(label)
                                    }
                                },
                                onClick = {
                                    saveCurrentConfig()
                                    selectedIndex = index
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 基本设置
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(AppIcons.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("基本设置", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                OutlinedTextField(
                                    value = labelText,
                                    onValueChange = { labelText = it; saveCurrentConfig() },
                                    label = { Text("配置标签") },
                                    placeholder = { Text("例如: 家庭网络") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = hostnameText,
                                    onValueChange = { hostnameText = it; saveCurrentConfig() },
                                    label = { Text("本机主机名") },
                                    placeholder = { Text("例如: my-phone") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = networkNameText,
                                    onValueChange = { networkNameText = it; saveCurrentConfig() },
                                    label = { Text("网络名称") },
                                    placeholder = { Text("例如: my-net") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = networkSecretText,
                                    onValueChange = { networkSecretText = it; saveCurrentConfig() },
                                    label = { Text("网络密钥") },
                                    placeholder = { Text("留空自动生成") },
                                    singleLine = true,
                                    visualTransformation = if (networkSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { networkSecretVisible = !networkSecretVisible }) {
                                            Icon(
                                                imageVector = if (networkSecretVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                contentDescription = if (networkSecretVisible) "隐藏网络密钥" else "显示网络密钥"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (!dhcpEnabled) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = ipv4Text,
                                        onValueChange = {
                                            ipv4Text = it
                                            saveCurrentConfig()
                                        },
                                        label = { Text("静态 IPv4") },
                                        placeholder = { Text("例如: 10.144.144.10") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        if (!dhcpEnabled) {
                            // 保留静态 IPv4 输入在卡片内
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(AppIcons.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("高级设置", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                                Text(if (showAdvanced) "收起" else "展开")
                            }
                        }
                        AnimatedVisibility(visible = showAdvanced) {
                            Column {
                                Text(
                                    "这些选项会直接影响实际组网行为，修改后立即保存到当前配置。",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                CustomSwitch(
                                    label = "DHCP 自动分配",
                                    hint = "关闭后需要填写静态 IPv4",
                                    value = dhcpEnabled
                                ) {
                                    dhcpEnabled = it
                                    saveCurrentConfig()
                                }
                                Spacer(Modifier.height(5.dp))
                                ExposedDropdownMenuBox(
                                     expanded = protocolDropdownExpanded,
                                    onExpandedChange = { protocolDropdownExpanded = !protocolDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = when (defaultProtocolText) {
                                            "" -> "自动"
                                            else -> defaultProtocolText
                                        },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("默认协议") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = protocolDropdownExpanded,
                                        onDismissRequest = { protocolDropdownExpanded = false }
                                    ) {
                                        listOf("", "udp", "tcp", "wg", "ws", "wss").forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(if (option.isEmpty()) "自动" else option) },
                                                onClick = {
                                                    defaultProtocolText = option
                                                    protocolDropdownExpanded = false
                                                    saveCurrentConfig()
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                ExposedDropdownMenuBox(
                                    expanded = encryptionDropdownExpanded,
                                    onExpandedChange = { encryptionDropdownExpanded = !encryptionDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = encryptionAlgorithmText,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("加密算法") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = encryptionDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = encryptionDropdownExpanded,
                                        onDismissRequest = { encryptionDropdownExpanded = false }
                                    ) {
                                        listOf(
                                            "aes-gcm",
                                            "xor",
                                            "chacha20",
                                            "aes-gcm-256",
                                            "openssl-aes128-gcm",
                                            "openssl-aes256-gcm",
                                            "openssl-chacha20"
                                        ).forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    encryptionAlgorithmText = option
                                                    encryptionDropdownExpanded = false
                                                    saveCurrentConfig()
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                SectionLabel("连接行为")
                                CustomSwitch("低延迟优先", latencyFirstEnabled) {
                                    latencyFirstEnabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch("私有模式", privateModeEnabled) {
                                    privateModeEnabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch(
                                    "仅 P2P",
                                    "只与已建立 P2P 的节点通信，不走中转",
                                    p2pOnlyEnabled
                                ) {
                                    p2pOnlyEnabled = it
                                    if (it) disableP2pEnabled = false
                                    saveCurrentConfig()
                                }
                                CustomSwitch("禁用 P2P", disableP2pEnabled) {
                                    disableP2pEnabled = it
                                    if (it) p2pOnlyEnabled = false
                                    saveCurrentConfig()
                                }
                                CustomSwitch("禁用 UDP 打洞", disableUdpHolePunchingEnabled) {
                                    disableUdpHolePunchingEnabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch("禁用 TCP 打洞", disableTcpHolePunchingEnabled) {
                                    disableTcpHolePunchingEnabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch("禁用 UPnP", disableUpnpEnabled) {
                                    disableUpnpEnabled = it
                                    saveCurrentConfig()
                                }
                                Spacer(Modifier.height(6.dp))
                                SectionLabel("安全与协议")
                                CustomSwitch("启用加密", enableEncryptionEnabled) {
                                    enableEncryptionEnabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch("启用魔法 DNS", acceptDnsEnabled) {
                                    acceptDnsEnabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch("禁用 IPv6", disableIpv6Enabled) {
                                    disableIpv6Enabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch("启用 UDP 广播中继", enableUdpBroadcastRelayEnabled) {
                                    enableUdpBroadcastRelayEnabled = it
                                    saveCurrentConfig()
                                }
                                Spacer(Modifier.height(6.dp))
                                SectionLabel("TUN 与转发")
                                CustomSwitch("无 TUN 模式", noTunEnabled) {
                                    noTunEnabled = it
                                    saveCurrentConfig()
                                }
                                CustomSwitch("系统转发", systemForwardingEnabled) {
                                    systemForwardingEnabled = it
                                    saveCurrentConfig()
                                }
                                Spacer(Modifier.height(6.dp))
                                SectionLabel("高级地址")
                                OutlinedTextField(
                                    value = listenAddressesText,
                                    onValueChange = {
                                        listenAddressesText = it
                                        saveCurrentConfig()
                                    },
                                    label = { Text("监听地址（逗号分隔）") },
                                    placeholder = { Text("tcp://0.0.0.0:11010, udp://0.0.0.0:11010") },
                                    singleLine = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(
                                    value = proxyNetworksText,
                                    onValueChange = {
                                        proxyNetworksText = it
                                        saveCurrentConfig()
                                    },
                                    label = { Text("子网代理 CIDR（逗号分隔）") },
                                    placeholder = { Text("例如: 192.168.1.0/24") },
                                    singleLine = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(
                                    value = customRoutesText,
                                    onValueChange = {
                                        customRoutesText = it
                                        saveCurrentConfig()
                                    },
                                    label = { Text("自定义路由（逗号分隔）") },
                                    placeholder = { Text("例如: 10.10.0.0/16") },
                                    singleLine = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(5.dp))
                                CustomSwitch("启用网络白名单", foreignNetworkWhitelistEnabled) {
                                    foreignNetworkWhitelistEnabled = it
                                    saveCurrentConfig()
                                }
                                if (foreignNetworkWhitelistEnabled) {
                                    Spacer(Modifier.height(5.dp))
                                    OutlinedTextField(
                                        value = whitelistText,
                                        onValueChange = {
                                            whitelistText = it
                                            saveCurrentConfig()
                                        },
                                        label = { Text("网络白名单（逗号分隔）") },
                                        placeholder = { Text("例如: office-net, test-net") },
                                        singleLine = false,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(
                                    value = devNameText,
                                    onValueChange = {
                                        devNameText = it
                                        saveCurrentConfig()
                                    },
                                    label = { Text("TUN 设备名") },
                                    placeholder = { Text("留空使用默认") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(
                                    value = mtuText,
                                    onValueChange = {
                                        mtuText = it
                                        saveCurrentConfig()
                                    },
                                    label = { Text("MTU (1-1380)") },
                                    placeholder = { Text("留空使用默认") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 入口服务器
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(AppIcons.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("入口服务器", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                saveCurrentConfig(); showServerDialog = true
                            }) { Text("管理") }
                        }
                        if (selectedIndex in configs.indices) {
                            configs[selectedIndex].servers.forEach { server ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(6.dp))
                                    Text(server, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // 服务器管理对话框
                if (showServerDialog && selectedIndex in configs.indices) {
                    val cfg = configs[selectedIndex]
                    var servers by remember { mutableStateOf(cfg.servers.toMutableList()) }
                    var newUrl by remember { mutableStateOf("") }

                    AppDialog(
                        title = "管理入口服务器",
                        onDismissRequest = { showServerDialog = false },
                        confirmText = "确定",
                        icon = AppIcons.Dns,
                        onConfirm = {
                            cfg.servers = servers; saveConfigs(); showServerDialog = false
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                placeholder = { Text("wss://example.com") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newUrl.isNotBlank()) { servers.add(newUrl.trim()); newUrl = "" }
                                },
                                shape = RoundedCornerShape(499.5.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text("添加")
                            }
                        }
                        if (servers.isEmpty()) {
                            Text("暂无服务器", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val curServers = servers.toList()
                            curServers.forEachIndexed { idx, server ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            server,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        IconButton(onClick = {
                                            servers = servers.toMutableList().also { it.removeAt(idx) }
                                        }) {
                                            AppIcon(AppIcons.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // VPN 占用冲突对话框
                if (showVpnConflictDialog && selectedIndex in configs.indices) {
                    val otherInstance = EasyTierService.getActiveVpnInstanceName()
                    AlertDialog(
                        onDismissRequest = { showVpnConflictDialog = false },
                        title = { Text("VPN 已被占用") },
                        text = {
                            Text("实例「${otherInstance ?: "未知"}」正在使用 VPN。\n\n是否释放该 VPN，让当前实例使用？\n释放后原实例将以无 TUN 模式继续运行。\n\n点击「释放 VPN」后请再次点击「启动网络」。")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showVpnConflictDialog = false
                                scope.launch {
                                    EasyTierService.stopVpnService(context, otherInstance)
                                    showToast("VPN 已释放，请再次点击启动")
                                }
                            }) { Text("释放 VPN") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showVpnConflictDialog = false }) { Text("取消") }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 操作按钮
                Button(
                    onClick = {
                        if (selectedIndex !in configs.indices) return@Button
                        val cfg = configs[selectedIndex]
                        scope.launch {
                            isLoading = true
                            isStopping = isRunning
                            saveCurrentConfig()
                            if (!isRunning) {
                                if (cfg.networkName.isBlank()) {
                                    isLoading = false
                                    isStopping = false
                                    showToast("请先填写网络名称")
                                    return@launch
                                }
                                if (!dhcpEnabled && cfg.ipv4.isBlank()) {
                                    dhcpEnabled = true
                                    cfg.dhcp = true
                                    saveConfigs()
                                    forceRecompose()
                                    LogService.warn("检测到旧配置缺少静态 IPv4，已自动回退为 DHCP", source = "NetworkConfig")
                                    showToast("检测到旧配置缺少静态 IPv4，已自动切回 DHCP")
                                }

                                if (!cfg.noTun && EasyTierService.isVpnInUseByOther(cfg.instanceName)) {
                                    isLoading = false
                                    isStopping = false
                                    showVpnConflictDialog = true
                                    return@launch
                                }
                            }
                            if (isRunning) {
                                val result = EasyTierService.stopNetwork(cfg.instanceName)
                                if (result.success) {
                                    EasyTierService.stopVpnService(context, cfg.instanceName)
                                    isRunning = false
                                    cfg.isRunning = false
                                    nodes = emptyList()
                                    forceRecompose()
                                }
                                isLoading = false
                                isStopping = false
                            } else {
                                val result = EasyTierService.startNetwork(cfg)
                                if (!result.success) {
                                    isLoading = false
                                    isStopping = false
                                    showToast("启动失败: ${result.errorMessage}")
                                    return@launch
                                }

                                isRunning = true
                                cfg.isRunning = true
                                forceRecompose()
                                LogService.info("网络已启动，等待 IP 分配", source = "NetworkConfig")

                                if (cfg.noTun) {
                                    isLoading = false
                                    isStopping = false
                                    showToast("网络已启动（禁用 TUN）")
                                    return@launch
                                }

                                var assignedIp = if (!cfg.dhcp && cfg.ipv4.isNotBlank()) {
                                    NetworkConfig.vpnIpv4Address(cfg.ipv4)
                                } else {
                                    ""
                                }
                                var routes = mutableListOf<String>()
                                val attempts = if (assignedIp.isEmpty()) 20 else 5
                                repeat(attempts) {
                                    val currentNodes = EasyTierService.collectNodeInfos(cfg.instanceName)
                                    val localNode = currentNodes.find { it.isLocal }
                                    if (localNode != null && localNode.virtualIp.isNotEmpty()) {
                                        assignedIp = localNode.virtualIp
                                    }
                                    val jsonStr = EasyTierService.collectNetworkInfoJson()
                                    if (jsonStr != null) {
                                        routes = collectProxyCidrsFromJson(jsonStr, cfg.instanceName).toMutableList()
                                    }
                                    if (assignedIp.isNotEmpty()) {
                                        return@repeat
                                    }
                                    delay(300)
                                }

                                LogService.info("分配的 IP: $assignedIp, 路由: $routes", source = "NetworkConfig")
                                if (assignedIp.isEmpty()) {
                                    val stopResult = EasyTierService.stopNetwork(cfg.instanceName)
                                    isLoading = false
                                    isStopping = false
                                    isRunning = false
                                    cfg.isRunning = false
                                    nodes = emptyList()
                                    forceRecompose()
                                    if (!stopResult.success) {
                                        LogService.warn("未拿到虚拟 IP，回滚实例失败: ${stopResult.errorMessage}", source = "NetworkConfig")
                                    }
                                    showToast("未拿到本机虚拟 IP，请检查 DHCP/静态 IP、网络名称和入口服务器后重试")
                                    return@launch
                                }

                                val intent = VpnService.prepare(context)
                                if (intent != null) {
                                    startPendingConfig = cfg
                                    startPendingIp = assignedIp
                                    startPendingRoutes = routes
                                    vpnPermissionLauncher.launch(intent)
                                    return@launch
                                }

                                EasyTierService.startVpnService(context, cfg.instanceName, assignedIp, 24, routes)
                                isLoading = false
                                isStopping = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = if (!isRunning) ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                             else ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    AppIcon(if (!isRunning) AppIcons.Play else AppIcons.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            isLoading && isStopping -> "停止中..."
                            isLoading -> "启动中..."
                            !isRunning -> "启动网络"
                            else -> "停止网络"
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 节点监控
                if (isRunning) {
                    val localNode = nodes.firstOrNull { it.isLocal }

                    DirectConnectCard(
                        node = localNode,
                        onCopyIp = {
                            localNode?.virtualIp?.takeIf { it.isNotBlank() }?.let(::copyVirtualIp)
                        },
                        onShareIp = {
                            localNode?.takeIf { it.virtualIp.isNotBlank() }?.let(::shareVirtualIp)
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(AppIcons.MonitorHeart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("节点监测", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("实时", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    if (nodes.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                                Text("等待节点数据...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        nodes.forEach { node -> NodeInfoCard(node = node) }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CustomSwitch(label: String, value: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChanged)
    }
}

@Composable
private fun CustomSwitch(label: String, hint: String, value: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            Text(hint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onChanged)
    }
}

@Composable
private fun DirectConnectCard(
    node: NodeInfo?,
    onCopyIp: () -> Unit,
    onShareIp: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.Lan, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("本机直连地址", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (!node?.virtualIp.isNullOrBlank()) {
                    TextButton(onClick = onCopyIp) {
                        AppIcon(AppIcons.Copy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("复制")
                    }
                    TextButton(onClick = onShareIp) {
                        AppIcon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("分享")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (node?.virtualIp.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "网络已启动，正在获取本机虚拟 IP...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val virtualIp = node?.virtualIp.orEmpty()
                val hostname = node?.hostname.orEmpty()

                Text(
                    "可复制当前虚拟 IP，在需要手动直连的场景中使用。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        text = virtualIp,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "设备名: ${hostname.ifEmpty { "本机" }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

