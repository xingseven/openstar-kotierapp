import QtQuick
import QtQuick.Controls
import QtQuick.Shapes

// 节点信息卡片（对应 Kotlin NodeInfoCard）
Rectangle {
    id: root
    implicitWidth: parent ? parent.width : 300
    implicitHeight: 64

    property var node: ({})
    property bool isLocal: false
    property string hostname: ""
    property string virtualIp: ""
    property int latencyMs: 0
    property string protocol: ""
    property string connectionType: "unknown"
    property string trafficText: ""

    color: Theme.surface
    radius: Theme.radiusSmall

    // 连接类型颜色
    function statusColor(connType, local) {
        if (local) return Theme.accent
        switch (connType) {
            case "direct": return Theme.statusDirect
            case "relay":  return Theme.statusRelay
            case "server": return Theme.statusServer
            default:       return Theme.statusOffline
        }
    }

    Row {
        anchors.fill: parent
        anchors.margins: 12
        spacing: 12
        verticalAlignment: Qt.AlignVCenter

        // 状态指示灯
        Rectangle {
            width: 10
            height: 10
            radius: 5
            anchors.verticalCenter: parent.verticalCenter
            color: statusColor(connectionType, isLocal)
        }

        // 图标
        Rectangle {
            width: 32
            height: 32
            radius: 16
            anchors.verticalCenter: parent.verticalCenter
            color: Theme.surfaceVariant
            Text {
                anchors.centerIn: parent
                text: ""  // fa-desktop（使用字体图标或emoji替代）
                font.pixelSize: 16
                color: Theme.text
            }
        }

        // 节点信息
        Column {
            anchors.verticalCenter: parent.verticalCenter
            spacing: 2

            Row {
                spacing: 6
                Text {
                    text: hostname
                    font.pixelSize: Theme.fontSizeBody
                    font.bold: true
                    color: Theme.text
                }
                Text {
                    text: isLocal ? "(本机)" : ""
                    font.pixelSize: Theme.fontSizeSmall
                    color: Theme.accent
                    visible: isLocal
                }
            }

            Text {
                text: virtualIp
                font.pixelSize: Theme.fontSizeSmall
                color: Theme.surfaceVariant
            }

            Text {
                text: latencyMs > 0 ? latencyMs + "ms" : "-"
                font.pixelSize: Theme.fontSizeSmall
                color: Theme.surfaceVariant
            }
        }

        // 流量信息
        Text {
            anchors.verticalCenter: parent.verticalCenter
            anchors.right: parent.right
            font.pixelSize: Theme.fontSizeSmall
            color: Theme.surfaceVariant
            text: trafficText
        }
    }
}
