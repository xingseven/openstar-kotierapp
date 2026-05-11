#ifndef LOGENTRY_HPP
#define LOGENTRY_HPP

#include <QString>
#include <QDateTime>
#include <QStringList>

enum class LogLevel {
    DEBUG = 0,
    INFO = 1,
    WARN = 2,
    ERROR = 3
};

inline LogLevel logLevelFromString(const QString& str) {
    QString lower = str.toLower();
    if (lower == "debug") return LogLevel::DEBUG;
    if (lower == "info") return LogLevel::INFO;
    if (lower == "warn" || lower == "warning") return LogLevel::WARN;
    if (lower == "error") return LogLevel::ERROR;
    return LogLevel::INFO;
}

inline QString logLevelToString(LogLevel level) {
    switch (level) {
        case LogLevel::DEBUG: return "DEBUG";
        case LogLevel::INFO:  return "INFO";
        case LogLevel::WARN:  return "WARN";
        case LogLevel::ERROR: return "ERROR";
    }
    return "UNKNOWN";
}

struct LogEntry {
    qint64 timestamp = 0;
    LogLevel level = LogLevel::INFO;
    QString message;
    QString source;

    LogEntry() : timestamp(QDateTime::currentMSecsSinceEpoch()) {}
    LogEntry(LogLevel lvl, const QString& msg, const QString& src = "")
        : timestamp(QDateTime::currentMSecsSinceEpoch()), level(lvl), message(msg), source(src) {}

    QString formattedTime() const {
        return QDateTime::fromMSecsSinceEpoch(timestamp).toString("HH:mm:ss.zzz");
    }

    QString levelLabel() const {
        return logLevelToString(level);
    }

    QString toFormattedString() const {
        QString src = source.isEmpty() ? "" : QString(" [%1]").arg(source);
        return QString("[%1] [%2]%3 %4")
            .arg(formattedTime(), levelLabel(), src, message);
    }
};

#endif // LOGENTRY_HPP
