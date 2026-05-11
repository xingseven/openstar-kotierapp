import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import "../components"

// 网络配置页面（对应 Kotlin NetworkConfigPage — 最复杂的页面）
Item {
    id: root
    anchors.fill: parent

    // ── 状态 ──
    property var configs: []
    property int selectedIndex: 0
    property bool isRunning: false
    property bool showAdvanced: false
    property bool isLoading: false
    property bool showServerDialog: false

    // 表单字段
    property string labelText: ""
    property string hostnameText: ""
    property string networkNameText: ""
    property string networkSecretText: ""
    property string ipv4Text: ""
    property string proxyNetworksText: ""
    property string whitelistText: ""
    property string listenAddressesText: ""

    // 节点监控
    property var nodes: []

    function bindConfig(index) {
        if (index < 0 || index >= configs.length) return
        var cfg = configs[index]
        labelText = cfg.networkLabel || ""
        hostnameText = cfg.hostname || ""
        networkNameText = cfg.networkName || ""
        networkSecretText = cfg.networkSecret || ""
        ipv4Text = cfg.ipv4 || ""
        proxyNetworksText = (cfg.proxyNetworks || []).join(", ")
        whitelistText = (cfg.foreignNetworkWhitelist || []).join(", ")
        listenAddressesText = (cfg.listenAddresses || []).join(", ")
        isRunning = cfg.isRunning || false
        showAdvanced = false
    }

    function saveCurrentConfig() {
        if (selectedIndex < 0 || selectedIndex >= configs.length) return
        var cfg = configs[selectedIndex]
        cfg.networkLabel = labelText
        cfg.hostname = hostnameText
        cfg.networkName = networkNameText
        cfg.networkSecret = networkSecretText
        cfg.ipv4 = ipv4Text
        cfg.proxyNetworks = proxyNetworksText.split(",").map(function(s){ return s.trim() }).filter(function(s){ return s.length > 0 })
        cfg.foreignNetworkWhitelist = whitelistText.split(",").map(function(s){ return s.trim() }).filter(function(s){ return s.length > 0 })
        cfg.listenAddresses = listenAddressesText.split(",").map(function(s){ return s.trim() }).filter(function(s){ return s.length > 0 })
        saveConfigs()
    }

    function saveConfigs() {
        var jsonStr = JSON.stringify(configs)
        if (settingsRepo)
            settingsRepo.saveNetworkConfigs(jsonStr)
    }

    function loadSavedConfigs() {
        if (!settingsRepo) return
        var json = settingsRepo.loadNetworkConfigsJson()
        if (!json || json.length === 0) {
            configs = [{ instanceName: "EasyTierET-" + Math.random().toString(36).substr(2,10) }]
        } else {
            configs = JSON.parse(json)
        }
        bindConfig(0)
    }

    function addConfig() {
        saveCurrentConfig()
        var newCfg = { instanceName: "EasyTierET-" + Math.random().toString(36).substr(2,10) }
        configs.push(newCfg)
        selectedIndex = configs.length - 1
        bindConfig(selectedIndex)
        saveConfigs()
    }

    function deleteConfig() {
        if (selectedIndex < 0 || selectedIndex >= configs.length) return
        configs.splice(selectedIndex, 1)
        if (configs.length === 0)
            configs.push({ instanceName: "EasyTierET-" + Math.random().toString(36).substr(2,10) })
        selectedIndex = Math.min(0, configs.length - 1)
        bindConfig(selectedIndex)
        saveConfigs()
    }

    function toggleNetwork() {
        if (selectedIndex < 0 || selectedIndex >= configs.length) return
        saveCurrentConfig()
        var cfg = configs[selectedIndex]

        if (isRunning) {
            // 停止网络
            if (easyTierService) {
                easyTierService.stopVpnService()
                easyTierService.stopNetwork(cfg.instanceName, function(res) {
                    isRunning = false
                    cfg.isRunning = false
                    nodes = []
                    isLoading = false
                })
            }
        } else {
            // 启动网络
            isLoading = true
            if (easyTierService) {
                easyTierService.startNetwork(cfg, function(res) {
                    if (!res.success) {
                        isLoading = false
                        return
                    }
                    // 模拟等待 IP 分配（简化，实际应轮询）
                    var timer = Qt.createQmlObject("import QtQuick; Timer {}", root)
                    timer.interval = 500
                    timer.repeat = true
                    var attempts = 0
                    timer.triggered.connect(function() {
                        attempts++
                        if (attempts >= 5 || isRunning) {
                            timer.stop()
                            timer.destroy()
                        }
                        // 尝试启动 VPN
                        if (easyTierService) {
                            easyTierService.startVpnService(cfg.instanceName,
                                cfg.ipv4 || "10.144.144.1", 24, [])
                            isRunning = true
                            cfg.isRunning = true
                            isLoading = false
                        }
                    })
                    timer.start()
                })
            }
        }
    }

    CompactTopBar {
        id: topBar
        anchors.top: parent.top
        width: parent.width
        title: "网络配置"

        actionRow: [
            Button {
                text: "+"
                flat: true
                anchors.verticalCenter: parent.verticalCenter
                onClicked: addConfig()
            },
            Button {
                text: "🗑"
                flat: true
                anchors.verticalCenter: parent.verticalCenter
                onClicked: deleteConfig()
            },
            Button {
                text: "💾"
                flat: true
                anchors.verticalCenter: parent.verticalCenter
                onClicked: {
                    saveCurrentConfig()
                }
            }
        ]
    }

    Flickable {
        anchors.top: topBar.bottom
        anchors.bottom: parent.bottom
        width: parent.width
        contentHeight: scrollContent.height + 32
        clip: true

        Column {
            id: scrollContent
            width: parent.width - 32
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.top: parent.top
            anchors.topMargin: 16
            spacing: 12

            // ── 配置标签页 ──
            Row {
                width: parent.width
                spacing: 8
                visible: configs.length > 1

                Repeater {
                    model: configs.length

                    Rectangle {
                        width: (parent.width - (configs.length-1)*8) / configs.length
                        height: 32
                        radius: 16
                        color: index === selectedIndex ? Theme.accent : Theme.surfaceVariant

                        Text {
                            anchors.centerIn: parent
                            text: configs[index].networkLabel || ("配置 " + (index+1))
                            font.pixelSize: Theme.fontSizeSmall
                            color: index === selectedIndex ? "#FFFFFF" : Theme.text
                        }

                        MouseArea {
                            anchors.fill: parent
                            onClicked: {
                                saveCurrentConfig()
                                selectedIndex = index
                                bindConfig(index)
                            }
                        }
                    }
                }
            }

            // ── 基本设置 ──
            SectionCard {
                title: "基本设置"
                width: parent.width

                TextField {
                    width: parent.width
                    placeholderText: "配置标签"
                    text: labelText
                    onTextChanged: labelText = text
                    font.pixelSize: Theme.fontSizeBody
                }

                Item { width: parent.width; height: 6 }

                TextField {
                    width: parent.width
                    placeholderText: "本机主机名 (如: my-phone)"
                    text: hostnameText
                    onTextChanged: hostnameText = text
                }

                Item { width: parent.width; height: 6 }

                TextField {
                    width: parent.width
                    placeholderText: "网络名称 (如: my-net)"
                    text: networkNameText
                    onTextChanged: networkNameText = text
                }

                Item { width: parent.width; height: 6 }

                TextField {
                    width: parent.width
                    placeholderText: "网络密钥 (留空自动生成)"
                    text: networkSecretText
                    onTextChanged: networkSecretText = text
                }

                Item { width: parent.width; height: 6 }

                CustomSwitch {
                    width: parent.width
                    label: "DHCP 自动分配 IP"
                    value: selectedIndex < configs.length ? (configs[selectedIndex].dhcp !== false) : true
                    onToggled: {
                        if (selectedIndex < configs.length) {
                            configs[selectedIndex].dhcp = newValue
                        }
                    }
                }

                TextField {
                    width: parent.width
                    placeholderText: "静态 IPv4 (如: 10.144.144.10)"
                    text: ipv4Text
                    onTextChanged: ipv4Text = text
                    visible: selectedIndex < configs.length ? (configs[selectedIndex].dhcp === false) : false
                }
            }

            // ── 入口服务器 ──
            SectionCard {
                title: "入口服务器"
                width: parent.width

                Button {
                    text: "管理"
                    flat: true
                    anchors.right: parent.right
                    anchors.top: parent.top
                    onClicked: {
                        saveCurrentConfig()
                        showServerDialog = true
                    }
                }

                Column {
                    width: parent.width
                    Repeater {
                        model: selectedIndex < configs.length ? (configs[selectedIndex].servers || []) : []

                        Row {
                            width: parent.width
                            spacing: 4

                            Text {
                                text: "• " + modelData
                                font.pixelSize: Theme.fontSizeSmall
                                font.family: "monospace"
                                color: Theme.surfaceVariant
                                elide: Text.ElideRight
                                width: parent.width
                            }
                        }
                    }
                }
            }

            // ── 高级设置标题（可折叠） ──
            Rectangle {
                width: parent.width
                height: 48
                radius: Theme.radiusMedium
                color: Theme.surface

                Row {
                    anchors.fill: parent
                    anchors.leftMargin: 16
                    anchors.rightMargin: 16
                    spacing: 6

                    Text {
                        text: "⚙"
                        font.pixelSize: 18
                        anchors.verticalCenter: parent.verticalCenter
                    }

                    Text {
                        text: "高级设置"
                        font.pixelSize: Theme.fontSizeTitle
                        font.bold: true
                        color: Theme.text
                        anchors.verticalCenter: parent.verticalCenter
                    }

                    Item { width: parent.width - 160; height: 1 }

                    Button {
                        anchors.verticalCenter: parent.verticalCenter
                        text: showAdvanced ? "▲" : "▼"
                        flat: true
                        onClicked: showAdvanced = !showAdvanced
                    }
                }

                MouseArea {
                    anchors.fill: parent
                    onClicked: showAdvanced = !showAdvanced
                }
            }

            // ── 高级设置内容 ──
            Column {
                width: parent.width
                visible: showAdvanced && selectedIndex < configs.length

                SectionCard {
                    title: "协议与传输"
                    width: parent.width

                    // 使用 JS 辅助获取/设置配置属性
                    property var cfg: selectedIndex < configs.length ? configs[selectedIndex] : ({})

                    Repeater {
                        model: [
                            { label: "KCP 代理", key: "enableKcpProxy", hint: "启用 KCP 代理入站", def: true },
                            { label: "禁用 KCP 入站", key: "disableKcpInput", def: false },
                            { label: "QUIC 代理", key: "enableQuicProxy", hint: "启用 QUIC 代理入站", def: false },
                            { label: "禁用 QUIC 入站", key: "disableQuicInput", def: false }
                        ]

                        Item {
                            width: parent.width
                            height: 42

                            Text {
                                anchors.left: parent.left
                                anchors.verticalCenter: parent.verticalCenter
                                text: modelData.label
                                font.pixelSize: Theme.fontSizeBody
                                color: Theme.text
                            }

                            Text {
                                anchors.left: parent.left
                                anchors.top: parent.top
                                anchors.topMargin: 20
                                text: modelData.hint || ""
                                font.pixelSize: Theme.fontSizeSmall
                                color: Theme.surfaceVariant
                                visible: modelData.hint !== undefined
                            }

                            Switch {
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                checked: selectedIndex < configs.length ?
                                    (configs[selectedIndex][modelData.key] !== undefined ?
                                        configs[selectedIndex][modelData.key] : (modelData.def || false)) : false
                                onClicked: {
                                    if (selectedIndex < configs.length) {
                                        configs[selectedIndex][modelData.key] = checked
                                    }
                                }
                            }
                        }
                    }
                }

                SectionCard {
                    title: "网络与连接"
                    width: parent.width

                    Repeater {
                        model: [
                            { label: "禁用 UDP 打孔", key: "disableUdpHolePunching" },
                            { label: "禁用对称 NAT 打孔", key: "disableSymHolePunching" },
                            { label: "禁用 P2P", key: "disableP2p" },
                            { label: "禁用 IPv6", key: "disableIpv6" },
                            { label: "延迟优先", key: "latencyFirst", hint: "优先选择延迟最低的路径" }
                        ]

                        Item {
                            width: parent.width
                            height: modelData.hint ? 42 : 36

                            Text {
                                anchors.left: parent.left
                                anchors.verticalCenter: parent.verticalCenter
                                text: modelData.label
                                font.pixelSize: Theme.fontSizeBody
                                color: Theme.text
                            }

                            Text {
                                anchors.left: parent.left
                                anchors.top: parent.top
                                anchors.topMargin: 20
                                text: modelData.hint || ""
                                font.pixelSize: Theme.fontSizeSmall
                                color: Theme.surfaceVariant
                                visible: modelData.hint !== undefined
                            }

                            Switch {
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                checked: selectedIndex < configs.length ?
                                    (configs[selectedIndex][modelData.key] || false) : false
                                onClicked: {
                                    if (selectedIndex < configs.length)
                                        configs[selectedIndex][modelData.key] = checked
                                }
                            }
                        }
                    }
                }

                SectionCard {
                    title: "高级选项"
                    width: parent.width

                    Repeater {
                        model: [
                            { label: "加密", key: "enableEncryption", hint: "启用网络加密", def: true },
                            { label: "出口节点", key: "enableExitNode", hint: "将本机作为网络出口" },
                            { label: "系统转发", key: "systemForwarding", hint: "启用系统 IP 转发" },
                            { label: "多线程", key: "multiThread", def: true },
                            { label: "Smoltcp 协议栈", key: "useSmoltcp" },
                            { label: "绑定设备", key: "bindDevice", def: true },
                            { label: "私有模式", key: "privateMode", hint: "仅允许白名单节点加入", def: true },
                            { label: "中转所有 RPC", key: "relayAllPeerRpc" },
                            { label: "接受 DNS", key: "acceptDns" },
                            { label: "禁用 TUN", key: "noTun" }
                        ]

                        Item {
                            width: parent.width
                            height: modelData.hint ? 42 : 36

                            Text {
                                anchors.left: parent.left
                                anchors.verticalCenter: parent.verticalCenter
                                text: modelData.label
                                font.pixelSize: Theme.fontSizeBody
                                color: Theme.text
                            }
                            Text {
                                anchors.left: parent.left
                                anchors.top: parent.top
                                anchors.topMargin: 20
                                text: modelData.hint || ""
                                font.pixelSize: Theme.fontSizeSmall
                                color: Theme.surfaceVariant
                                visible: modelData.hint !== undefined
                            }
                            Switch {
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                checked: selectedIndex < configs.length ?
                                    (configs[selectedIndex][modelData.key] !== undefined ?
                                        configs[selectedIndex][modelData.key] : (modelData.def || false)) : false
                                onClicked: {
                                    if (selectedIndex < configs.length)
                                        configs[selectedIndex][modelData.key] = checked
                                }
                            }
                        }
                    }
                }

                SectionCard {
                    title: "白名单"
                    width: parent.width

                    TextField {
                        width: parent.width
                        placeholderText: "监听地址（逗号分隔）"
                        text: listenAddressesText
                        onTextChanged: listenAddressesText = text
                        label: "tcp://0.0.0.0:11010, udp://0.0.0.0:11010"
                    }

                    Item { width: parent.width; height: 6 }

                    TextField {
                        width: parent.width
                        placeholderText: "代理网络 CIDR（逗号分隔）如: 192.168.1.0/24"
                        text: proxyNetworksText
                        onTextChanged: proxyNetworksText = text
                    }

                    Item { width: parent.width; height: 6 }

                    CustomSwitch {
                        width: parent.width
                        label: "启用外部网络白名单"
                        value: selectedIndex < configs.length ? (configs[selectedIndex].foreignNetworkWhitelistEnabled || false) : false
                        onToggled: {
                            if (selectedIndex < configs.length)
                                configs[selectedIndex].foreignNetworkWhitelistEnabled = newValue
                        }
                    }

                    TextField {
                        width: parent.width
                        placeholderText: "外部网络白名单（逗号分隔）"
                        text: whitelistText
                        onTextChanged: whitelistText = text
                        visible: selectedIndex < configs.length ? (configs[selectedIndex].foreignNetworkWhitelistEnabled || false) : false
                    }
                }
            }

            // ── 操作按钮 ──
            Row {
                width: parent.width
                spacing: 12

                Button {
                    width: (parent.width - 12) / 2
                    height: 50
                    text: isLoading ? "处理中..." : (isRunning ? "停止网络" : "启动网络")
                    highlighted: !isRunning
                    enabled: !isLoading
                    onClicked: toggleNetwork()
                }

                Button {
                    width: (parent.width - 12) / 2
                    height: 50
                    text: "一键联机"
                    flat: true
                    onClicked: {
                        if (mainController)
                            mainController.selectedTabIndex = 1
                    }
                }
            }

            // ── 节点监控 ──
            Column {
                width: parent.width
                visible: isRunning

                SectionCard {
                    title: "节点监测"
                    width: parent.width

                    Row {
                        width: parent.width
                        Text {
                            text: "实时"
                            font.pixelSize: Theme.fontSizeSmall
                            color: Theme.surfaceVariant
                            anchors.right: parent.right
                        }
                    }

                    Item { width: parent.width; height: 8 }

                    Repeater {
                        model: nodes.length > 0 ? nodes : [{ empty: true }]

                        Loader {
                            width: parent.width
                            sourceComponent: modelData.empty ? emptyComponent : nodeCardComponent
                        }
                    }
                }
            }

            Item { width: parent.width; height: 16 }
        }
    }

    Component {
        id: nodeCardComponent
        NodeInfoCard {
            hostname: modelData.hostname
            virtualIp: modelData.virtualIp
            latencyMs: modelData.latencyMs || 0
            protocol: modelData.protocol || ""
            connectionType: modelData.connectionType || "unknown"
            isLocal: modelData.isLocal || false
            trafficText: modelData.trafficText || ""
        }
    }

    Component {
        id: emptyComponent
        Rectangle {
            height: 48
            color: Theme.surface
            radius: Theme.radiusSmall
            Text {
                anchors.centerIn: parent
                text: "等待节点数据..."
                color: Theme.surfaceVariant
                font.pixelSize: Theme.fontSizeBody
            }
        }
    }

    // ── 服务器管理对话框 ──
    Dialog {
        id: serverDialog
        title: "管理入口服务器"
        standardButtons: Dialog.Ok | Dialog.Cancel
        modal: true
        visible: showServerDialog

        property var serverList: selectedIndex < configs.length ?
            (configs[selectedIndex].servers || []).slice() : []
        property string newUrl: ""

        Column {
            width: 280
            spacing: 8

            Row {
                spacing: 6
                TextField {
                    id: newServerField
                    width: 200
                    placeholderText: "wss://example.com"
                }
                Button {
                    text: "添加"
                    onClicked: {
                        if (newServerField.text.trim().length > 0) {
                            serverDialog.serverList.push(newServerField.text.trim())
                            newServerField.text = ""
                        }
                    }
                }
            }

            Repeater {
                model: serverDialog.serverList

                Row {
                    width: parent.width
                    spacing: 6

                    Text {
                        text: modelData
                        font.family: "monospace"
                        font.pixelSize: Theme.fontSizeSmall
                        color: Theme.text
                        elide: Text.ElideRight
                        width: parent.width - 40
                    }

                    Button {
                        text: "✕"
                        flat: true
                        width: 30
                        height: 30
                        onClicked: {
                            serverDialog.serverList.splice(index, 1)
                        }
                    }
                }
            }

            Text {
                text: "暂无服务器"
                color: Theme.surfaceVariant
                visible: serverDialog.serverList.length === 0
            }
        }

        onAccepted: {
            if (selectedIndex < configs.length) {
                configs[selectedIndex].servers = serverDialog.serverList
                saveConfigs()
            }
            showServerDialog = false
        }
        onRejected: {
            showServerDialog = false
        }
    }

    Component.onCompleted: {
        loadSavedConfigs()
    }
}
