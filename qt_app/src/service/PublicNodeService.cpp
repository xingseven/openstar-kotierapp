#include "PublicNodeService.hpp"
#include "LogService.hpp"
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QRegularExpression>
#include <QUrl>

const QString PublicNodeService::STATUS_URL =
    "https://info.qtet.cn/uptime/status/easytier";
const QString PublicNodeService::HEARTBEAT_URL =
    "https://info.qtet.cn/uptime/api/status-page/heartbeat/easytier";
const QStringList PublicNodeService::RELAY_GROUP_NAMES =
    {"社区公共节点",       // "社区公共节点"
     "社区公共节点[海外]"}; // "社区公共节点[海外]"

PublicNodeService* PublicNodeService::instance() {
    static PublicNodeService s_instance;
    return &s_instance;
}

PublicNodeService::PublicNodeService(QObject* parent)
    : QObject(parent)
{
    m_manager = new QNetworkAccessManager(this);
}

void PublicNodeService::fetchNodes(std::function<void(QList<PublicNode>)> callback) {
    auto* reply = m_manager->get(QNetworkRequest(QUrl(STATUS_URL)));

    connect(reply, &QNetworkReply::finished, this, [this, reply, callback]() {
        reply->deleteLater();

        if (reply->error() != QNetworkReply::NoError) {
            LogService::instance()->error(
                QString("fetchNodes HTTP error: %1").arg(reply->errorString()), "PublicNode");
            callback(QList<PublicNode>());
            return;
        }

        QString html = QString::fromUtf8(reply->readAll());

        // 提取 window.preloadData
        QRegularExpression preloadRe(
            R"(window\.preloadData\s*=\s*(\{[\s\S]*?\});\s*</script>)");
        auto match = preloadRe.match(html);
        if (!match.hasMatch()) {
            LogService::instance()->warn("fetchNodes: preloadData not found", "PublicNode");
            callback(QList<PublicNode>());
            return;
        }

        QString jsStr = match.captured(1);
        QString jsonStr = jsLiteralToJson(jsStr);

        QJsonDocument doc = QJsonDocument::fromJson(jsonStr.toUtf8());
        if (!doc.isObject()) {
            callback(QList<PublicNode>());
            return;
        }

        QJsonObject data = doc.object();
        QJsonArray groups = data["publicGroupList"].toArray();

        QList<PublicNode> nodes;

        for (const auto& g : groups) {
            QJsonObject group = g.toObject();
            QString groupName = group["name"].toString();
            if (!RELAY_GROUP_NAMES.contains(groupName)) continue;

            QJsonArray monitors = group["monitorList"].toArray();
            for (const auto& m : monitors) {
                QJsonObject mObj = m.toObject();
                int id = mObj["id"].toInt();
                QString rawName = mObj["name"].toString();
                QString type = mObj["type"].toString("port");

                auto [serverUrl, desc] = parseNodeName(rawName);
                if (serverUrl.isEmpty()) continue;

                PublicNode node;
                node.id = id;
                node.name = rawName;
                node.serverUrl = resolveUrl(serverUrl);
                node.description = desc;
                node.type = type;
                node.group = groupName;
                nodes.append(node);
            }
        }

        LogService::instance()->info(
            QString("Fetched %1 public nodes").arg(nodes.size()), "PublicNode");
        callback(nodes);
    });
}

void PublicNodeService::attachHeartbeat(QList<PublicNode>& nodes,
                                         std::function<void()> callback) {
    if (nodes.isEmpty()) {
        callback();
        return;
    }

    auto* reply = m_manager->get(QNetworkRequest(QUrl(HEARTBEAT_URL)));

    connect(reply, &QNetworkReply::finished, this, [this, reply, &nodes, callback]() {
        reply->deleteLater();

        if (reply->error() != QNetworkReply::NoError) {
            LogService::instance()->warn(
                QString("heartbeat HTTP error: %1").arg(reply->errorString()), "PublicNode");
            callback();
            return;
        }

        QString body = QString::fromUtf8(reply->readAll());
        QJsonDocument doc = QJsonDocument::fromJson(body.toUtf8());
        if (!doc.isObject()) { callback(); return; }

        QJsonObject data = doc.object();
        QJsonObject heartbeatList = data["heartbeatList"].toObject();

        for (auto& node : nodes) {
            QString key = QString::number(node.id);
            if (!heartbeatList.contains(key)) continue;

            QJsonArray entries = heartbeatList[key].toArray();
            if (entries.isEmpty()) continue;

            QJsonObject last = entries.last().toObject();
            node.status = last["status"].toInt(0);
            if (last.contains("ping"))
                node.ping = last["ping"].toInt(-1);
        }

        callback();
    });
}

QString PublicNodeService::jsLiteralToJson(const QString& input) {
    QString result;
    result.reserve(input.size());
    bool inStr = false;

    for (int i = 0; i < input.size(); ++i) {
        QChar c = input[i];
        if (c == '\\' && inStr && i + 1 < input.size()) {
            result.append(c);
            result.append(input[i + 1]);
            ++i;
        } else if (c == '\'') {
            result.append('"');
            inStr = !inStr;
        } else if (c == '"' && inStr) {
            result.append("\\\"");
        } else {
            result.append(c);
        }
    }

    return result;
}

QPair<QString, QString> PublicNodeService::parseNodeName(const QString& name) {
    // 剥离前缀标签 [xxx]
    QString clean = name.trimmed();
    clean.replace(QRegularExpression(R"(^\[.+?\])"), "").trimmed();

    static QRegularExpression urlRe(R"(^(\w+://[^\s（(]+)[（(](.+)[）)]$)");
    auto match = urlRe.match(clean);
    if (match.hasMatch()) {
        QString url = match.captured(1).trimmed();
        url.replace(QRegularExpression(R"(^\[.+?\])"), "");
        return {url.trimmed(), match.captured(2).trimmed()};
    }
    return {"", ""};
}

QString PublicNodeService::resolveUrl(const QString& originalUrl) {
    QString url = originalUrl;
    url.replace("*", "");
    return url;
}
