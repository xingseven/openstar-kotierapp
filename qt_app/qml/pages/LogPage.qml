import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import "../components"

// 日志查看页面（对应 Kotlin LogPage）
Item {
    id: root
    anchors.fill: parent

    signal backRequested()

    CompactTopBar {
        id: topBar
        anchors.top: parent.top
        width: parent.width
        title: "运行日志"

        actionRow: [
            Switch {
                id: autoScrollSwitch
                text: "自动滚动"
                checked: true
                anchors.verticalCenter: parent.verticalCenter
            },
            Button {
                text: "清除"
                flat: true
                anchors.verticalCenter: parent.verticalCenter
                onClicked: {
                    if (easyTierService)
                        logModel.clear()
                }
            },
            Button {
                text: "← 返回"
                flat: true
                anchors.verticalCenter: parent.verticalCenter
                onClicked: backRequested()
            }
        ]
    }

    // 日志列表
    ListView {
        anchors.top: topBar.bottom
        anchors.bottom: parent.bottom
        width: parent.width
        clip: true
        spacing: 1

        model: ListModel { id: logModel }

        delegate: Rectangle {
            width: parent.width
            height: 20
            color: {
                switch (level) {
                    case "DEBUG": return Qt.rgba(0,0,0,0)
                    case "WARN":  return Qt.rgba(1,0.65,0.15,0.1)
                    case "ERROR": return Qt.rgba(1,0,0,0.1)
                    default:      return Qt.rgba(0,0,0,0)
                }
            }

            Text {
                anchors.left: parent.left
                anchors.leftMargin: 8
                anchors.verticalCenter: parent.verticalCenter
                text: displayText
                font.pixelSize: Theme.fontSizeSmall
                font.family: "monospace"
                color: {
                    switch (level) {
                        case "DEBUG": return Theme.logDebug
                        case "WARN":  return Theme.logWarn
                        case "ERROR": return Theme.logError
                        default:      return Theme.text
                    }
                }
                elide: Text.ElideRight
            }
        }

        // 定时刷新日志
        Timer {
            interval: 1000
            repeat: true
            running: true
            onTriggered: {
                // 模拟日志刷新（实际由 LogService 的信号驱动）
                // 此处简化处理，真实场景通过 C++ 信号槽连接
            }
        }

        // 自动滚动到底部
        onCountChanged: {
            if (autoScrollSwitch.checked && count > 0)
                positionViewAtEnd()
        }
    }
}
