import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import "../components"

// 一键联机页面（对应 Kotlin OneClickPage）
Item {
    id: root
    anchors.fill: parent

    property bool isHostMode: true
    property bool isRunning: false
    property bool isLoading: false
    property string generatedCode: ""
    property string statusMessage: ""
    property bool statusIsError: false

    CompactTopBar {
        id: topBar
        anchors.top: parent.top
        width: parent.width
        title: "一键联机"
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

            // ── 模式切换 ──
            SectionCard {
                width: parent.width

                Row {
                    width: parent.width
                    spacing: 8

                    Button {
                        width: (parent.width - 8) / 2
                        text: "创建网络"
                        highlighted: root.isHostMode
                        flat: !root.isHostMode
                        onClicked: {
                            root.isHostMode = true
                            root.statusMessage = ""
                        }
                    }

                    Button {
                        width: (parent.width - 8) / 2
                        text: "加入网络"
                        highlighted: !root.isHostMode
                        flat: root.isHostMode
                        onClicked: {
                            root.isHostMode = false
                            root.statusMessage = ""
                        }
                    }
                }
            }

            // ── 房主模式 ──
            Column {
                width: parent.width
                visible: root.isHostMode
                spacing: 16

                Rectangle {
                    width: 80
                    height: 80
                    radius: 40
                    anchors.horizontalCenter: parent.horizontalCenter
                    color: root.isRunning ? Theme.statusOnline : Theme.surfaceVariant

                    Text {
                        anchors.centerIn: parent
                        text: "📶"
                        font.pixelSize: 32
                    }
                }

                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: root.isRunning ? "运行中" : "未运行"
                    font.pixelSize: Theme.fontSizeTitle
                    color: root.isRunning ? Theme.statusOnline : Theme.surfaceVariant
                }

                // 启动/停止按钮
                Button {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: root.isRunning ? "停止网络" : "启动网络"
                    highlighted: !root.isRunning
                    onClicked: {
                        if (root.isRunning) {
                            stopHost()
                        } else {
                            startHost()
                        }
                    }
                }

                // 联机码显示
                Rectangle {
                    width: parent.width
                    visible: root.isRunning && root.generatedCode.length > 0
                    height: 80
                    color: Theme.surfaceVariant
                    radius: Theme.radiusMedium

                    Column {
                        anchors.centerIn: parent
                        spacing: 8

                        Text {
                            anchors.horizontalCenter: parent.horizontalCenter
                            text: root.generatedCode
                            font.pixelSize: Theme.fontSizeLarge
                            font.bold: true
                            font.family: "monospace"
                            color: Theme.accent
                        }

                        Row {
                            anchors.horizontalCenter: parent.horizontalCenter
                            spacing: 16

                            Button {
                                text: "复制联机码"
                                flat: true
                                onClicked: {
                                    // 将联机码复制到剪贴板
                                    console.log("Copy:", root.generatedCode)
                                }
                            }

                            Button {
                                text: "刷新"
                                flat: true
                                onClicked: {
                                    // 重新生成
                                }
                            }
                        }
                    }
                }

                // 状态消息
                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: root.statusMessage
                    color: root.statusIsError ? Theme.errorRed : Theme.statusOnline
                    font.pixelSize: Theme.fontSizeBody
                    visible: root.statusMessage.length > 0
                }
            }

            // ── 访客模式 ──
            Column {
                width: parent.width
                visible: !root.isHostMode
                spacing: 16

                Rectangle {
                    width: parent.width
                    height: 60
                    color: Theme.surface
                    radius: Theme.radiusMedium
                    border.color: Theme.divider

                    Row {
                        anchors.fill: parent
                        anchors.margins: 12
                        spacing: 8

                        TextField {
                            id: guestCodeField
                            placeholderText: "输入联机码 XXXX-XXXX-XXXX-..."
                            width: parent.width - 80
                            anchors.verticalCenter: parent.verticalCenter
                            font.family: "monospace"
                        }

                        Button {
                            anchors.verticalCenter: parent.verticalCenter
                            text: "加入"
                            enabled: guestCodeField.text.length >= 24
                            highlighted: true
                            onClicked: {
                                joinNetwork()
                            }
                        }
                    }
                }

                // 已加入提示
                Rectangle {
                    width: parent.width
                    height: 60
                    visible: root.isRunning
                    color: Qt.rgba(Theme.statusOnline.r, Theme.statusOnline.g, Theme.statusOnline.b, 0.1)
                    radius: Theme.radiusMedium

                    Text {
                        anchors.centerIn: parent
                        text: "✓ 已加入网络"
                        color: Theme.statusOnline
                        font.pixelSize: Theme.fontSizeTitle
                    }
                }

                Button {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: "离开网络"
                    visible: root.isRunning
                    onClicked: {
                        leaveNetwork()
                    }
                }

                // 状态消息
                Text {
                    anchors.horizontalCenter: parent.horizontalCenter
                    text: root.statusMessage
                    color: root.statusIsError ? Theme.errorRed : Theme.statusOnline
                    font.pixelSize: Theme.fontSizeBody
                    visible: root.statusMessage.length > 0
                }
            }
        }
    }

    // ── 函数 ──

    function startHost() {
        root.isLoading = true
        root.statusMessage = "正在启动..."

        // 生成凭证和联机码
        // 实际使用 Base32::generateRoomCredentials + encodeConnectionCode
        root.generatedCode = "ABCD-EFGH-JKLM-NPQR-STUV-WX9"
        root.isRunning = true
        root.isLoading = false
        root.statusMessage = "网络已创建，分享联机码给朋友"
        root.statusIsError = false
    }

    function stopHost() {
        root.isRunning = false
        root.generatedCode = ""
        root.statusMessage = "网络已停止"
        root.statusIsError = false
    }

    function joinNetwork() {
        var code = guestCodeField.text.trim()
        if (code.length < 24) {
            root.statusMessage = "联机码格式不正确"
            root.statusIsError = true
            return
        }

        root.isRunning = true
        root.statusMessage = "已成功加入网络"
        root.statusIsError = false
    }

    function leaveNetwork() {
        root.isRunning = false
        root.statusMessage = "已离开网络"
        root.statusIsError = false
        guestCodeField.text = ""
    }
}
