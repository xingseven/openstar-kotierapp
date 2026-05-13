package com.easytier.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.BackHandler
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
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showLogPage by remember { mutableStateOf(false) }
    BackHandler(enabled = showLogPage) { showLogPage = false }
    val stateHolder = rememberSaveableStateHolder()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val selected = selectedIndex == index
                            val navTint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { selectedIndex = index }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(if (selected) item.selectedIconRes else item.iconRes),
                                        contentDescription = item.label,
                                        tint = navTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(item.label, fontSize = 11.sp, color = navTint, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                Color.Transparent,
                            )
                        )
                    )
            )
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (showLogPage) {
                    LogPage(onBack = { showLogPage = false })
                } else {
                    stateHolder.SaveableStateProvider(selectedIndex) {
                        when (selectedIndex) {
                            0 -> NetworkConfigPage()
                            1 -> OneClickPage()
                            2 -> ServersPage()
                            3 -> SettingsPage(onNavigateToLog = { showLogPage = true })
                        }
                    }
                }
            }
        }
    }
}

