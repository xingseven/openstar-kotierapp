package com.easytier.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

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
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedIndex == index) item.selectedIcon else item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
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
