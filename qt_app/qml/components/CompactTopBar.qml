import QtQuick
import QtQuick.Controls

// 可复用的紧凑顶部导航栏
Item {
    id: root
    height: 44

    property alias title: titleText.text
    property alias titleItem: titleText
    property alias actionRow: actionRow

    default property alias actionItems: actionRow.data

    Rectangle {
        anchors.fill: parent
        color: Theme.surface

        Row {
            id: actionRow
            anchors.right: parent.right
            anchors.rightMargin: 8
            anchors.verticalCenter: parent.verticalHeight
            height: parent.height
            layoutDirection: Qt.RightToLeft
            spacing: 4
        }
    }

    Text {
        id: titleText
        anchors.left: parent.left
        anchors.leftMargin: 16
        anchors.verticalCenter: parent.verticalCenter
        font.pixelSize: Theme.fontSizeTitle
        font.bold: true
        color: Theme.text
    }

    // 底部 0.5dp 分隔线
    Rectangle {
        anchors.bottom: parent.bottom
        width: parent.width
        height: 0.5
        color: Theme.divider
    }
}
