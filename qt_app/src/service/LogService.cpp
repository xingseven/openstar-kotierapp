#include "LogService.hpp"
#include <QMutexLocker>

LogService* LogService::instance() {
    static LogService s_instance;
    return &s_instance;
}

LogService::LogService(QObject* parent) : QObject(parent) {}

QList<LogEntry> LogService::logs() const {
    QMutexLocker lock(&m_mutex);
    return m_logs;
}

void LogService::addLog(LogLevel level, const QString& message, const QString& source) {
    if (static_cast<int>(level) < static_cast<int>(minimumLevel))
        return;

    LogEntry entry(level, message, source);

    {
        QMutexLocker lock(&m_mutex);
        m_logs.append(entry);
        if (m_logs.size() > MAX_LOGS)
            m_logs.removeFirst();
    }

    emit logAdded(entry);
}

void LogService::debug(const QString& message, const QString& source) {
    addLog(LogLevel::DEBUG, message, source);
}

void LogService::info(const QString& message, const QString& source) {
    addLog(LogLevel::INFO, message, source);
}

void LogService::warn(const QString& message, const QString& source) {
    addLog(LogLevel::WARN, message, source);
}

void LogService::error(const QString& message, const QString& source) {
    addLog(LogLevel::ERROR, message, source);
}

void LogService::clear() {
    QMutexLocker lock(&m_mutex);
    m_logs.clear();
}

QString LogService::getPlainText() const {
    QMutexLocker lock(&m_mutex);
    QStringList lines;
    for (const auto& entry : m_logs)
        lines.append(entry.toFormattedString());
    return lines.join("\n");
}

QList<LogEntry> LogService::getFilteredLogs(LogLevel minLevel, const QString& query) const {
    QMutexLocker lock(&m_mutex);
    QList<LogEntry> result = m_logs;

    // Filter by level
    result.erase(std::remove_if(result.begin(), result.end(),
        [minLevel](const LogEntry& e) {
            return static_cast<int>(e.level) < static_cast<int>(minLevel);
        }), result.end());

    // Filter by query
    if (!query.isEmpty()) {
        QString q = query.toLower();
        result.erase(std::remove_if(result.begin(), result.end(),
            [&q](const LogEntry& e) {
                return !e.message.toLower().contains(q) &&
                       !e.source.toLower().contains(q);
            }), result.end());
    }

    return result;
}
