package com.easytier.service

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** 应用设置持久化 —— 封装 SharedPreferences */
class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("easytier_settings", Context.MODE_PRIVATE)

    // ── 通用设置 ──

    var followSystemTheme: Boolean
        get() = prefs.getBoolean("follow_system", true)
        set(v) = prefs.edit().putBoolean("follow_system", v).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(v) = prefs.edit().putBoolean("dark_mode", v).apply()

    var startOnBoot: Boolean
        get() = prefs.getBoolean("start_on_boot", false)
        set(v) = prefs.edit().putBoolean("start_on_boot", v).apply()

    // ── 网络设置 ──

    var autoReconnect: Boolean
        get() = prefs.getBoolean("auto_reconnect", false)
        set(v) = prefs.edit().putBoolean("auto_reconnect", v).apply()

    // ── 通知设置 ──

    var notifyOnConnect: Boolean
        get() = prefs.getBoolean("notify_on_connect", true)
        set(v) = prefs.edit().putBoolean("notify_on_connect", v).apply()

    var notifyOnDisconnect: Boolean
        get() = prefs.getBoolean("notify_on_disconnect", true)
        set(v) = prefs.edit().putBoolean("notify_on_disconnect", v).apply()

    // ── 日志设置 ──

    var logLevel: String
        get() = prefs.getString("log_level", "info") ?: "info"
        set(v) = prefs.edit().putString("log_level", v).apply()

    // ── 网络配置持久化 ──

    fun saveNetworkConfigs(jsonArray: JSONArray) {
        prefs.edit().putString("network_configs", jsonArray.toString()).apply()
    }

    fun loadNetworkConfigsJson(): JSONArray? {
        val json = prefs.getString("network_configs", null) ?: return null
        return try { JSONArray(json) } catch (_: Exception) { null }
    }

    // ── 服务器收藏 ──

    fun saveFavoriteServers(jsonArray: JSONArray) {
        prefs.edit().putString("favorite_servers", jsonArray.toString()).apply()
    }

    fun loadFavoriteServersJson(): JSONArray? {
        val json = prefs.getString("favorite_servers", null) ?: return null
        return try { JSONArray(json) } catch (_: Exception) { null }
    }

    // ── 清除 ──

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
