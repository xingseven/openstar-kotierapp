import QtQuick
import QtQuick.Controls

// 带标签的开关组件（对应 Kotlin CustomSwitch）
Item {
    id: root
    implicitWidth: parent ? parent.width : 300
    implicitHeight: 48

    property alias label: labelText.text
    property alias hint: hintText.text
    property bool value: false
    signal toggled(bool newValue)

    Row {
        anchors.fill: parent
        anchors.leftMargin: 16
        anchors.rightMargin: 8
        spacing: 8
        verticalAlignment: Qt.AlignVCenter

        Column {
            width: parent.width - switchControl.width - 8
            anchors.verticalCenter: parent.verticalCenter
            spacing: 2

            Text {
                id: labelText
                font.pixelSize: Theme.fontSizeBody
                color: Theme.text
                elide: Text.ElideRight
            }

            Text {
                id: hintText
                font.pixelSize: Theme.fontSizeSmall
                color: Theme.surfaceVariant
                visible: text.length > 0
                elide: Text.ElideRight
            }
        }

        Switch {
            id: switchControl
            anchors.verticalCenter: parent.verticalCenter
            checked: root.value
            onClicked: {
                root.value = checked
                root.toggled(checked)
            }
            palette {
                accent: Theme.accent
            }
        }
    }
}
