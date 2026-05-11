import QtQuick
import QtQuick.Controls

// 分区卡片（用于设置页面等）
Rectangle {
    id: root
    implicitWidth: parent ? parent.width : 300
    implicitHeight: column.height + 24

    property alias title: headerText.text
    property alias contentItem: column

    default property alias contentItems: column.data

    color: Theme.surface
    radius: Theme.radiusMedium

    Column {
        id: column
        anchors.left: parent.left
        anchors.leftMargin: 16
        anchors.right: parent.right
        anchors.rightMargin: 16
        anchors.top: parent.top
        anchors.topMargin: 12
        spacing: 0

        Text {
            id: headerText
            font.pixelSize: Theme.fontSizeSmall
            font.bold: true
            color: Theme.accent
            visible: text.length > 0
            bottomPadding: 8
        }
    }
}
