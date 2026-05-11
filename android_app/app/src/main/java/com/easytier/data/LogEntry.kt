package com.easytier.data

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String,
    val source: String = ""
) {
    val formattedTime: String
        get() = run {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
            val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
            val s = cal.get(java.util.Calendar.SECOND).toString().padStart(2, '0')
            val ms = cal.get(java.util.Calendar.MILLISECOND).toString().padStart(3, '0')
            "$h:$m:$s.$ms"
        }

    val levelLabel: String get() = level.name

    fun toFormattedString(): String {
        val src = if (source.isNotEmpty()) " [$source]" else ""
        return "[$formattedTime] [$levelLabel]$src $message"
    }
}
