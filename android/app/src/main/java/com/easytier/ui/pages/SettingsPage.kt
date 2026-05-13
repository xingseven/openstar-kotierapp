package com.easytier.ui.pages

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.BuildConfig
import com.easytier.service.SettingsRepository
import com.easytier.service.UpdateChecker
import com.easytier.service.UpdateInfo
import com.easytier.ui.components.AppDialog
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.ui.components.CompactTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(onNavigateToLog: (() -> Unit)? = null) {
    val repo = LocalSettingsRepository.current
    var followSystem by remember { mutableStateOf(repo.followSystemTheme) }
    var darkMode by remember { mutableStateOf(repo.darkMode) }
    var logLevel by remember { mutableStateOf(repo.logLevel) }
    var showClearDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }

    if (showClearDialog) {
        AppDialog(
            title = "清除所有数据",
            onDismissRequest = { showClearDialog = false },
            confirmText = "清除",
            destructive = true,
            onConfirm = {
                repo.clearAll()
                followSystem = true; darkMode = false; logLevel = "info"
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

    // 更新对话框
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AppDialog(
            title = "发现新版本 v${info.latestVersion}",
            onDismissRequest = { showUpdateDialog = false },
            confirmText = if (isDownloading) "下载中 ${downloadProgress}%" else "下载更新",
            confirmEnabled = !isDownloading,
            onConfirm = {
                isDownloading = true
                scope.launch {
                    val err = UpdateChecker(context).downloadAndInstall(info) { pct ->
                        downloadProgress = pct
                    }
                    isDownloading = false
                    showUpdateDialog = false
                    if (err != null) {
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                }
            },
        ) {
            Text(
                info.releaseNotes.ifEmpty { "暂无更新说明" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToLog?.invoke() }
                )
            }

            SectionHeader("关于")
            SettingsCard {
                InfoRow("版本", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoRow("运行平台", "Android")
                InfoRow("后端", "EasyTier JNI")
                HorizontalDivider()
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (isCheckingUpdate) "检查中..." else "检查更新", fontSize = 13.sp)
                        }
                    },
                    trailingContent = { AppIcon(AppIcons.ChevronRight, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isCheckingUpdate && !isDownloading) {
                            isCheckingUpdate = true
                            scope.launch {
                                val result = UpdateChecker(context).check()
                                isCheckingUpdate = false
                                when (result) {
                                    is UpdateChecker.Result.Available -> {
                                        updateInfo = result.info
                                        showUpdateDialog = true
                                    }
                                    is UpdateChecker.Result.Unavailable -> {
                                        Toast.makeText(context, result.reason, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                )
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
