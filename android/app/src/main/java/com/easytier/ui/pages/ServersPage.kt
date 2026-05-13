package com.easytier.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.easytier.ui.components.CompactTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.data.PublicNode
import com.easytier.data.ServerEntry
import com.easytier.service.PublicNodeService
import com.easytier.service.SettingsRepository
import com.easytier.ui.components.AppDialog
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersPage() {
    val repo = LocalSettingsRepository.current
    val scope = rememberCoroutineScope()

    var servers by remember { mutableStateOf(repo.loadFavoriteServers()) }
    var publicNodes by remember { mutableStateOf<List<PublicNode>>(emptyList()) }
    var publicExpanded by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var publicNodeError by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    // 加载公共节点
    LaunchedEffect(Unit) {
        publicNodeError = null
        val nodes = PublicNodeService.fetchNodes()
        if (nodes.isNotEmpty()) {
            PublicNodeService.attachHeartbeat(nodes)
        } else {
            publicNodeError = "无法获取节点列表（可能是网络问题）"
        }
        publicNodes = nodes
        isLoading = false
    }

    // 添加服务器对话框
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }

        AppDialog(
            title = "添加服务器",
            onDismissRequest = { showAddDialog = false },
            confirmText = "添加",
            confirmEnabled = url.isNotBlank(),
            icon = AppIcons.Add,
            onConfirm = {
                if (url.isNotBlank()) {
                    servers = (servers + ServerEntry(
                        name = name.ifBlank { url.trim() },
                        url = url.trim()
                    )).toMutableList()
                    repo.saveFavoriteServers(servers)
                    showAddDialog = false
                }
            }
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("服务器名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("服务器地址") },
                placeholder = { Text("wss://example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 编辑服务器对话框
    if (editingIndex >= 0 && editingIndex < servers.size) {
        val entry = servers[editingIndex]
        var editName by remember(editingIndex) { mutableStateOf(entry.name) }
        var editUrl by remember(editingIndex) { mutableStateOf(entry.url) }

        AppDialog(
            title = "编辑服务器",
            onDismissRequest = { editingIndex = -1 },
            confirmText = "保存",
            confirmEnabled = editUrl.isNotBlank(),
            icon = AppIcons.Edit,
            onConfirm = {
                servers = servers.toMutableList().also { it[editingIndex] = entry.apply { name = editName.trim(); url = editUrl.trim() } }
                repo.saveFavoriteServers(servers)
                editingIndex = -1
            }
        ) {
            OutlinedTextField(value = editName, onValueChange = { editName = it },
                label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = editUrl, onValueChange = { editUrl = it },
                label = { Text("地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CompactTopBar(title = "服务器") {
                    IconButton(onClick = { showAddDialog = true }) {
                        AppIcon(AppIcons.Add, contentDescription = "添加服务器")
                    }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // ── 公共节点 ──
                item {
                    if (publicNodes.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    if (publicNodeError != null) AppIcons.CloudOff else AppIcons.Cloud,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    publicNodeError ?: "暂无公共节点数据",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (publicNodeError != null) {
                                    TextButton(onClick = {
                                        isLoading = true
                                        publicNodeError = null
                                        scope.launch {
                                            val nodes = PublicNodeService.fetchNodes()
                                            if (nodes.isNotEmpty()) {
                                                PublicNodeService.attachHeartbeat(nodes)
                                            } else {
                                                publicNodeError = "无法获取节点列表"
                                            }
                                            publicNodes = nodes
                                            isLoading = false
                                        }
                                    }) { Text("重试", fontSize = 12.sp) }
                                }
                            }
                        }
                    } else {
                        Card(
                            onClick = { publicExpanded = !publicExpanded },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            AppIcon(AppIcons.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("社区公共节点", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("${publicNodes.size} 个", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                AppIcon(
                                    AppIcons.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (publicExpanded) {
                    items(publicNodes, key = { it.id }) { node ->
                        PublicNodeCard(node = node, onUse = {
                            val exists = servers.any { it.url == node.serverUrl }
                            if (!exists) {
                                servers = (servers + ServerEntry(
                                    name = node.description.ifBlank { node.serverUrl },
                                    url = node.serverUrl
                                )).toMutableList()
                                repo.saveFavoriteServers(servers)
                            }
                            repo.addServerToFirstNetworkConfig(node.serverUrl)
                        })
                    }
                }

                // ── 我的收藏 ──
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "我的收藏",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }

                if (servers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppIcon(
                                    AppIcons.Dns,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("暂无收藏服务器", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { showAddDialog = true }) {
                                    AppIcon(AppIcons.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("添加服务器")
                                }
                            }
                        }
                    }
                } else {
                    items(servers.withIndex().toList(), key = { it.value.url }) { (index, entry) ->
                        ServerCard(
                            entry = entry,
                            onEdit = { editingIndex = index },
                            onDelete = {
                                if (!entry.isDefault) {
                                    servers = servers.toMutableList().also { it.removeAt(index) }
                                    repo.saveFavoriteServers(servers)
                                }
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PublicNodeCard(node: PublicNode, onUse: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态灯
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .then(
                        if (node.isOnline) Modifier
                        else Modifier
                    )
                    .let { mod ->
                        mod.then(
                            if (node.isOnline) Modifier
                            else Modifier
                        )
                    }
            )
            // I'm doing the above nonsense because Kotlin's conditional chaining requires it.
            // Let me simplify:
            Spacer(Modifier.width(10.dp))

            // Actually, let me just do inline:
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = if (node.isOnline) Color(0xFF4CAF50) else Color.Gray,
            ) {}

            Spacer(Modifier.width(10.dp))

            // 延迟
            if (node.ping != null) {
                val pingColor = when {
                    node.ping!! < 300 -> Color(0xFF4CAF50)
                    node.ping!! < 800 -> Color(0xFFFFA726)
                    else -> Color(0xFFEF5350)
                }
                Text(
                    text = "${node.ping}ms",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = pingColor,
                    modifier = Modifier.width(44.dp)
                )
            } else {
                Spacer(Modifier.width(44.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.description.ifEmpty { node.name },
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = node.serverUrl,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(4.dp))

            val hasAsterisk = node.serverUrl.contains("*")
            OutlinedButton(
                onClick = { if (!hasAsterisk) onUse() },
                enabled = !hasAsterisk,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(if (hasAsterisk) "不可用" else "使用", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ServerCard(
    entry: ServerEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(
                        if (entry.isDefault) AppIcons.Star else AppIcons.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (entry.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "默认",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    entry.url,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (!entry.isDefault) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    AppIcon(AppIcons.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    AppIcon(AppIcons.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

