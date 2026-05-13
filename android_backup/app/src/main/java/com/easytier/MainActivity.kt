package com.easytier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
            EasyTierTheme {
                CompositionLocalProvider(LocalSettingsRepository provides settingsRepo) {
                    HomePage()
                }
            }
        }
    }
}
