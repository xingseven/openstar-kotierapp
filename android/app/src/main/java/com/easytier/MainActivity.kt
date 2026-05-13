package com.easytier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.easytier.service.SettingsRepository
import com.easytier.ui.pages.HomePage
import com.easytier.ui.pages.LocalSettingsRepository
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

            CompositionLocalProvider(
                LocalDensity provides scaledDensity,
                LocalSettingsRepository provides settingsRepo,
            ) {
                EasyTierTheme {
                    HomePage()
                }
            }
        }
    }
}
