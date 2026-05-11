#ifndef SETTINGSREPOSITORY_HPP
#define SETTINGSREPOSITORY_HPP

#include <QObject>
#include <QSettings>
#include <QString>
#include <QJsonArray>
#include <QJsonDocument>

class SettingsRepository : public QObject {
    Q_OBJECT
public:
    explicit SettingsRepository(QObject* parent = nullptr);

    // 通用设置
    bool followSystemTheme() const;
    void setFollowSystemTheme(bool v);

    bool darkMode() const;
    void setDarkMode(bool v);

    bool startOnBoot() const;
    void setStartOnBoot(bool v);

    // 网络设置
    bool autoReconnect() const;
    void setAutoReconnect(bool v);

    // 通知设置
    bool notifyOnConnect() const;
    void setNotifyOnConnect(bool v);

    bool notifyOnDisconnect() const;
    void setNotifyOnDisconnect(bool v);

    // 日志设置
    QString logLevel() const;
    void setLogLevel(const QString& v);

    // 网络配置持久化（JSON 字符串）
    void saveNetworkConfigs(const QJsonArray& configs);
    QJsonArray loadNetworkConfigsJson();

    // 服务器收藏
    void saveFavoriteServers(const QJsonArray& servers);
    QJsonArray loadFavoriteServersJson();

    // 清除所有
    void clearAll();

signals:
    void settingsChanged();

private:
    QSettings m_settings;
};

#endif // SETTINGSREPOSITORY_HPP
