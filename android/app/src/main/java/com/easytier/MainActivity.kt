package com.easytier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.easytier.service.SettingsRepository
import com.easytier.ui.pages.HomePage
import com.easytier.ui.pages.LocalSettingsRepository
import com.easytier.ui.pages.LocalThemeChangeListener
import com.easytier.ui.pages.ThemeChangeListener
import com.easytier.ui.theme.EasyTierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepo = SettingsRepository(this)

        setContent {
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity) {
                Density(
                    density = baseDensity.density * 0.95f,
                    fontScale = baseDensity.fontScale * 0.95f,
                )
            }

            // 读取主题设置
            var followSystem by remember { mutableStateOf(settingsRepo.followSystemTheme) }
            var darkMode by remember { mutableStateOf(settingsRepo.darkMode) }

            // 计算当前是否应该使用深色主题
            val isDarkTheme = when {
                followSystem -> isSystemInDarkTheme()
                else -> darkMode
            }
            val view = LocalView.current

            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }

            // 提供主题变化监听器
            val themeListener = remember {
                ThemeChangeListener(
                    onThemeChanged = { newFollowSystem, newDarkMode ->
                        followSystem = newFollowSystem
                        darkMode = newDarkMode
                    }
                )
            }

            CompositionLocalProvider(
                LocalDensity provides scaledDensity,
                LocalSettingsRepository provides settingsRepo,
                LocalThemeChangeListener provides themeListener,
            ) {
                EasyTierTheme(darkTheme = isDarkTheme) {
                    HomePage()
                }
            }
        }
    }
}
