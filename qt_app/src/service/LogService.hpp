#ifndef LOGSERVICE_HPP
#define LOGSERVICE_HPP

#include <QObject>
#include <QList>
#include <QMutex>
#include <QString>
#include "data/LogEntry.hpp"

class LogService : public QObject {
    Q_OBJECT
public:
    static LogService* instance();

    LogLevel minimumLevel = LogLevel::DEBUG;

    QList<LogEntry> logs() const;
    void debug(const QString& message, const QString& source = "");
    void info(const QString& message, const QString& source = "");
    void warn(const QString& message, const QString& source = "");
    void error(const QString& message, const QString& source = "");
    void clear();
    QString getPlainText() const;

    QList<LogEntry> getFilteredLogs(LogLevel minLevel = LogLevel::DEBUG,
                                     const QString& query = "") const;

signals:
    void logAdded(const LogEntry& entry);

private:
    explicit LogService(QObject* parent = nullptr);
    void addLog(LogLevel level, const QString& message, const QString& source);

    mutable QMutex m_mutex;
    QList<LogEntry> m_logs;
    static constexpr int MAX_LOGS = 2000;
};

#endif // LOGSERVICE_HPP
