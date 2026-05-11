import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import "../components"

// 服务器管理页面（对应 Kotlin ServersPage）
Item {
    id: root
    anchors.fill: parent

    CompactTopBar {
        id: topBar
        anchors.top: parent.top
        width: parent.width
        title: "服务器"

        actionRow: [
            Button {
                text: "+ 添加"
                flat: true
                anchors.verticalCenter: parent.verticalCenter
                onClicked: addDialog.open()
            }
        ]
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

            // ── 公共节点（可折叠） ──
            SectionCard {
                title: "公共节点"
                width: parent.width

                // 可折叠控制
                Button {
                    text: publicListVisible ? "收起" : "展开"
                    flat: true
                    anchors.right: parent.right
                    anchors.top: parent.top
                    onClicked: publicListVisible = !publicListVisible
                }

                property bool publicListVisible: true

                Column {
                    visible: publicListVisible
                    width: parent.width

                    Repeater {
                        model: publicNodeModel

                        Rectangle {
                            width: parent.width
                            height: 72
                            color: "transparent"
                            border.color: Theme.divider
                            border.width: 1
                            radius: Theme.radiusSmall

                            Row {
                                anchors.fill: parent
                                anchors.margins: 8
                                spacing: 8

                                // 状态灯
                                Rectangle {
                                    width: 8; height: 8; radius: 4
                                    anchors.verticalCenter: parent.verticalCenter
                                    color: status === 1 ? Theme.statusOnline : Theme.statusOffline
                                }

                                Column {
                                    anchors.verticalCenter: parent.verticalCenter
                                    spacing: 2

                                    Text {
                                        text: description
                                        font.pixelSize: Theme.fontSizeBody
                                        font.bold: true
                                        color: Theme.text
                                        elide: Text.ElideRight
                                        width: 150
                                    }

                                    Text {
                                        text: serverUrl
                                        font.pixelSize: Theme.fontSizeSmall
                                        color: Theme.surfaceVariant
                                        elide: Text.ElideRight
                                        width: 150
                                    }

                                    Text {
                                        text: ping >= 0 ? ping + "ms" : "检测中..."
                                        font.pixelSize: Theme.fontSizeSmall
                                        color: Theme.surfaceVariant
                                    }
                                }

                                Button {
                                    anchors.right: parent.right
                                    anchors.verticalCenter: parent.verticalCenter
                                    text: "使用"
                                    onClicked: {
                                        // 添加到收藏 + 第一个配置的服务器列表
                                        addToFavorites(serverUrl, description)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 我的收藏 ──
            SectionCard {
                title: "我的收藏"
                width: parent.width

                Repeater {
                    model: favoriteServerModel

                    Rectangle {
                        width: parent.width
                        height: 48
                        color: "transparent"
                        border.color: Theme.divider
                        border.width: 1
                        radius: Theme.radiusSmall

                        Row {
                            anchors.fill: parent
                            anchors.margins: 8
                            spacing: 8

                            Column {
                                anchors.verticalCenter: parent.verticalCenter
                                Text {
                                    text: name
                                    font.pixelSize: Theme.fontSizeBody
                                    color: Theme.text
                                }
                                Text {
                                    text: url
                                    font.pixelSize: Theme.fontSizeSmall
                                    color: Theme.surfaceVariant
                                }
                            }

                            Button {
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                text: "编辑"
                                flat: true
                                onClicked: {
                                    editIndex = index
                                    editDialog.open()
                                }
                            }
                        }
                    }
                }

                // 空状态
                Text {
                    visible: favoriteServerModel.count === 0
                    text: "暂无收藏服务器\n点击右上角 + 添加"
                    color: Theme.surfaceVariant
                    font.pixelSize: Theme.fontSizeBody
                    horizontalAlignment: Text.AlignHCenter
                    width: parent.width
                    height: 60
                }
            }
        }
    }

    // ── 模拟数据 ──
    ListModel { id: publicNodeModel }
    ListModel { id: favoriteServerModel }

    property int editIndex: -1

    // ── 添加服务器对话框 ──
    Dialog {
        id: addDialog
        title: "添加服务器"
        standardButtons: Dialog.Save | Dialog.Cancel
        modal: true

        Column {
            spacing: 8
            width: 250

            TextField {
                id: addNameField
                placeholderText: "服务器名称"
                width: parent.width
            }
            TextField {
                id: addUrlField
                placeholderText: "服务器 URL"
                width: parent.width
            }
        }

        onAccepted: {
            if (addNameField.text.length > 0 && addUrlField.text.length > 0) {
                favoriteServerModel.append({
                    name: addNameField.text,
                    url: addUrlField.text
                })
                addNameField.text = ""
                addUrlField.text = ""
            }
        }
    }

    // ── 编辑服务器对话框 ──
    Dialog {
        id: editDialog
        title: "编辑服务器"
        standardButtons: Dialog.Save | Dialog.Cancel | Dialog.Destructive
        modal: true

        Column {
            spacing: 8
            width: 250

            TextField {
                id: editNameField
                placeholderText: "服务器名称"
                width: parent.width
            }
            TextField {
                id: editUrlField
                placeholderText: "服务器 URL"
                width: parent.width
            }
        }

        onAccepted: {
            if (editIndex >= 0) {
                favoriteServerModel.set(editIndex, {
                    name: editNameField.text,
                    url: editUrlField.text
                })
            }
        }

        onDestructiveClicked: {
            if (editIndex >= 0) {
                favoriteServerModel.remove(editIndex)
            }
        }
    }

    function addToFavorites(serverUrl, description) {
        // 检查是否已存在
        for (var i = 0; i < favoriteServerModel.count; i++) {
            if (favoriteServerModel.get(i).url === serverUrl)
                return
        }
        favoriteServerModel.append({
            name: description,
            url: serverUrl
        })
    }

    // 页面加载时获取公共节点
    Component.onCompleted: {
        if (publicNodeService) {
            publicNodeService.fetchNodes(function(nodes) {
                publicNodeModel.clear()
                for (var i = 0; i < nodes.length; i++) {
                    var n = nodes[i]
                    publicNodeModel.append({
                        id: n.id,
                        name: n.name,
                        serverUrl: n.serverUrl,
                        description: n.description,
                        type: n.type,
                        group: n.group,
                        ping: n.ping,
                        status: n.status
                    })
                }
            })
        }
    }
}
