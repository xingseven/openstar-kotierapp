#include "NetworkConfig.hpp"
#include <QJsonDocument>
#include <QJsonArray>

QString NetworkConfig::toToml() const {
    QString result;

    auto appendLine = [&](const QString& line) {
        result += line + "\n";
    };

    auto appendBool = [&](const QString& key, bool val) {
        appendLine(QStringLiteral("%1 = %2").arg(key, val ? "true" : "false"));
    };

    auto appendStr = [&](const QString& key, const QString& val) {
        appendLine(QStringLiteral("%1 = \"%2\"").arg(key, val));
    };

    // instance_name 和 hostname
    appendStr("instance_name", instanceName);
    appendStr("hostname", hostname);
    appendBool("dhcp", dhcp);

    if (!ipv4.isEmpty() && !dhcp) {
        appendStr("ipv4", ipv4);
    }

    // listeners
    if (!listenAddresses.isEmpty()) {
        appendLine("");
        appendLine("listeners = [");
        for (const auto& addr : listenAddresses) {
            appendLine(QStringLiteral("    \"%1\",").arg(addr));
        }
        appendLine("]");
    }

    // [network_identity]
    appendLine("");
    appendLine("[network_identity]");
    appendStr("network_name", networkName);
    appendStr("network_secret", networkSecret);

    // [[peer]]
    for (const auto& server : servers) {
        appendLine("");
        appendLine("[[peer]]");
        appendStr("uri", server);
    }

    // [[proxy_network]]
    for (const auto& cidr : proxyNetworks) {
        appendLine("");
        appendLine("[[proxy_network]]");
        appendStr("cidr", cidr);
    }

    // [flags]
    appendLine("");
    appendLine("[flags]");
    appendBool("enable_encryption", enableEncryption);
    appendBool("enable_ipv6", !disableIpv6);
    appendBool("latency_first", latencyFirst);
    appendBool("enable_exit_node", enableExitNode);
    appendBool("no_tun", noTun);
    appendBool("use_smoltcp", useSmoltcp);

    if (foreignNetworkWhitelistEnabled) {
        appendLine(QStringLiteral("foreign_network_whitelist = \"%1\"")
            .arg(foreignNetworkWhitelist.join(" ")));
    }

    appendBool("enable_quic_proxy", enableQuicProxy);
    appendBool("disable_quic_input", disableQuicInput);
    appendBool("enable_kcp_proxy", enableKcpProxy);
    appendBool("disable_kcp_input", disableKcpInput);
    appendBool("bind_device", bindDevice);
    appendBool("private_mode", privateMode);
    appendBool("disable_p2p", disableP2p);
    appendBool("multi_thread", multiThread);
    appendBool("accept_dns", acceptDns);
    appendBool("disable_sym_hole_punching", disableSymHolePunching);
    appendBool("relay_all_peer_rpc", relayAllPeerRpc);
    appendBool("disable_udp_hole_punching", disableUdpHolePunching);
    appendBool("proxy_forward_by_system", systemForwarding);

    return result;
}

