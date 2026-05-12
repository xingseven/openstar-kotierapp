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
import androidx.annotation.DrawableRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.R

data class NavItem(
    val label: String,
    @DrawableRes val iconRes: Int,
    @DrawableRes val selectedIconRes: Int = iconRes
)

private val navItems = listOf(
    NavItem("网络", R.drawable.ic_nav_network),
    NavItem("一键联机", R.drawable.ic_nav_online),
    NavItem("服务器", R.drawable.ic_nav_server),
    NavItem("设置", R.drawable.ic_nav_setup),
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
                                                painter = painterResource(item.selectedIconRes),
                                                contentDescription = item.label,
                                                tint = contentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        painter = painterResource(item.iconRes),
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

