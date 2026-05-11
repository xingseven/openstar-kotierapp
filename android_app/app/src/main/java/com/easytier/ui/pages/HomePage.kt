package com.easytier.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val navItems = listOf(
    NavItem("网络", Icons.Outlined.Lan, Icons.Filled.Lan),
    NavItem("一键联机", Icons.Outlined.FlashOn, Icons.Filled.FlashOn),
    NavItem("服务器", Icons.Outlined.Dns, Icons.Filled.Dns),
    NavItem("设置", Icons.Outlined.Settings, Icons.Filled.Settings),
)

@Composable
fun HomePage() {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            // 自定义紧凑底部导航栏：Surface 处理颜色/阴影，Row 处理内容高度，
            // windowInsetsPadding 处理手势区/导航条安全区，避免与 Modifier.height 冲突
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavigationBarDefaults.containerColor,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        val selected = selectedIndex == index
                        val contentColor = if (selected)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (selected) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(48.dp, 24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                item.selectedIcon,
                                                contentDescription = item.label,
                                                tint = contentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(item.label, fontSize = 11.sp, color = contentColor, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedIndex) {
                0 -> NetworkConfigPage()
                1 -> OneClickPage()
                2 -> ServersPage()
                3 -> SettingsPage()
            }
        }
    }
}

