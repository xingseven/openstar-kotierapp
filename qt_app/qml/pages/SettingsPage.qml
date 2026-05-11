import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import "../components"

// 设置页面（对应 Kotlin SettingsPage）
Item {
    id: root
    anchors.fill: parent

    CompactTopBar {
        id: topBar
        anchors.top: parent.top
        width: parent.width
        title: "设置"
    }

    Flickable {
        anchors.top: topBar.bottom
        anchors.bottom: parent.bottom
        width: parent.width
        contentHeight: column.height + 32
        clip: true

        Column {
            id: column
            width: parent.width - 32
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.top: parent.top
            anchors.topMargin: 16
            spacing: 16

            // ── 通用 ──
            SectionCard {
                title: "通用"
                width: parent.width

                CustomSwitch {
                    width: parent.width
                    label: "跟随系统"
                    value: settingsRepo ? settingsRepo.followSystemTheme : true
                    onToggled: {
                        if (settingsRepo) {
                            settingsRepo.followSystemTheme = newValue
                            if (newValue) {
                                darkModeSwitch.enabled = false
                            } else {
                                darkModeSwitch.enabled = true
                            }
                        }
                    }
                }

                CustomSwitch {
                    id: darkModeSwitch
                    width: parent.width
                    label: "深色模式"
                    value: settingsRepo ? settingsRepo.darkMode : false
                    enabled: settingsRepo ? !settingsRepo.followSystemTheme : true
                    onToggled: {
                        if (settingsRepo) {
                            settingsRepo.darkMode = newValue
                            Theme.isDark = newValue
                        }
                    }
                }

                CustomSwitch {
                    width: parent.width
                    label: "开机自启"
                    value: settingsRepo ? settingsRepo.startOnBoot : false
                    onToggled: {
                        if (settingsRepo)
                            settingsRepo.startOnBoot = newValue
                    }
                }
            }

            // ── 网络 ──
            SectionCard {
                title: "网络"
                width: parent.width

                CustomSwitch {
                    width: parent.width
                    label: "自动回连"
                    hint: "断开后自动重新连接"
                    value: settingsRepo ? settingsRepo.autoReconnect : false
                    onToggled: {
                        if (settingsRepo)
                            settingsRepo.autoReconnect = newValue
                    }
                }
            }

            // ── 通知 ──
            SectionCard {
                title: "通知"
                width: parent.width

                CustomSwitch {
                    width: parent.width
                    label: "连接通知"
                    value: settingsRepo ? settingsRepo.notifyOnConnect : true
                    onToggled: {
                        if (settingsRepo)
                            settingsRepo.notifyOnConnect = newValue
                    }
                }

                CustomSwitch {
                    width: parent.width
                    label: "断开通知"
                    value: settingsRepo ? settingsRepo.notifyOnDisconnect : true
                    onToggled: {
                        if (settingsRepo)
                            settingsRepo.notifyOnDisconnect = newValue
                    }
                }
            }

            // ── 日志 ──
            SectionCard {
                title: "日志"
                width: parent.width

                Row {
                    width: parent.width
                    spacing: 8

                    Text {
                        text: "日志级别"
                        font.pixelSize: Theme.fontSizeBody
                        color: Theme.text
                        anchors.verticalCenter: parent.verticalCenter
                    }

                    ComboBox {
                        width: 120
                        model: ["debug", "info", "warn", "error"]
                        currentIndex: {
                            var level = settingsRepo ? settingsRepo.logLevel : "info"
                            if (level === "debug") return 0
                            if (level === "info") return 1
                            if (level === "warn") return 2
                            if (level === "error") return 3
                            return 1
                        }
                        onActivated: {
                            if (settingsRepo)
                                settingsRepo.logLevel = currentText
                        }
                    }
                }

                Item { width: parent.width; height: 8 }

                Button {
                    text: "查看运行日志"
                    flat: true
                    onClicked: {
                        // 导航到日志页面
                        logPageLoader.active = true
                    }
                }
            }

            // ── 关于 ──
            SectionCard {
                title: "关于"
                width: parent.width

                Column {
                    width: parent.width
                    spacing: 4

                    Text { text: "版本: 1.0.0"; font.pixelSize: Theme.fontSizeBody; color: Theme.text }
                    Text { text: "平台: Android / C++ (Qt 6)"; font.pixelSize: Theme.fontSizeBody; color: Theme.text }
                    Text { text: "后端: EasyTier Rust"; font.pixelSize: Theme.fontSizeBody; color: Theme.text }
                }
            }

            // ── 清除数据 ──
            Button {
                width: parent.width
                text: "清除所有数据"
                highlighted: true
                onClicked: {
                    clearDialog.open()
                }
            }
        }
    }

    // 日志页面（内嵌）
    Loader {
        id: logPageLoader
        anchors.fill: parent
        active: false
        sourceComponent: LogPage {
            onBackRequested: logPageLoader.active = false
        }
    }

    // 清除确认对话框
    Dialog {
        id: clearDialog
        title: "确认清除"
        standardButtons: Dialog.Yes | Dialog.No
        modal: true

        Label {
            text: "确定要清除所有数据吗？此操作不可恢复。"
        }

        onAccepted: {
            if (settingsRepo)
                settingsRepo.clearAll()
        }
    }
}
