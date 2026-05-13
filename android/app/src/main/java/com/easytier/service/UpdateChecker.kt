package com.easytier.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

class UpdateChecker(private val context: Context) {

    companion object {
        private const val GITHUB_API =
            "https://api.github.com/repos/xingseven/openstar-kotierapp/releases/latest"
        private const val TIMEOUT = 15_000
    }

    sealed class Result {
        data class Available(val info: UpdateInfo) : Result()
        data class Unavailable(val reason: String) : Result()
    }

    suspend fun check(): Result = withContext(Dispatchers.IO) {
        try {
            val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
            conn.apply {
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                setRequestProperty("Accept", "application/vnd.github+json")
            }

            val code = conn.responseCode
            if (code == 404) {
                return@withContext Result.Unavailable("暂无发布版本")
            }
            if (code != 200) {
                return@withContext Result.Unavailable("检查失败 (HTTP $code)")
            }

            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "") ?: ""
            if (tagName.isBlank()) {
                return@withContext Result.Unavailable("无法获取版本信息")
            }

            // 从 assets 找 APK
            val assets = json.optJSONArray("assets") ?: return@withContext Result.Unavailable("未找到 APK 下载")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.optString("browser_download_url", "")
                    break
                }
            }

            if (apkUrl.isNullOrBlank()) {
                return@withContext Result.Unavailable("未找到 APK 下载")
            }

            val currentVersion = com.easytier.BuildConfig.VERSION_NAME
            val latestVersion = tagName.removePrefix("v")

            if (compareVersions(latestVersion, currentVersion) <= 0) {
                return@withContext Result.Unavailable("已是最新版本 ($currentVersion)")
            }

            val releaseNotes = json.optString("body", "").take(500)

            Result.Available(
                UpdateInfo(
                    latestVersion = latestVersion,
                    downloadUrl = apkUrl,
                    releaseNotes = releaseNotes,
                )
            )
        } catch (e: Exception) {
            Result.Unavailable("网络错误: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    suspend fun downloadAndInstall(info: UpdateInfo, onProgress: (Int) -> Unit = {}): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.externalCacheDir, "updates")
                dir.mkdirs()
                val file = File(dir, "kotier-v${info.latestVersion}.apk")
                // 已下载过直接安装
                if (file.exists()) {
                    installApk(file)
                    return@withContext null
                }

                val conn = URL(info.downloadUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = TIMEOUT
                conn.readTimeout = TIMEOUT
                conn.instanceFollowRedirects = true

                val total = conn.contentLength
                val input = conn.inputStream
                val output = FileOutputStream(file)
                val buffer = ByteArray(8192)
                var downloaded = 0
                var lastReported = -1

                while (true) {
                    val bytes = input.read(buffer)
                    if (bytes == -1) break
                    output.write(buffer, 0, bytes)
                    downloaded += bytes
                    val pct = if (total > 0) (downloaded * 100 / total) else -1
                    if (pct != lastReported) {
                        onProgress(pct)
                        lastReported = pct
                    }
                }

                output.close()
                input.close()
                installApk(file)
                null
            } catch (e: Exception) {
                "下载失败: ${e.localizedMessage ?: "未知错误"}"
            }
        }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }
}
