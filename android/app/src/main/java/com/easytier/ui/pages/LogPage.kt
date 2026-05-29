package com.easytier.ui.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.ui.components.CompactTopBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.data.LogEntry
import com.easytier.data.LogLevel
import com.easytier.service.LogService
import kotlinx.coroutines.delay
import java.util.Calendar

private fun LogEntry.formattedDate(): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.YEAR)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.DAY_OF_MONTH)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPage(onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(LogService.logs) }
    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var selectedDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            logs = LogService.logs
            delay(1000)
        }
    }

    val allDates = remember(logs) {
        logs.map { it.formattedDate() }.distinct().sortedDescending()
    }

    val filteredLogs = remember(logs, selectedDate) {
        if (selectedDate == null) logs
        else logs.filter { it.formattedDate() == selectedDate }
    }

    // Group logs by date for separator display
    val groupedLogs = remember(filteredLogs) {
        val map = linkedMapOf<String, MutableList<LogEntry>>()
        filteredLogs.forEach { entry ->
            val date = entry.formattedDate()
            map.getOrPut(date) { mutableListOf() }.add(entry)
        }
        map.toList() // List of Pair<String, List<LogEntry>>
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    IconButton(onClick = {
                        val text = filteredLogs.joinToString("\n") { it.toFormattedString() }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("easytier_logs", text))
                    }) {
                        AppIcon(AppIcons.Copy, contentDescription = "复制日志")
                    }
                    TextButton(onClick = { autoScroll = !autoScroll }) {
                        Text(if (autoScroll) "自动滚动: 开" else "自动滚动: 关")
                    }
                    IconButton(onClick = { LogService.clear() }) {
                        AppIcon(AppIcons.DeleteSweep, contentDescription = "清除")
                    }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            // 日期筛选栏
            if (allDates.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // "全部" chip
                    Surface(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { selectedDate = null },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedDate == null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            "全部",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedDate == null) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    allDates.forEach { date ->
                        Surface(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { selectedDate = date },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedDate == date) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                date,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedDate == date) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    groupedLogs.forEach { (date, entries) ->
                        // 日期分隔
                        item(key = "date_$date") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    date,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        items(entries, key = { it.hashCode().toLong() + date.hashCode() }) { entry ->
                            val color = when (entry.level) {
                                LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                                LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                                LogLevel.WARN -> Color(0xFFFFA726)
                                LogLevel.ERROR -> Color(0xFFEF5350)
                            }
                            Text(
                                text = entry.toFormattedString(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = color,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
