package com.easytier.service

import com.easytier.data.LogEntry
import com.easytier.data.LogLevel

/** 内存日志服务，带级别过滤和上限 */
object LogService {
    private val _logs = mutableListOf<LogEntry>()
    private const val MAX_LOGS = 2000
    var minimumLevel: LogLevel = LogLevel.DEBUG

    val logs: List<LogEntry> get() = _logs.toList()

    fun debug(message: String, source: String = "") = addLog(LogLevel.DEBUG, message, source)
    fun info(message: String, source: String = "") = addLog(LogLevel.INFO, message, source)
    fun warn(message: String, source: String = "") = addLog(LogLevel.WARN, message, source)
    fun error(message: String, source: String = "") = addLog(LogLevel.ERROR, message, source)

    private fun addLog(level: LogLevel, message: String, source: String) {
        if (level.ordinal < minimumLevel.ordinal) return
        val entry = LogEntry(level = level, message = message, source = source)
        synchronized(_logs) {
            _logs.add(entry)
            if (_logs.size > MAX_LOGS) _logs.removeAt(0)
        }
    }

    fun clear() { synchronized(_logs) { _logs.clear() } }

    fun getPlainText(): String = synchronized(_logs) {
        _logs.joinToString("\n") { it.toFormattedString() }
    }

    fun getFilteredLogs(minLevel: LogLevel? = null, query: String? = null): List<LogEntry> =
        synchronized(_logs) {
            var result: List<LogEntry> = _logs
            if (minLevel != null) result = result.filter { it.level.ordinal >= minLevel.ordinal }
            if (!query.isNullOrEmpty()) {
                val q = query.lowercase()
                result = result.filter {
                    it.message.lowercase().contains(q) || it.source.lowercase().contains(q)
                }
            }
            result
        }
}
