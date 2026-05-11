#include "EasyTierService.hpp"
#include "LogService.hpp"
#include <QDebug>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QtConcurrent>
#include <QThread>
#include <QDir>

#ifdef Q_OS_ANDROID
#include "utils/JniBridge.hpp"
#endif

EasyTierService* EasyTierService::instance() {
    static EasyTierService s_instance;
    return &s_instance;
}

EasyTierService::EasyTierService(QObject* parent)
    : QObject(parent)
{
    m_monitorTimer = new QTimer(this);
    m_monitorTimer->setInterval(3000);
    connect(m_monitorTimer, &QTimer::timeout, this, [this]() {
        if (m_monitoredInstance.isEmpty()) return;
        collectNodeInfos(m_monitoredInstance, [this](const QList<NodeInfo>& nodes) {
            emit nodesUpdated(nodes);
        });
    });
}

bool EasyTierService::initialize() {
    if (m_initialized) return true;

    // 加载 Rust FFI 库
    QString libPath;
#ifdef Q_OS_ANDROID
    libPath = "libeasytier_ffi.so";
#else
    // 桌面环境测试路径
    QStringList searchPaths = {
        QDir::currentPath() + "/libs/arm64-v8a/libeasytier_ffi.so",
        QDir::currentPath() + "/libeasytier_ffi.so",
    };
    for (const auto& path : searchPaths) {
        if (QFile::exists(path)) {
            libPath = path;
            break;
        }
    }
#endif

    if (libPath.isEmpty()) {
        LogService::instance()->error("EasyTierService: FFI library not found", "EasyTier");
        return false;
    }

    if (!m_ffi.load(libPath)) {
        LogService::instance()->error("EasyTierService: FFI init failed", "EasyTier");
        return false;
    }

    m_initialized = true;
    LogService::instance()->info("EasyTierService initialized", "EasyTier");
    return true;
}

bool EasyTierService::parseConfig(const QString& tomlConfig) {
    if (!m_initialized) return false;
    int result = m_ffi.parseConfig(tomlConfig);
    if (result != 0) {
        LogService::instance()->error("parseConfig failed: " + m_ffi.getLastError(), "EasyTier");
    }
    return result == 0;
}

void EasyTierService::startNetwork(const NetworkConfig& config,
                                    std::function<void(EasyTierResult)> callback) {
    QtConcurrent::run([this, config, callback]() {
        if (!m_initialized) {
            QMetaObject::invokeMethod(this, [callback]() {
                callback(EasyTierResult::fail("not initialized"));
            }, Qt::QueuedConnection);
            return;
        }

        QString toml = config.toToml();
        int result = m_ffi.runNetworkInstance(toml);
        EasyTierResult res;
        if (result == 0) {
            res = EasyTierResult::ok();
            QMetaObject::invokeMethod(this, [this]() {
                emit networkStatusChanged(true);
            }, Qt::QueuedConnection);
        } else {
            res = EasyTierResult::fail(m_ffi.getLastError());
        }

        QMetaObject::invokeMethod(this, [callback, res]() {
            callback(res);
        }, Qt::QueuedConnection);
    });
}

