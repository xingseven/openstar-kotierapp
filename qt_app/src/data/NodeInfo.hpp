#ifndef NODEINFO_HPP
#define NODEINFO_HPP

#include <QString>

enum class ConnectionType {
    DIRECT,
    RELAY,
    SERVER,
    UNKNOWN
};

inline ConnectionType connectionTypeFromString(const QString& str) {
    if (str == "direct") return ConnectionType::DIRECT;
    if (str == "relay") return ConnectionType::RELAY;
    if (str == "server") return ConnectionType::SERVER;
    return ConnectionType::UNKNOWN;
}

struct NodeInfo {
    QString hostname;
    QString virtualIp;
    int latencyMs = 0;
    QString protocol;
    QString connectionType = "unknown";
    bool isLocal = false;
    QString natType;
    qint64 rxBytes = 0;
    qint64 txBytes = 0;
    double lossRate = 0.0;

    ConnectionType connTypeEnum() const {
        return connectionTypeFromString(connectionType);
    }

    QString latencyText() const {
        if (latencyMs <= 0) return "-";
        return QString("%1ms").arg(latencyMs);
    }

    QString trafficText() const {
        return QString::fromUtf8("↓%1 ↑%2")
            .arg(formatBytes(rxBytes), formatBytes(txBytes));
    }

    static QString formatBytes(qint64 bytes) {
        if (bytes < 1024)
            return QString("%1 B").arg(bytes);
        if (bytes < 1024 * 1024)
            return QString("%1 KB").arg(bytes / 1024.0, 0, 'f', 1);
        return QString("%1 MB").arg(bytes / (1024.0 * 1024.0), 0, 'f', 1);
    }
};

#endif // NODEINFO_HPP
