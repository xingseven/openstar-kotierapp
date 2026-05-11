import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import "components"
import "pages"

// 主窗口 —— 底部导航容器（对应 Kotlin HomePage）
ApplicationWindow {
    id: window
    visible: true
    width: 360
    height: 640
    title: "EasyTier"

    // 初始化主题
    Component.onCompleted: {
        Theme.isDark = settingsRepo ? settingsRepo.darkMode : false
    }

    // 主内容区
    SwipeView {
        id: swipeView
        anchors.fill: parent
        anchors.bottomMargin: 56
        interactive: false
        currentIndex: mainController ? mainController.selectedTabIndex : 0

        NetworkConfigPage { }
        OneClickPage { }
        ServersPage { }
        SettingsPage { }
    }

    // 底部导航栏
    Rectangle {
        anchors.bottom: parent.bottom
        width: parent.width
        height: 56
        color: Theme.surface

        Rectangle {
            anchors.top: parent.top
            width: parent.width
            height: 0.5
            color: Theme.divider
        }

        Row {
            anchors.fill: parent
            spacing: 0

            Repeater {
                model: [
                    { label: "网络", icon: "🌐" },
                    { label: "一键联机", icon: "🔗" },
                    { label: "服务器", icon: "🖥" },
                    { label: "设置", icon: "⚙" }
                ]

                Item {
                    width: parent.width / 4
                    height: parent.height

                    property bool isSelected: index === swipeView.currentIndex

                    Rectangle {
                        anchors.fill: parent
                        color: isSelected ? Qt.rgba(Theme.accent.r, Theme.accent.g, Theme.accent.b, 0.1) : "transparent"

                        Column {
                            anchors.centerIn: parent
                            spacing: 2

                            Text {
                                anchors.horizontalCenter: parent.horizontalCenter
                                text: modelData.icon
                                font.pixelSize: 20
                            }

                            Text {
                                anchors.horizontalCenter: parent.horizontalCenter
                                text: modelData.label
                                font.pixelSize: 11
                                color: isSelected ? Theme.accent : Theme.surfaceVariant
                            }
                        }

                        MouseArea {
                            anchors.fill: parent
                            onClicked: {
                                swipeView.currentIndex = index
                                if (mainController)
                                    mainController.selectedTabIndex = index
                            }
                        }
                    }
                }
            }
        }
    }
}