void EasyTierService::stopNetwork(const QString& instanceName,
                                   std::function<void(EasyTierResult)> callback) {
    QtConcurrent::run([this, instanceName, callback]() {
        if (!m_initialized) {
            QMetaObject::invokeMethod(this, [callback]() {
                callback(EasyTierResult::fail("not initialized"));
            }, Qt::QueuedConnection);
            return;
        }

        // 收集所有实例，保留非目标实例
        KeyValuePair infos[16] = {};
        int count = m_ffi.collectNetworkInfos(infos, 16);

        QStringList namesToRetain;
        for (int i = 0; i < count; ++i) {
            QString key = QString::fromUtf8(infos[i].key);
            if (key != instanceName)
                namesToRetain.append(key);
            m_ffi.freeString(infos[i].key);
            m_ffi.freeString(infos[i].value);
        }

        // 构建 C 字符串数组
        QList<QByteArray> nameBytes;
        for (const auto& n : namesToRetain)
            nameBytes.append(n.toUtf8());
        QList<const char*> cNames;
        for (const auto& b : nameBytes)
            cNames.append(b.constData());

        int result;
        if (cNames.isEmpty()) {
            result = m_ffi.retainNetworkInstance(nullptr, 0);
        } else {
            result = m_ffi.retainNetworkInstance(cNames.data(), cNames.size());
        }

        EasyTierResult res;
        if (result == 0) {
            res = EasyTierResult::ok();
        } else {
            res = EasyTierResult::fail(m_ffi.getLastError());
        }

        QMetaObject::invokeMethod(this, [callback, res]() {
            callback(res);
        }, Qt::QueuedConnection);
    });
}

void EasyTierService::stopAllNetworks(std::function<void(bool)> callback) {
    QtConcurrent::run([this, callback]() {
        if (!m_initialized) {
            QMetaObject::invokeMethod(this, [callback]() {
                callback(false);
            }, Qt::QueuedConnection);
            return;
        }

        int result = m_ffi.retainNetworkInstance(nullptr, 0);
        bool ok = (result == 0);

        QMetaObject::invokeMethod(this, [this, callback, ok]() {
            if (ok) emit networkStatusChanged(false);
            callback(ok);
        }, Qt::QueuedConnection);
    });
}

