package com.easytier.backend.protocol

import org.json.JSONObject

sealed class JsonRpcMessage {
    abstract val jsonrpc: String
    abstract val id: String?
}

data class JsonRpcRequest(
    override val id: String?,
    val method: String,
    val params: JSONObject?
) : JsonRpcMessage() {
    override val jsonrpc: String get() = "2.0"

    fun toJson(): JSONObject = JSONObject().apply {
        put("jsonrpc", jsonrpc)
        id?.let { put("id", it) }
        put("method", method)
        params?.let { put("params", it) }
    }
}

data class JsonRpcResponse(
    override val id: String?,
    val result: Any?
) : JsonRpcMessage() {
    override val jsonrpc: String get() = "2.0"

    fun toJson(): JSONObject = JSONObject().apply {
        put("jsonrpc", jsonrpc)
        id?.let { put("id", it) }
        result?.let { put("result", it) }
    }
}

data class JsonRpcError(
    override val id: String?,
    val code: Int,
    val message: String,
    val data: JSONObject? = null
) : JsonRpcMessage() {
    override val jsonrpc: String get() = "2.0"

    fun toJson(): JSONObject = JSONObject().apply {
        put("jsonrpc", jsonrpc)
        id?.let { put("id", it) }
        put("error", JSONObject().apply {
            put("code", code)
            put("message", message)
            data?.let { put("data", it) }
        })
    }
}

object JsonRpcParser {
    fun parse(jsonStr: String): JsonRpcMessage? {
        return try {
            val json = JSONObject(jsonStr)
            val id = json.optString("id", null)
            val jsonrpc = json.optString("jsonrpc", "")

            if (json.has("method")) {
                JsonRpcRequest(
                    id = id,
                    method = json.getString("method"),
                    params = json.optJSONObject("params")
                )
            } else if (json.has("error")) {
                val error = json.getJSONObject("error")
                JsonRpcError(
                    id = id,
                    code = error.getInt("code"),
                    message = error.getString("message"),
                    data = error.optJSONObject("data")
                )
            } else if (json.has("result")) {
                JsonRpcResponse(
                    id = id,
                    result = json.opt("result")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
