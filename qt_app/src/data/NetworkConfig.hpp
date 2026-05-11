#ifndef NETWORKCONFIG_HPP
#define NETWORKCONFIG_HPP

#include <QString>
#include <QStringList>
#include <QJsonObject>
#include <QJsonArray>
#include <QRandomGenerator>

struct NetworkConfig {
    QString instanceName;
    QString networkLabel;
    bool isRunning = false;

    QString hostname;
    QString networkName;
    QString networkSecret;
    bool dhcp = true;
    QString ipv4;
    bool latencyFirst = false;
    bool privateMode = true;
    QStringList servers = {"wss://qtet-public.070219.xyz"};

    bool enableKcpProxy = true;
    bool disableKcpInput = false;
    bool noTun = false;
    bool enableQuicProxy = false;
    bool disableQuicInput = false;
    bool disableUdpHolePunching = false;
    bool multiThread = true;
    bool useSmoltcp = false;
    bool bindDevice = true;
    bool disableP2p = false;
    bool enableExitNode = false;
    bool systemForwarding = false;
    bool disableSymHolePunching = false;
    bool disableIpv6 = false;
    bool relayAllPeerRpc = false;
    bool enableEncryption = true;
    bool acceptDns = false;

    bool foreignNetworkWhitelistEnabled = false;
    QStringList foreignNetworkWhitelist;
    QStringList listenAddresses = {"tcp://0.0.0.0:11010", "udp://0.0.0.0:11010"};
    QStringList proxyNetworks;

    NetworkConfig() {
        instanceName = generateInstanceName();
    }

    // 将配置序列化为 TOML 字符串（必须与 Kotlin toToml() 输出完全一致）
    QString toToml() const;

    // JSON 序列化/反序列化
    QJsonObject toJson() const;
    static NetworkConfig fromJson(const QJsonObject& json);

    static QString generateInstanceName() {
        static const char chars[] = "0123456789abcdefghijklmnopqrstuvwxyz";
        QString suffix;
        for (int i = 0; i < 10; ++i) {
            int idx = QRandomGenerator::global()->bounded(36);
            suffix.append(chars[idx]);
        }
        return QStringLiteral("EasyTierET-") + suffix;
    }
};

#endif // NETWORKCONFIG_HPP
