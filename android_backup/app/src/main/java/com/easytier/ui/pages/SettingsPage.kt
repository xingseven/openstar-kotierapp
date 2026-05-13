package com.easytier.ui.pages

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.BuildConfig
import com.easytier.service.SettingsRepository
import com.easytier.ui.components.AppDialog
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.ui.components.CompactTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage() {
    val repo = LocalSettingsRepository.current
    var followSystem by remember { mutableStateOf(repo.followSystemTheme) }
    var darkMode by remember { mutableStateOf(repo.darkMode) }
    var startOnBoot by remember { mutableStateOf(repo.startOnBoot) }
    var autoReconnect by remember { mutableStateOf(repo.autoReconnect) }
    var notifyOnConnect by remember { mutableStateOf(repo.notifyOnConnect) }
    var notifyOnDisconnect by remember { mutableStateOf(repo.notifyOnDisconnect) }
    var logLevel by remember { mutableStateOf(repo.logLevel) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AppDialog(
            title = "清除所有数据",
            onDismissRequest = { showClearDialog = false },
            confirmText = "清除",
            destructive = true,
            onConfirm = {
                repo.clearAll()
                followSystem = true; darkMode = false; startOnBoot = false
                autoReconnect = false; notifyOnConnect = true
                notifyOnDisconnect = true; logLevel = "info"
                showClearDialog = false
            }
        ) {
            Text(
                "这将清除所有配置、服务器收藏和设置。此操作不可撤销。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { CompactTopBar("设置") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader("通用")
            SettingsCard {
                SettingSwitch(label = "跟随系统", value = followSystem) { v ->
                    followSystem = v; repo.followSystemTheme = v
                    if (v) { darkMode = false; repo.darkMode = false }
                }
                if (!followSystem) {
                    HorizontalDivider()
                    SettingSwitch(label = "深色模式", value = darkMode) { v ->
                        darkMode = v; repo.darkMode = v
                    }
                }
                HorizontalDivider()
                SettingSwitch(label = "开机自启", hint = "系统启动时自动运行", value = startOnBoot) { v ->
                    startOnBoot = v; repo.startOnBoot = v
                }
            }

            SectionHeader("网络")
            SettingsCard {
                SettingSwitch(label = "自动回连", hint = "网络断开后自动重新连接", value = autoReconnect) { v ->
                    autoReconnect = v; repo.autoReconnect = v
                }
            }

            SectionHeader("通知")
            SettingsCard {
                SettingSwitch(label = "连接成功通知", value = notifyOnConnect) { v ->
                    notifyOnConnect = v; repo.notifyOnConnect = v
                }
                HorizontalDivider()
                SettingSwitch(label = "断开连接通知", value = notifyOnDisconnect) { v ->
                    notifyOnDisconnect = v; repo.notifyOnDisconnect = v
                }
            }

            SectionHeader("日志")
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(AppIcons.Terminal, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("日志级别", modifier = Modifier.weight(1f), fontSize = 13.sp)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) { Text(logLevel.uppercase()) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("debug", "info", "warn", "error").forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level.uppercase()) },
                                    onClick = { logLevel = level; repo.logLevel = level; expanded = false }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("查看运行日志", fontSize = 13.sp) },
                    trailingContent = { AppIcon(AppIcons.ChevronRight, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionHeader("关于")
            SettingsCard {
                InfoRow("版本", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow("运行平台", "Android")
                InfoRow("后端", "EasyTier JNI")
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    AppIcon(AppIcons.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("清除所有数据")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), content = content)
    }
}

@Composable
private fun SettingSwitch(label: String, value: Boolean, hint: String? = null, onChanged: (Boolean) -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp)
            if (hint != null) Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onChanged)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    ListItem(
        headlineContent = { Text(title, fontSize = 13.sp) },
        trailingContent = { Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

val LocalSettingsRepository = staticCompositionLocalOf<SettingsRepository> {
    error("SettingsRepository not provided")
}
