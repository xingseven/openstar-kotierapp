package com.easytier.data

import org.json.JSONArray
import org.json.JSONObject

data class ServerEntry(
    var name: String,
    var url: String,
    val isDefault: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("url", url)
        put("is_default", isDefault)
    }

    companion object {
        fun fromJson(json: JSONObject): ServerEntry = ServerEntry(
            name = json.optString("name", ""),
            url = json.optString("url", ""),
            isDefault = json.optBoolean("is_default", false)
        )
    }
}

data class PublicNode(
    val id: Int,
    val name: String,
    val serverUrl: String,
    val description: String,
    val type: String,
    val group: String,
    var ping: Int? = null,
    var status: Int = 0
) {
    val isOnline: Boolean get() = status == 1
}
