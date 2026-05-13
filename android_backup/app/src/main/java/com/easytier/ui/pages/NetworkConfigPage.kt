package com.easytier.ui.pages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.backend.csvToMutableStringList
import com.easytier.backend.collectProxyCidrsFromJson
import com.easytier.backend.collectRunningInstanceNames
import com.easytier.data.NetworkConfig
import com.easytier.ui.components.AppDialog
import com.easytier.ui.components.CompactTopBar
import com.easytier.data.NodeInfo
import com.easytier.service.EasyTierService
import com.easytier.service.LogService
import com.easytier.service.SettingsRepository
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.ui.components.CustomSwitch
import com.easytier.ui.components.NodeInfoCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkConfigPage() {
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val repo = LocalSettingsRepository.current
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var configs by remember { mutableStateOf(repo.loadNetworkConfigs()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var nodes by remember { mutableStateOf<List<NodeInfo>>(emptyList()) }
    var showAdvanced by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    // 触发 Compose 重绘 —— 每次修改配置后调用
    fun forceRecompose() {
        configs = ArrayList(configs)
    }

    // 配置持久化
    fun saveConfigs() {
        repo.saveNetworkConfigs(configs)
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
                assignedIp.ifEmpty { config.ipv4.ifEmpty { "10.144.144.1" } }, 24, routes)
            isRunning = true
            config.isRunning = true
            isLoading = false
        } else {
            scope.launch {
                EasyTierService.stopNetwork(config.instanceName)
            }
            isLoading = false
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

    // 选择配置时绑定表单
    fun bindConfig(index: Int) {
        if (index < 0 || index >= configs.size) return
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
        defaultProtocolText = cfg.defaultProtocol
        encryptionAlgorithmText = cfg.encryptionAlgorithm
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
        cfg.ipv4 = ipv4Text
        cfg.proxyNetworks = csvToMutableStringList(proxyNetworksText)
        cfg.customRoutes = csvToMutableStringList(customRoutesText)
        cfg.exitNodes = csvToMutableStringList(exitNodesText)
        cfg.foreignNetworkWhitelist = csvToMutableStringList(whitelistText)
        cfg.listenAddresses = csvToMutableStringList(listenAddressesText)
        cfg.devName = devNameText.trim()
        cfg.mtu = mtuText.toIntOrNull()?.takeIf { it in 1..1380 } ?: 0
        cfg.defaultProtocol = defaultProtocolText.trim().lowercase()
        cfg.encryptionAlgorithm = encryptionAlgorithmText.trim().ifEmpty { "aes-gcm" }.lowercase()
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

    // 页面重建时检测是否有正在运行的实例
    LaunchedEffect(Unit) {
        val jsonStr = EasyTierService.collectNetworkInfoJson()
        val runningNames = jsonStr?.let { collectRunningInstanceNames(it) }.orEmpty()

        configs.forEach { cfg ->
            cfg.isRunning = runningNames.contains(cfg.instanceName)
        }
        if (selectedIndex in configs.indices) {
            isRunning = configs[selectedIndex].isRunning
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
        topBar = {
            CompactTopBar(title = "网络配置") {
                    IconButton(onClick = { addConfig() }) { AppIcon(AppIcons.Add, contentDescription = "新建配置") }
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
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            FilterChip(
                                selected = index == selectedIndex,
                                label = { Text(label) },
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
                        OutlinedTextField(value = labelText, onValueChange = { labelText = it }, label = { Text("配置标签") }, placeholder = { Text("例如: 家庭网络") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(5.dp))
                        OutlinedTextField(value = hostnameText, onValueChange = { hostnameText = it }, label = { Text("本机主机名") }, placeholder = { Text("例如: my-phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(5.dp))
                        OutlinedTextField(value = networkNameText, onValueChange = { networkNameText = it }, label = { Text("网络名称") }, placeholder = { Text("例如: my-net") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(5.dp))
                        OutlinedTextField(value = networkSecretText, onValueChange = { networkSecretText = it }, label = { Text("网络密钥") }, placeholder = { Text("留空自动生成") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(5.dp))

                        val dhcpOn = configs.getOrNull(selectedIndex)?.dhcp ?: true
                        CustomSwitch(label = "DHCP 自动分配 IP", value = dhcpOn) { v ->
                            configs = configs.toMutableList().also { it[selectedIndex].dhcp = v }
                        }
                        if (!dhcpOn) {
                            Spacer(Modifier.height(5.dp))
                            OutlinedTextField(value = ipv4Text, onValueChange = { ipv4Text = it }, label = { Text("静态 IPv4") }, placeholder = { Text("例如: 10.144.144.10") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                                shape = RoundedCornerShape(999.dp),
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
                                    shape = RoundedCornerShape(16.dp),
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

                // 高级设置标题
                Surface(
                    onClick = { showAdvanced = !showAdvanced },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(AppIcons.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("高级设置", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        AppIcon(
                            AppIcons.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).let { mod ->
                            val rotation by animateFloatAsState(
                                targetValue = if (showAdvanced) 180f else 0f,
                                label = "expand"
                            )
                            mod.rotate(rotation)
                        }
                        )
                    }
                }

                // 高级设置内容
                AnimatedVisibility(visible = showAdvanced) {
                    if (selectedIndex in configs.indices) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                SectionLabel("协议与传输")
                                CustomSwitch("KCP 代理", "启用 KCP 代理入站", configs[selectedIndex].enableKcpProxy) { configs[selectedIndex].enableKcpProxy = it; forceRecompose() }
                                CustomSwitch("禁用 KCP 入站", configs[selectedIndex].disableKcpInput) { configs[selectedIndex].disableKcpInput = it; forceRecompose() }
                                CustomSwitch("QUIC 代理", "启用 QUIC 代理入站", configs[selectedIndex].enableQuicProxy) { configs[selectedIndex].enableQuicProxy = it; forceRecompose() }
                                CustomSwitch("禁用 QUIC 入站", configs[selectedIndex].disableQuicInput) { configs[selectedIndex].disableQuicInput = it; forceRecompose() }
                                CustomSwitch("禁用中继 KCP", configs[selectedIndex].disableRelayKcp) { configs[selectedIndex].disableRelayKcp = it; forceRecompose() }
                                CustomSwitch("禁用中继 QUIC", configs[selectedIndex].disableRelayQuic) { configs[selectedIndex].disableRelayQuic = it; forceRecompose() }
                                CustomSwitch("允许中继外部网络 KCP", configs[selectedIndex].enableRelayForeignNetworkKcp) { configs[selectedIndex].enableRelayForeignNetworkKcp = it; forceRecompose() }
                                CustomSwitch("允许中继外部网络 QUIC", configs[selectedIndex].enableRelayForeignNetworkQuic) { configs[selectedIndex].enableRelayForeignNetworkQuic = it; forceRecompose() }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                SectionLabel("网络与连接")
                                CustomSwitch("禁用 UDP 打孔", configs[selectedIndex].disableUdpHolePunching) { configs[selectedIndex].disableUdpHolePunching = it; forceRecompose() }
                                CustomSwitch("禁用 TCP 打孔", configs[selectedIndex].disableTcpHolePunching) { configs[selectedIndex].disableTcpHolePunching = it; forceRecompose() }
                                CustomSwitch("禁用 UPnP/NAT-PMP", configs[selectedIndex].disableUpnp) { configs[selectedIndex].disableUpnp = it; forceRecompose() }
                                CustomSwitch("禁用对称 NAT 打孔", configs[selectedIndex].disableSymHolePunching) { configs[selectedIndex].disableSymHolePunching = it; forceRecompose() }
                                CustomSwitch("禁用 P2P", configs[selectedIndex].disableP2p) { configs[selectedIndex].disableP2p = it; forceRecompose() }
                                CustomSwitch("需要 P2P", configs[selectedIndex].needP2p) { configs[selectedIndex].needP2p = it; forceRecompose() }
                                CustomSwitch("懒 P2P", configs[selectedIndex].lazyP2p) { configs[selectedIndex].lazyP2p = it; forceRecompose() }
                                CustomSwitch("仅 P2P", configs[selectedIndex].p2pOnly) { configs[selectedIndex].p2pOnly = it; forceRecompose() }
                                CustomSwitch("禁用 IPv6", configs[selectedIndex].disableIpv6) { configs[selectedIndex].disableIpv6 = it; forceRecompose() }
                                CustomSwitch("延迟优先", "优先选择延迟最低的路径", configs[selectedIndex].latencyFirst) { configs[selectedIndex].latencyFirst = it; forceRecompose() }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                SectionLabel("高级选项")
                                CustomSwitch("加密", "启用网络加密", configs[selectedIndex].enableEncryption) { configs[selectedIndex].enableEncryption = it; forceRecompose() }
                                CustomSwitch("出口节点", "将本机作为网络出口", configs[selectedIndex].enableExitNode) { configs[selectedIndex].enableExitNode = it; forceRecompose() }
                                CustomSwitch("系统转发", "启用系统 IP 转发", configs[selectedIndex].systemForwarding) { configs[selectedIndex].systemForwarding = it; forceRecompose() }
                                CustomSwitch("多线程", configs[selectedIndex].multiThread) { configs[selectedIndex].multiThread = it; forceRecompose() }
                                CustomSwitch("Smoltcp 协议栈", configs[selectedIndex].useSmoltcp) { configs[selectedIndex].useSmoltcp = it; forceRecompose() }
                                CustomSwitch("绑定设备", configs[selectedIndex].bindDevice) { configs[selectedIndex].bindDevice = it; forceRecompose() }
                                CustomSwitch("私有模式", "仅允许白名单节点加入", configs[selectedIndex].privateMode) { configs[selectedIndex].privateMode = it; forceRecompose() }
                                CustomSwitch("中转所有 RPC", configs[selectedIndex].relayAllPeerRpc) { configs[selectedIndex].relayAllPeerRpc = it; forceRecompose() }
                                CustomSwitch("接受 DNS", configs[selectedIndex].acceptDns) { configs[selectedIndex].acceptDns = it; forceRecompose() }
                                CustomSwitch("转发 UDP 广播", "改善依赖局域网发现的软件兼容性", configs[selectedIndex].enableUdpBroadcastRelay) { configs[selectedIndex].enableUdpBroadcastRelay = it; forceRecompose() }
                                CustomSwitch("禁用 TUN", configs[selectedIndex].noTun) { configs[selectedIndex].noTun = it; forceRecompose() }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                SectionLabel("设备与协议")
                                OutlinedTextField(value = devNameText, onValueChange = { devNameText = it; saveCurrentConfig() }, label = { Text("TUN 设备名") }, placeholder = { Text("留空使用默认") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(value = mtuText, onValueChange = { mtuText = it; saveCurrentConfig() }, label = { Text("MTU") }, placeholder = { Text("1-1380，留空不指定") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(value = defaultProtocolText, onValueChange = { defaultProtocolText = it; saveCurrentConfig() }, label = { Text("默认协议") }, placeholder = { Text("udp, tcp, wg, ws, wss") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(value = encryptionAlgorithmText, onValueChange = { encryptionAlgorithmText = it; saveCurrentConfig() }, label = { Text("加密算法") }, placeholder = { Text("aes-gcm, xor, chacha20 ...") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                SectionLabel("路由与白名单")
                                OutlinedTextField(value = listenAddressesText, onValueChange = { listenAddressesText = it; saveCurrentConfig() }, label = { Text("监听地址（逗号分隔）") }, placeholder = { Text("tcp://0.0.0.0:11010, udp://0.0.0.0:11010") }, maxLines = 2, modifier = Modifier.fillMaxWidth())

                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(value = proxyNetworksText, onValueChange = { proxyNetworksText = it; saveCurrentConfig() }, label = { Text("代理网络 CIDR（逗号分隔）") }, placeholder = { Text("192.168.1.0/24") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(value = customRoutesText, onValueChange = { customRoutesText = it; saveCurrentConfig() }, label = { Text("自定义路由（逗号分隔）") }, placeholder = { Text("192.168.1.0/24") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                Spacer(Modifier.height(5.dp))
                                OutlinedTextField(value = exitNodesText, onValueChange = { exitNodesText = it; saveCurrentConfig() }, label = { Text("出口节点（逗号分隔）") }, placeholder = { Text("peer-id") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                Spacer(Modifier.height(5.dp))
                                CustomSwitch("启用外部网络白名单", configs[selectedIndex].foreignNetworkWhitelistEnabled) { configs[selectedIndex].foreignNetworkWhitelistEnabled = it; forceRecompose() }
                                if (configs[selectedIndex].foreignNetworkWhitelistEnabled) {
                                    Spacer(Modifier.height(5.dp))
                                    OutlinedTextField(value = whitelistText, onValueChange = { whitelistText = it; saveCurrentConfig() }, label = { Text("外部网络白名单（逗号分隔）") }, placeholder = { Text("network1, network2") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 操作按钮
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (selectedIndex !in configs.indices) return@Button
                            saveCurrentConfig()
                            val cfg = configs[selectedIndex]
                            scope.launch {
                                isLoading = true
                                if (isRunning) {
                                    // ── 停止网络 ──
                                    EasyTierService.stopVpnService(context)
                                    val result = EasyTierService.stopNetwork(cfg.instanceName)
                                    if (result.success) {
                                        isRunning = false; cfg.isRunning = false; nodes = emptyList()
                                    }
                                    isLoading = false
                                } else {
                                    // ── 启动网络 ──
                                    val result = EasyTierService.startNetwork(cfg)
                                    if (!result.success) {
                                        isLoading = false; return@launch
                                    }
                                    LogService.info("网络已启动，等待 IP 分配", source = "NetworkConfig")

                                    // 等待 EasyTier 分配虚拟 IP 并获取路由
                                    var assignedIp = ""
                                    var routes = mutableListOf<String>()
                                    for (i in 0..30) {
                                        val nodes = EasyTierService.collectNodeInfos(cfg.instanceName)
                                        val localNode = nodes.find { it.isLocal }
                                        if (localNode != null && localNode.virtualIp.isNotEmpty()) {
                                            assignedIp = localNode.virtualIp
                                            // 从 backend 获取代理 CIDR 路由
                                            val jsonStr = EasyTierService.collectNetworkInfoJson()
                                            if (jsonStr != null) {
                                                routes = collectProxyCidrsFromJson(jsonStr, cfg.instanceName).toMutableList()
                                            }
                                            break
                                        }
                                        delay(500)
                                    }
                                    LogService.info("分配的 IP: $assignedIp, 路由: $routes", source = "NetworkConfig")
                                    if (assignedIp.isEmpty()) {
                                        LogService.warn("IP 分配超时，使用默认 IP", source = "NetworkConfig")
                                        assignedIp = cfg.ipv4.ifEmpty { "10.144.144.1" }
                                    }

                                    // 请求 VPN 授权
                                    val intent = VpnService.prepare(context)
                                    if (intent != null) {
                                        startPendingConfig = cfg
                                        startPendingIp = assignedIp
                                        startPendingRoutes = routes
                                        vpnPermissionLauncher.launch(intent)
                                        return@launch
                                    }
                                    // 已授权，直接启动 VPN（带路由）
                                    EasyTierService.startVpnService(context, cfg.instanceName, assignedIp, 24, routes)
                                    isRunning = true; cfg.isRunning = true; isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = if (!isRunning) ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                 else ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                    ) {
                        AppIcon(if (!isRunning) AppIcons.Play else AppIcons.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isLoading) "处理中..." else if (!isRunning) "启动网络" else "停止网络")
                    }

                    Spacer(Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = { /* 一键联机可通过底部导航访问 */ },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        AppIcon(AppIcons.Flash, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("一键联机")
                    }
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

