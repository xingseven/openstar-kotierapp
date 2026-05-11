#ifndef SERVERENTRY_HPP
#define SERVERENTRY_HPP

#include <QString>
#include <QJsonObject>

struct ServerEntry {
    QString name;
    QString url;
    bool isDefault = false;

    QJsonObject toJson() const {
        QJsonObject obj;
        obj["name"] = name;
        obj["url"] = url;
        obj["is_default"] = isDefault;
        return obj;
    }

    static ServerEntry fromJson(const QJsonObject& json) {
        ServerEntry entry;
        entry.name = json["name"].toString();
        entry.url = json["url"].toString();
        entry.isDefault = json["is_default"].toBool(false);
        return entry;
    }
};

#endif // SERVERENTRY_HPP
