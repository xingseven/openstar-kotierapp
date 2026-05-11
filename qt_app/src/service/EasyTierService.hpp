#ifndef EASYTIERSERVICE_HPP
#define EASYTIERSERVICE_HPP

#include <QObject>
#include <QTimer>
#include <QList>
#include <QString>
#include <functional>
#include "ffi/RustFfi.hpp"
#include "data/NetworkConfig.hpp"
#include "data/NodeInfo.hpp"

struct EasyTierResult {
    bool success = false;
    QString errorMessage;

    static EasyTierResult ok() { return {true, ""}; }
    static EasyTierResult fail(const QString& msg) { return {false, msg}; }
};

class EasyTierService : public QObject {
    Q_OBJECT
public:
    static EasyTierService* instance();

    bool initialize();
    bool isInitialized() const { return m_initialized; }

    // 配置解析
    bool parseConfig(const QString& tomlConfig);

    // 网络实例管理
    void startNetwork(const NetworkConfig& config, std::function<void(EasyTierResult)> callback);
    void stopNetwork(const QString& instanceName, std::function<void(EasyTierResult)> callback);
    void stopAllNetworks(std::function<void(bool)> callback);

    // 节点监控
    void collectNodeInfos(const QString& instanceName, std::function<void(QList<NodeInfo>)> callback);
    void startMonitoring(const QString& instanceName);
    void stopMonitoring();

    // VPN 管理
    void startVpnService(const QString& instanceName, const QString& ipv4,
                         int prefix = 24, const QStringList& routes = {});
    void stopVpnService();

signals:
    void nodesUpdated(const QList<NodeInfo>& nodes);
    void networkStatusChanged(bool running);
    void errorOccurred(const QString& message);

private:
    explicit EasyTierService(QObject* parent = nullptr);
    static QString intToIp(int addr);

    RustFfi m_ffi;
    bool m_initialized = false;
    QTimer* m_monitorTimer = nullptr;
    QString m_monitoredInstance;
};

#endif // EASYTIERSERVICE_HPP
