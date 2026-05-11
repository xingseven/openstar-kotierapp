#ifndef PUBLICNODESERVICE_HPP
#define PUBLICNODESERVICE_HPP

#include <QObject>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QList>
#include <functional>
#include "data/PublicNode.hpp"

class PublicNodeService : public QObject {
    Q_OBJECT
public:
    static PublicNodeService* instance();

    void fetchNodes(std::function<void(QList<PublicNode>)> callback);
    void attachHeartbeat(QList<PublicNode>& nodes, std::function<void()> callback);

private:
    explicit PublicNodeService(QObject* parent = nullptr);

    QString jsLiteralToJson(const QString& input);
    QPair<QString, QString> parseNodeName(const QString& name);
    QString resolveUrl(const QString& originalUrl);

    QNetworkAccessManager* m_manager = nullptr;
    static const QString STATUS_URL;
    static const QString HEARTBEAT_URL;
    static const QStringList RELAY_GROUP_NAMES;
};

#endif // PUBLICNODESERVICE_HPP
