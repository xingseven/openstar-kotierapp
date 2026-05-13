package com.easytier

import android.app.Application
import com.easytier.service.EasyTierService
import com.easytier.service.LogService

class EasyTierApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LogService.info("EasyTier 应用启动", source = "App")
        EasyTierService.initialize(this)
    }
}
