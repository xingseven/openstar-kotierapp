#include "SettingsRepository.hpp"

SettingsRepository::SettingsRepository(QObject* parent)
    : QObject(parent)
    , m_settings("EasyTier", "EasyTierApp")
{
}

// ── 通用设置 ──

bool SettingsRepository::followSystemTheme() const {
    return m_settings.value("follow_system", true).toBool();
}
void SettingsRepository::setFollowSystemTheme(bool v) {
    m_settings.setValue("follow_system", v);
    emit settingsChanged();
}

bool SettingsRepository::darkMode() const {
    return m_settings.value("dark_mode", false).toBool();
}
void SettingsRepository::setDarkMode(bool v) {
    m_settings.setValue("dark_mode", v);
    emit settingsChanged();
}

bool SettingsRepository::startOnBoot() const {
    return m_settings.value("start_on_boot", false).toBool();
}
void SettingsRepository::setStartOnBoot(bool v) {
    m_settings.setValue("start_on_boot", v);
    emit settingsChanged();
}

// ── 网络设置 ──

bool SettingsRepository::autoReconnect() const {
    return m_settings.value("auto_reconnect", false).toBool();
}
void SettingsRepository::setAutoReconnect(bool v) {
    m_settings.setValue("auto_reconnect", v);
    emit settingsChanged();
}

// ── 通知设置 ──

bool SettingsRepository::notifyOnConnect() const {
    return m_settings.value("notify_on_connect", true).toBool();
}
void SettingsRepository::setNotifyOnConnect(bool v) {
    m_settings.setValue("notify_on_connect", v);
    emit settingsChanged();
}

bool SettingsRepository::notifyOnDisconnect() const {
    return m_settings.value("notify_on_disconnect", true).toBool();
}
void SettingsRepository::setNotifyOnDisconnect(bool v) {
    m_settings.setValue("notify_on_disconnect", v);
    emit settingsChanged();
}

// ── 日志设置 ──

QString SettingsRepository::logLevel() const {
    return m_settings.value("log_level", "info").toString();
}
void SettingsRepository::setLogLevel(const QString& v) {
    m_settings.setValue("log_level", v);
    emit settingsChanged();
}

// ── 网络配置 ──

void SettingsRepository::saveNetworkConfigs(const QJsonArray& configs) {
    QJsonDocument doc(configs);
    m_settings.setValue("network_configs", QString::fromUtf8(doc.toJson(QJsonDocument::Compact)));
}

QJsonArray SettingsRepository::loadNetworkConfigsJson() {
    QString json = m_settings.value("network_configs").toString();
    if (json.isEmpty()) return QJsonArray();
    QJsonDocument doc = QJsonDocument::fromJson(json.toUtf8());
    if (!doc.isArray()) return QJsonArray();
    return doc.array();
}

// ── 服务器收藏 ──

void SettingsRepository::saveFavoriteServers(const QJsonArray& servers) {
    QJsonDocument doc(servers);
    m_settings.setValue("favorite_servers", QString::fromUtf8(doc.toJson(QJsonDocument::Compact)));
}

QJsonArray SettingsRepository::loadFavoriteServersJson() {
    QString json = m_settings.value("favorite_servers").toString();
    if (json.isEmpty()) return QJsonArray();
    QJsonDocument doc = QJsonDocument::fromJson(json.toUtf8());
    if (!doc.isArray()) return QJsonArray();
    return doc.array();
}

// ── 清除 ──

void SettingsRepository::clearAll() {
    m_settings.clear();
    emit settingsChanged();
}