void EasyTierService::collectNodeInfos(const QString& instanceName,
                                        std::function<void(QList<NodeInfo>)> callback) {
    QtConcurrent::run([this, instanceName, callback]() {
        if (!m_initialized) {
            QMetaObject::invokeMethod(this, [callback]() {
                callback(QList<NodeInfo>());
            }, Qt::QueuedConnection);
            return;
        }

        KeyValuePair infos[16] = {};
        int count = m_ffi.collectNetworkInfos(infos, 16);
        QList<NodeInfo> nodes;

        if (count < 0) {
            QMetaObject::invokeMethod(this, [callback]() {
                callback(QList<NodeInfo>());
            }, Qt::QueuedConnection);
            return;
        }

        for (int i = 0; i < count; ++i) {
            QString jsonStr = QString::fromUtf8(infos[i].value);
            QString key = QString::fromUtf8(infos[i].key);
            m_ffi.freeString(infos[i].key);
            m_ffi.freeString(infos[i].value);

            QJsonDocument doc = QJsonDocument::fromJson(jsonStr.toUtf8());
            if (!doc.isObject()) continue;
            QJsonObject root = doc.object();
            QJsonObject map = root["map"].toObject();

            for (auto it = map.begin(); it != map.end(); ++it) {
                QJsonObject value = it.value().toObject();
                QJsonObject nodeObj = value["my_node_info"].toObject();
                bool isLocal = (it.key() == instanceName);

                // 解析 virtual_ipv4
                QJsonObject ipv4Obj = nodeObj["virtual_ipv4"].toObject();
                QJsonObject addrObj = ipv4Obj["address"].toObject();
                qint64 rawAddr = addrObj["addr"].toVariant().toLongLong();
                QString virtualIp = intToIp(static_cast<int>(rawAddr));

                QString hostname = nodeObj["hostname"].toString();

                NodeInfo localNode;
                localNode.hostname = hostname;
                localNode.virtualIp = virtualIp;
                localNode.isLocal = isLocal;
                nodes.append(localNode);

                // 解析 peers
                QJsonArray peers = value["peers"].toArray();
                QMap<qint64, QJsonObject> peerMap;
                for (const auto& p : peers) {
                    QJsonObject peer = p.toObject();
                    qint64 peerId = peer["peer_id"].toVariant().toLongLong();
                    peerMap[peerId] = peer;
                }

                // 解析 routes
                qint64 myPeerId = nodeObj["peer_id"].toVariant().toLongLong();
                QJsonArray routes = value["routes"].toArray();
                for (const auto& r : routes) {
                    QJsonObject route = r.toObject();
                    qint64 peerId = route["peer_id"].toVariant().toLongLong();
                    if (peerId == myPeerId || peerId == -1) continue;

                    QJsonObject routeIpv4 = route["ipv4_addr"].toObject();
                    QJsonObject routeAddr = routeIpv4["address"].toObject();
                    qint64 routeRawAddr = routeAddr["addr"].toVariant().toLongLong();
                    QString peerVirtualIp = intToIp(static_cast<int>(routeRawAddr));
                    QString peerHostname = route["hostname"].toString();
                    int latencyMs = route["path_latency"].toInt(0);

                    QJsonObject featureFlag = route["feature_flag"].toObject();
                    bool isPublicServer = featureFlag["is_public_server"].toBool(false);
                    bool isDirectlyConnected = peerMap.contains(peerId);

                    NodeInfo peerNode;
                    peerNode.hostname = peerHostname.isEmpty() ?
                        QString("peer-%1").arg(peerId) : peerHostname;
                    peerNode.virtualIp = peerVirtualIp;
                    peerNode.isLocal = false;
                    peerNode.latencyMs = latencyMs;

                    if (isPublicServer)
                        peerNode.connectionType = "server";
                    else if (isDirectlyConnected)
                        peerNode.connectionType = "direct";
                    else
                        peerNode.connectionType = "relay";

                    if (peerMap.contains(peerId)) {
                        QJsonObject peer = peerMap[peerId];
                        QJsonArray conns = peer["conns"].toArray();
                        if (!conns.isEmpty()) {
                            QJsonObject conn = conns[0].toObject();
                            QJsonObject tunnel = conn["tunnel"].toObject();
                            peerNode.protocol = tunnel["tunnel_type"].toString().toUpper();
                            peerNode.rxBytes = conn["stats"].toObject()["rx_bytes"].toVariant().toLongLong();
                            peerNode.txBytes = conn["stats"].toObject()["tx_bytes"].toVariant().toLongLong();
                            peerNode.lossRate = conn["loss_rate"].toDouble(0.0);
                        }
                    }

                    nodes.append(peerNode);
                }
            }
        }

        QMetaObject::invokeMethod(this, [callback, nodes]() {
            callback(nodes);
        }, Qt::QueuedConnection);
    });
}

void EasyTierService::startMonitoring(const QString& instanceName) {
    stopMonitoring();
    m_monitoredInstance = instanceName;
    m_monitorTimer->start();
    LogService::instance()->info(
        QString("Monitoring started: %1").arg(instanceName), "EasyTier");
}

void EasyTierService::stopMonitoring() {
    m_monitorTimer->stop();
    m_monitoredInstance.clear();
}

void EasyTierService::startVpnService(const QString& instanceName, const QString& ipv4,
                                       int prefix, const QStringList& routes) {
    LogService::instance()->info(
        QString("Starting VPN: %1 @ %2/%3").arg(instanceName, ipv4).arg(prefix), "EasyTier");

#ifdef Q_OS_ANDROID
    JniBridge::startVpnService(instanceName, ipv4, prefix, routes);
#else
    LogService::instance()->warn("VpnService not available on desktop", "EasyTier");
#endif
}

void EasyTierService::stopVpnService() {
    LogService::instance()->info("Stopping VPN", "EasyTier");
#ifdef Q_OS_ANDROID
    JniBridge::stopVpnService();
#endif
}

QString EasyTierService::intToIp(int addr) {
    return QString("%1.%2.%3.%4")
        .arg((addr >> 24) & 0xFF)
        .arg((addr >> 16) & 0xFF)
        .arg((addr >> 8) & 0xFF)
        .arg(addr & 0xFF);
}