QJsonObject NetworkConfig::toJson() const {
    QJsonObject obj;
    obj["instance_name"] = instanceName;
    obj["network_label"] = networkLabel;
    obj["is_running"] = isRunning;
    obj["hostname"] = hostname;
    obj["network_name"] = networkName;
    obj["network_secret"] = networkSecret;
    obj["dhcp"] = dhcp;
    obj["ipv4"] = ipv4;
    obj["latency_first"] = latencyFirst;
    obj["private_mode"] = privateMode;
    obj["servers"] = QJsonArray::fromStringList(servers);
    obj["enable_kcp_proxy"] = enableKcpProxy;
    obj["disable_kcp_input"] = disableKcpInput;
    obj["no_tun"] = noTun;
    obj["enable_quic_proxy"] = enableQuicProxy;
    obj["disable_quic_input"] = disableQuicInput;
    obj["disable_udp_hole_punching"] = disableUdpHolePunching;
    obj["multi_thread"] = multiThread;
    obj["use_smoltcp"] = useSmoltcp;
    obj["bind_device"] = bindDevice;
    obj["disable_p2p"] = disableP2p;
    obj["enable_exit_node"] = enableExitNode;
    obj["system_forwarding"] = systemForwarding;
    obj["disable_sym_hole_punching"] = disableSymHolePunching;
    obj["disable_ipv6"] = disableIpv6;
    obj["relay_all_peer_rpc"] = relayAllPeerRpc;
    obj["enable_encryption"] = enableEncryption;
    obj["accept_dns"] = acceptDns;
    obj["foreign_network_whitelist_enabled"] = foreignNetworkWhitelistEnabled;
    obj["foreign_network_whitelist"] = QJsonArray::fromStringList(foreignNetworkWhitelist);
    obj["listen_addresses"] = QJsonArray::fromStringList(listenAddresses);
    obj["proxy_networks"] = QJsonArray::fromStringList(proxyNetworks);
    return obj;
}

NetworkConfig NetworkConfig::fromJson(const QJsonObject& json) {
    NetworkConfig cfg;
    cfg.instanceName = json["instance_name"].toString();
    cfg.networkLabel = json["network_label"].toString();
    cfg.isRunning = json["is_running"].toBool(false);
    cfg.hostname = json["hostname"].toString();
    cfg.networkName = json["network_name"].toString();
    cfg.networkSecret = json["network_secret"].toString();
    cfg.dhcp = json["dhcp"].toBool(true);
    cfg.ipv4 = json["ipv4"].toString();
    cfg.latencyFirst = json["latency_first"].toBool(false);
    cfg.privateMode = json["private_mode"].toBool(true);

    cfg.servers.clear();
    for (const auto& v : json["servers"].toArray())
        cfg.servers.append(v.toString());

    cfg.enableKcpProxy = json["enable_kcp_proxy"].toBool(true);
    cfg.disableKcpInput = json["disable_kcp_input"].toBool(false);
    cfg.noTun = json["no_tun"].toBool(false);
    cfg.enableQuicProxy = json["enable_quic_proxy"].toBool(false);
    cfg.disableQuicInput = json["disable_quic_input"].toBool(false);
    cfg.disableUdpHolePunching = json["disable_udp_hole_punching"].toBool(false);
    cfg.multiThread = json["multi_thread"].toBool(true);
    cfg.useSmoltcp = json["use_smoltcp"].toBool(false);
    cfg.bindDevice = json["bind_device"].toBool(true);
    cfg.disableP2p = json["disable_p2p"].toBool(false);
    cfg.enableExitNode = json["enable_exit_node"].toBool(false);
    cfg.systemForwarding = json["system_forwarding"].toBool(false);
    cfg.disableSymHolePunching = json["disable_sym_hole_punching"].toBool(false);
    cfg.disableIpv6 = json["disable_ipv6"].toBool(false);
    cfg.relayAllPeerRpc = json["relay_all_peer_rpc"].toBool(false);
    cfg.enableEncryption = json["enable_encryption"].toBool(true);
    cfg.acceptDns = json["accept_dns"].toBool(false);
    cfg.foreignNetworkWhitelistEnabled = json["foreign_network_whitelist_enabled"].toBool(false);

    cfg.foreignNetworkWhitelist.clear();
    for (const auto& v : json["foreign_network_whitelist"].toArray())
        cfg.foreignNetworkWhitelist.append(v.toString());

    cfg.listenAddresses.clear();
    for (const auto& v : json["listen_addresses"].toArray())
        cfg.listenAddresses.append(v.toString());

    cfg.proxyNetworks.clear();
    for (const auto& v : json["proxy_networks"].toArray())
        cfg.proxyNetworks.append(v.toString());

    return cfg;
}
