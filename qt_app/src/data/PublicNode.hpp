#ifndef PUBLICNODE_HPP
#define PUBLICNODE_HPP

#include <QString>

struct PublicNode {
    int id = 0;
    QString name;
    QString serverUrl;
    QString description;
    QString type;
    QString group;
    int ping = -1;
    int status = 0; // 1 = online

    bool isOnline() const { return status == 1; }
};

#endif // PUBLICNODE_HPP
