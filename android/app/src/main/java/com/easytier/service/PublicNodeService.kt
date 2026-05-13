package com.easytier.service

import com.easytier.data.PublicNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** 公共节点服务 —— 从 info.qtet.cn 获取社区节点列表和心跳数据 */
object PublicNodeService {
    private const val STATUS_URL = "https://info.qtet.cn/uptime/status/easytier"
    private const val HEARTBEAT_URL = "https://info.qtet.cn/uptime/api/status-page/heartbeat/easytier"

    private val relayGroupNames = listOf("社区公共节点", "社区公共节点[海外]")
    private val urlPattern = Regex("""^(\w+://[^\s（(]+)[（(](.+)[）)]$""")

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNodes(): List<PublicNode> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(STATUS_URL).build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext emptyList()

            val preloadMatch = Regex(
                """window\.preloadData\s*=\s*(\{[\s\S]*?\});\s*</script>"""
            ).find(html) ?: return@withContext emptyList()

            val jsStr = preloadMatch.groupValues[1]
            val jsonStr = jsLiteralToJson(jsStr)
            val data = JSONObject(jsonStr)
            val groups = data.optJSONArray("publicGroupList") ?: return@withContext emptyList()

            val nodes = mutableListOf<PublicNode>()

            for (i in 0 until groups.length()) {
                val group = groups.getJSONObject(i)
                val groupName = group.optString("name", "")
                if (groupName !in relayGroupNames) continue

                val monitors = group.optJSONArray("monitorList") ?: continue
                for (j in 0 until monitors.length()) {
                    val m = monitors.getJSONObject(j)
                    val id = m.optInt("id")
                    val rawName = m.optString("name", "")
                    val type = m.optString("type", "port")
                    val (serverUrl, desc) = parseNodeName(rawName)
                    if (serverUrl.isEmpty()) continue

                    nodes.add(PublicNode(
                        id = id,
                        name = rawName,
                        serverUrl = serverUrl,
                        description = desc,
                        type = type,
                        group = groupName
                    ))
                }
            }

            nodes
        } catch (e: Exception) {
            LogService.error("fetch nodes failed: $e", source = "PublicNode")
            emptyList()
        }
    }

    suspend fun attachHeartbeat(nodes: List<PublicNode>) = withContext(Dispatchers.IO) {
        if (nodes.isEmpty()) return@withContext
        try {
            val request = Request.Builder().url(HEARTBEAT_URL).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext
            val data = JSONObject(body)
            val heartbeatList = data.optJSONObject("heartbeatList") ?: return@withContext

            for (node in nodes) {
                val key = node.id.toString()
                val entries = heartbeatList.optJSONArray(key) ?: continue
                if (entries.length() > 0) {
                    val last = entries.getJSONObject(entries.length() - 1)
                    node.status = last.optInt("status", 0)
                    if (last.has("ping")) node.ping = last.optInt("ping")
                }
            }
        } catch (e: Exception) {
            LogService.warn("heartbeat fetch failed: $e", source = "PublicNode")
        }
    }

    private fun parseNodeName(name: String): Pair<String, String> {
        val trimmed = name.trim()
        // 先剥离前缀标签如 [我的]、[海外]，再提取 URL
        val clean = trimmed.replaceFirst(Regex("""^\[.+?]"""), "").trim()
        val match = urlPattern.find(clean)
        if (match != null) {
            var url = match.groupValues[1].trim()
            url = url.replaceFirst(Regex("""^\[.+?]"""), "")
            return Pair(url, match.groupValues[2].trim())
        }
        return Pair("", "")
    }

    /** 将 JavaScript 单引号对象字面量转换为合法 JSON */
    private fun jsLiteralToJson(input: String): String {
        val sb = StringBuilder()
        var inStr = false
        var i = 0

        while (i < input.length) {
            val c = input[i]
            when {
                c == '\\' && inStr && i + 1 < input.length -> {
                    sb.append(c); sb.append(input[i + 1]); i += 2
                }
                c == '\'' -> { sb.append('"'); inStr = !inStr; i++ }
                c == '"' && inStr -> { sb.append("\\\""); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }
}
