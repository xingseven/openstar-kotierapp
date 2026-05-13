package com.easytier.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.ui.components.CompactTopBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.data.LogLevel
import com.easytier.service.LogService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPage(onBack: (() -> Unit)? = null) {
    var logs by remember { mutableStateOf(LogService.logs) }
    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // 定时刷新日志
    LaunchedEffect(Unit) {
        while (true) {
            logs = LogService.logs
            if (autoScroll && logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            CompactTopBar(
                title = "运行日志",
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            AppIcon(AppIcons.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            ) {
                    TextButton(onClick = { autoScroll = !autoScroll }) {
                        Text(if (autoScroll) "自动滚动: 开" else "自动滚动: 关")
                    }
                    IconButton(onClick = { LogService.clear() }) {
                        AppIcon(AppIcons.DeleteSweep, contentDescription = "清除")
                    }
            }
        }
    ) {
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(it),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(it),
                state = listState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                items(logs, key = { it.hashCode() }) { entry ->
                    val color = when (entry.level) {
                        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                        LogLevel.WARN -> androidx.compose.ui.graphics.Color(0xFFFFA726)
                        LogLevel.ERROR -> androidx.compose.ui.graphics.Color(0xFFEF5350)
                    }
                    Text(
                        text = entry.toFormattedString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = color,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
