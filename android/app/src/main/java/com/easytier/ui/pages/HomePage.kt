package com.easytier.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    BackHandler(enabled = showOneClickPage || showLogPage) {
        when {
            showLogPage -> showLogPage = false
            showOneClickPage -> showOneClickPage = false
        }
    }

    Scaffold(
        containerColor = Color(0xFFF2F4F8),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
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
    val devices = remember {
        mutableStateListOf(
            DashboardDevice("获证硬锁节点", "路由 2 / 在线 5", true),
            DashboardDevice("联机号中心", "路由 1 / 在线 2", false),
            DashboardDevice("家防护设备", "路由 3 / 在线 6", false),
            DashboardDevice("我方防锚", "路由 2 / 在线 4", true),
            DashboardDevice("稳定", "路由 4 / 在线 8", true),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 14.dp),
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
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "搜索",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "状态",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
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
                                        progress = { 0.32f },
                                        modifier = Modifier.size(90.dp),
                                        strokeWidth = 8.dp,
                                        color = Color(0xFF67E8F9),
                                        trackColor = Color.White.copy(alpha = 0.26f),
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "32",
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
                                    text = "总访问量",
                                    color = Color.White.copy(alpha = 0.86f),
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "518",
                                    color = Color.White,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "较昨日 +8.2%",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 11.sp,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "业务负载",
                                    color = Color.White.copy(alpha = 0.86f),
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "73",
                                    color = Color.White,
                                    fontSize = 27.sp,
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
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        Text("5 台设备在线", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "255 7/50",
                            color = Color(0xFF111827),
                            fontSize = 35.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            "网络评分 92.5 分",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("一键速度", color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "体验",
                                color = Color(0xFF1F6FFF),
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { onOpenOneClick() },
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("40/90 ms", color = Color(0xFF94A3B8), fontSize = 11.sp)
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
                                icon = Icons.Rounded.Settings,
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
                    .padding(horizontal = 12.dp),
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
                        Text(
                            text = "更多 >",
                            fontSize = 12.sp,
                            color = Color(0xFF98A2B3),
                            modifier = Modifier.clickable { onOpenServers() },
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    devices.forEachIndexed { index, item ->
                        DeviceSwitchRow(
                            name = item.name,
                            detail = item.detail,
                            enabled = item.enabled,
                            onEnabledChange = { devices[index] = item.copy(enabled = it) },
                        )
                        if (index < devices.lastIndex) {
                            HorizontalDivider(color = Color(0xFFEFF2F6))
                        }
                    }
                }
            }
        }
    }
}

private data class DashboardDevice(
    val name: String,
    val detail: String,
    val enabled: Boolean,
)

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
private fun DeviceSwitchRow(
    name: String,
    detail: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
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
            Text(text = name, color = Color(0xFF111827), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = detail, color = Color(0xFF98A2B3), fontSize = 11.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1F6FFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent,
            ),
        )
    }
}
