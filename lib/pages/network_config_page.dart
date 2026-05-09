import 'dart:async';
import 'package:flutter/material.dart';
import '../core/config/network_config.dart';
import '../core/config/node_info.dart';
import '../core/easy_tier_service.dart';
import '../core/log_service.dart';
import '../widgets/custom_switch.dart';
import '../widgets/node_info_card.dart';
import 'one_click_page.dart';

class NetworkConfigPage extends StatefulWidget {
  const NetworkConfigPage({super.key});

  @override
  State<NetworkConfigPage> createState() => _NetworkConfigPageState();
}

class _NetworkConfigPageState extends State<NetworkConfigPage> {
  final List<NetworkConfig> _configs = [];
  int _selectedIndex = -1;
  bool _isRunning = false;
  List<NodeInfo> _nodes = [];
  bool _showAdvanced = false;
  bool _isLoading = false;
  Timer? _monitorTimer;

  late TextEditingController _hostnameCtrl;
  late TextEditingController _networkNameCtrl;
  late TextEditingController _networkSecretCtrl;
  late TextEditingController _ipv4Ctrl;
  late TextEditingController _labelCtrl;
  late TextEditingController _proxyNetworksCtrl;
  late TextEditingController _whitelistCtrl;
  late TextEditingController _listenAddressesCtrl;

  @override
  void initState() {
    super.initState();
    _hostnameCtrl = TextEditingController();
    _networkNameCtrl = TextEditingController();
    _networkSecretCtrl = TextEditingController();
    _ipv4Ctrl = TextEditingController();
    _labelCtrl = TextEditingController();
    _proxyNetworksCtrl = TextEditingController();
    _whitelistCtrl = TextEditingController();
    _listenAddressesCtrl = TextEditingController();
    _loadConfigs();
  }

  @override
  void dispose() {
    _monitorTimer?.cancel();
    _hostnameCtrl.dispose();
    _networkNameCtrl.dispose();
    _networkSecretCtrl.dispose();
    _ipv4Ctrl.dispose();
    _labelCtrl.dispose();
    _proxyNetworksCtrl.dispose();
    _whitelistCtrl.dispose();
    _listenAddressesCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadConfigs() async {
    final configs = await EasyTierServiceFactory.instance.loadConfigs();
    if (!mounted) return;
    setState(() {
      _configs.clear();
      _configs.addAll(configs);
      if (_configs.isEmpty) {
        _addConfig();
      } else if (_selectedIndex < 0) {
        _selectedIndex = 0;
        _bindConfig(0);
      } else if (_selectedIndex < _configs.length) {
        _bindConfig(_selectedIndex);
      }
    });
  }

  void _bindConfig(int index) {
    if (index < 0 || index >= _configs.length) return;
    final cfg = _configs[index];
    _labelCtrl.text = cfg.networkLabel;
    _hostnameCtrl.text = cfg.hostname;
    _networkNameCtrl.text = cfg.networkName;
    _networkSecretCtrl.text = cfg.networkSecret;
    _ipv4Ctrl.text = cfg.ipv4;
    _proxyNetworksCtrl.text = cfg.proxyNetworks.join(', ');
    _whitelistCtrl.text = cfg.foreignNetworkWhitelist.join(', ');
    _listenAddressesCtrl.text = cfg.listenAddresses.join(', ');
    _isRunning = cfg.isRunning;
    _showAdvanced = false;
  }

  void _saveCurrentConfig() {
    if (_selectedIndex < 0 || _selectedIndex >= _configs.length) return;
    final cfg = _configs[_selectedIndex];
    cfg.networkLabel = _labelCtrl.text;
    cfg.hostname = _hostnameCtrl.text;
    cfg.networkName = _networkNameCtrl.text;
    cfg.networkSecret = _networkSecretCtrl.text;
    cfg.ipv4 = _ipv4Ctrl.text;
    cfg.proxyNetworks = _proxyNetworksCtrl.text
        .split(',')
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();
    cfg.foreignNetworkWhitelist = _whitelistCtrl.text
        .split(',')
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();
    cfg.listenAddresses = _listenAddressesCtrl.text
        .split(',')
        .map((e) => e.trim())
        .where((e) => e.isNotEmpty)
        .toList();
  }

  void _addConfig() {
    final cfg = NetworkConfig();
    LogService.instance.info('新建网络配置: ${cfg.instanceName}',
        source: 'NetworkConfig');
    setState(() {
      _configs.add(cfg);
      _selectedIndex = _configs.length - 1;
      _bindConfig(_selectedIndex);
    });
  }

  Future<void> _deleteConfig() async {
    if (_selectedIndex < 0) return;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('删除配置'),
        content: const Text('确定要删除这个网络配置吗？'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('取消')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red, foregroundColor: Colors.white),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirm == true && _selectedIndex < _configs.length) {
      LogService.instance.info(
          '删除网络配置: ${_configs[_selectedIndex].instanceName}',
          source: 'NetworkConfig');
      setState(() {
        _configs.removeAt(_selectedIndex);
        _selectedIndex = _configs.isEmpty ? -1 : 0;
        if (_selectedIndex >= 0) _bindConfig(_selectedIndex);
      });
    }
  }

  Future<void> _toggleNetwork() async {
    if (_selectedIndex < 0) return;
    _saveCurrentConfig();
    final cfg = _configs[_selectedIndex];
    final service = EasyTierServiceFactory.instance;

    setState(() => _isLoading = true);

    if (_isRunning) {
      final result = await service.stopNetwork(cfg.instanceName);
      if (!mounted) return;
      if (result.success) {
        LogService.instance.info(
            '网络已停止: ${cfg.networkName} (${cfg.instanceName})',
            source: 'NetworkConfig');
        setState(() {
          _isRunning = false;
          cfg.isRunning = false;
          _nodes = [];
        });
        _monitorTimer?.cancel();
        _monitorTimer = null;
      } else {
        LogService.instance.error(
            '停止网络失败: ${result.errorMessage}',
            source: 'NetworkConfig');
        _showSnackBar('停止失败: ${result.errorMessage}');
      }
    } else {
      final result = await service.startNetwork(cfg);
      if (!mounted) return;
      if (result.success) {
        LogService.instance.info(
            '网络已启动: ${cfg.networkName} (${cfg.instanceName})',
            source: 'NetworkConfig');
        setState(() {
          _isRunning = true;
          cfg.isRunning = true;
        });
        _startMonitoring(cfg.instanceName);
      } else {
        _showSnackBar('启动失败: ${result.errorMessage}');
      }
    }

    setState(() => _isLoading = false);
  }

  void _startMonitoring(String instanceName) {
    _monitorTimer?.cancel();
    _collectNodes(instanceName);
    _monitorTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      _collectNodes(instanceName);
    });
  }

  Future<void> _collectNodes(String instanceName) async {
    final nodes =
        await EasyTierServiceFactory.instance.collectNodeInfos(instanceName);
    if (!mounted) return;
    setState(() => _nodes = nodes);
  }

  Future<void> _saveConfigs() async {
    _saveCurrentConfig();
    await EasyTierServiceFactory.instance.saveConfigs(_configs);
    if (!mounted) return;
    LogService.instance.info(
        '配置已保存 (${_configs.length} 个)', source: 'NetworkConfig');
    _showSnackBar('配置已保存');
  }

  void _showSnackBar(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), behavior: SnackBarBehavior.floating),
    );
  }

  void _manageServers() async {
    if (_selectedIndex < 0) return;
    _saveCurrentConfig();
    final cfg = _configs[_selectedIndex];
    final result = await showDialog<List<String>>(
      context: context,
      builder: (ctx) => _ServerListEditor(servers: List.from(cfg.servers)),
    );
    if (result != null) {
      setState(() => cfg.servers = result);
    }
  }

  void _addProxyNetwork() {
    if (_selectedIndex < 0) return;
    final ctrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('添加代理网络'),
        content: TextField(
          controller: ctrl,
          decoration: const InputDecoration(
            labelText: 'CIDR',
            hintText: '例如 192.168.1.0/24',
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('取消')),
          ElevatedButton(
            onPressed: () {
              if (ctrl.text.isNotEmpty) {
                _configs[_selectedIndex].proxyNetworks.add(ctrl.text.trim());
                _proxyNetworksCtrl.text =
                    _configs[_selectedIndex].proxyNetworks.join(', ');
                Navigator.pop(ctx);
              }
            },
            child: const Text('添加'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('网络配置'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: '新建配置',
            onPressed: _addConfig,
          ),
          if (_selectedIndex >= 0)
            IconButton(
              icon: const Icon(Icons.delete_outline),
              tooltip: '删除配置',
              onPressed: _deleteConfig,
            ),
          IconButton(
            icon: const Icon(Icons.save_outlined),
            tooltip: '保存配置',
            onPressed: _saveConfigs,
          ),
        ],
      ),
      body: _selectedIndex < 0
          ? const Center(child: Text('暂无配置，点击右上角 + 新建'))
          : _buildBody(),
    );
  }

  Widget _buildBody() {
    return Column(
      children: [
        _buildConfigTabs(),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            children: [
              _buildBasicSettings(),
              const SizedBox(height: 8),
              _buildServerSection(),
              const SizedBox(height: 8),
              _buildAdvancedToggle(),
              if (_showAdvanced) ...[
                const SizedBox(height: 4),
                _buildAdvancedSettings(),
              ],
              const SizedBox(height: 16),
              _buildActionButtons(),
              const SizedBox(height: 16),
              if (_isRunning) _buildNodeMonitor(),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildConfigTabs() {
    if (_configs.length <= 1) return const SizedBox.shrink();
    return Container(
      height: 48,
      margin: const EdgeInsets.only(top: 4),
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        itemCount: _configs.length,
        itemBuilder: (context, index) {
          final isSelected = index == _selectedIndex;
          final label = _configs[index].networkLabel.isNotEmpty
              ? _configs[index].networkLabel
              : '配置 ${index + 1}';
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: Text(label),
              selected: isSelected,
              onSelected: (selected) {
                if (selected) {
                  _saveCurrentConfig();
                  setState(() {
                    _selectedIndex = index;
                    _bindConfig(index);
                  });
                }
              },
            ),
          );
        },
      ),
    );
  }

  Widget _buildBasicSettings() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.settings, size: 18,
                    color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 8),
                Text('基本设置',
                    style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: Theme.of(context).textTheme.bodyLarge?.color)),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _labelCtrl,
              decoration: const InputDecoration(
                  labelText: '配置标签', hintText: '例如: 家庭网络'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _hostnameCtrl,
              decoration: const InputDecoration(
                  labelText: '本机主机名', hintText: '例如: my-phone'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _networkNameCtrl,
              decoration: const InputDecoration(
                  labelText: '网络名称', hintText: '例如: my-net'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _networkSecretCtrl,
              decoration: const InputDecoration(
                  labelText: '网络密钥', hintText: '留空自动生成'),
              obscureText: true,
            ),
            const SizedBox(height: 10),
            CustomSwitch(
              label: 'DHCP 自动分配 IP',
              value: _selectedIndex >= 0 && _configs[_selectedIndex].dhcp,
              onChanged: (val) {
                if (_selectedIndex >= 0) {
                  setState(() => _configs[_selectedIndex].dhcp = val);
                }
              },
            ),
            if (_selectedIndex >= 0 && !_configs[_selectedIndex].dhcp)
              Padding(
                padding: const EdgeInsets.only(top: 10),
                child: TextField(
                  controller: _ipv4Ctrl,
                  decoration: const InputDecoration(
                      labelText: '静态 IPv4', hintText: '例如: 10.144.144.10'),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildServerSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.dns, size: 18,
                    color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 8),
                Text('入口服务器',
                    style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: Theme.of(context).textTheme.bodyLarge?.color)),
                const Spacer(),
                TextButton.icon(
                  icon: const Icon(Icons.edit, size: 16),
                  label: const Text('管理'),
                  onPressed: _manageServers,
                ),
              ],
            ),
            if (_selectedIndex >= 0) ...[
              const SizedBox(height: 4),
              ..._configs[_selectedIndex].servers.map(
                    (s) => Padding(
                      padding: const EdgeInsets.symmetric(vertical: 2),
                      child: Row(
                        children: [
                          Icon(Icons.link, size: 14,
                              color: Theme.of(context)
                                  .textTheme
                                  .bodySmall
                                  ?.color),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(s,
                                style: TextStyle(
                                    fontSize: 12,
                                    fontFamily: 'monospace',
                                    color: Theme.of(context)
                                        .textTheme
                                        .bodySmall
                                        ?.color),
                                overflow: TextOverflow.ellipsis),
                          ),
                        ],
                      ),
                    ),
                  ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildAdvancedToggle() {
    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: () => setState(() => _showAdvanced = !_showAdvanced),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Row(
            children: [
              Icon(Icons.tune, size: 18,
                  color: Theme.of(context).colorScheme.primary),
              const SizedBox(width: 8),
              Text('高级设置',
                  style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: Theme.of(context).textTheme.bodyLarge?.color)),
              const Spacer(),
              AnimatedRotation(
                turns: _showAdvanced ? 0.5 : 0,
                duration: const Duration(milliseconds: 200),
                child: Icon(Icons.expand_more,
                    color: Theme.of(context).textTheme.bodySmall?.color),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAdvancedSettings() {
    if (_selectedIndex < 0) return const SizedBox.shrink();
    final cfg = _configs[_selectedIndex];

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('协议与传输',
                style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Theme.of(context).textTheme.bodySmall?.color)),
            const SizedBox(height: 4),
            CustomSwitch(
              label: 'KCP 代理',
              hint: '启用 KCP 代理入站',
              value: cfg.enableKcpProxy,
              onChanged: (v) => setState(() => cfg.enableKcpProxy = v),
            ),
            CustomSwitch(
              label: '禁用 KCP 入站',
              value: cfg.disableKcpInput,
              onChanged: (v) => setState(() => cfg.disableKcpInput = v),
            ),
            CustomSwitch(
              label: 'QUIC 代理',
              hint: '启用 QUIC 代理入站',
              value: cfg.enableQuicProxy,
              onChanged: (v) => setState(() => cfg.enableQuicProxy = v),
            ),
            CustomSwitch(
              label: '禁用 QUIC 入站',
              value: cfg.disableQuicInput,
              onChanged: (v) => setState(() => cfg.disableQuicInput = v),
            ),
            const Divider(height: 24),
            Text('网络与连接',
                style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Theme.of(context).textTheme.bodySmall?.color)),
            const SizedBox(height: 4),
            CustomSwitch(
              label: '禁用 UDP 打孔',
              value: cfg.disableUdpHolePunching,
              onChanged: (v) => setState(() => cfg.disableUdpHolePunching = v),
            ),
            CustomSwitch(
              label: '禁用对称 NAT 打孔',
              value: cfg.disableSymHolePunching,
              onChanged: (v) => setState(() => cfg.disableSymHolePunching = v),
            ),
            CustomSwitch(
              label: '禁用 P2P',
              value: cfg.disableP2p,
              onChanged: (v) => setState(() => cfg.disableP2p = v),
            ),
            CustomSwitch(
              label: '禁用 IPv6',
              value: cfg.disableIpv6,
              onChanged: (v) => setState(() => cfg.disableIpv6 = v),
            ),
            CustomSwitch(
              label: '延迟优先',
              hint: '优先选择延迟最低的路径',
              value: cfg.latencyFirst,
              onChanged: (v) => setState(() => cfg.latencyFirst = v),
            ),
            const Divider(height: 24),
            Text('高级选项',
                style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Theme.of(context).textTheme.bodySmall?.color)),
            const SizedBox(height: 4),
            CustomSwitch(
              label: '加密',
              hint: '启用网络加密',
              value: cfg.enableEncryption,
              onChanged: (v) => setState(() => cfg.enableEncryption = v),
            ),
            CustomSwitch(
              label: '出口节点',
              hint: '将本机作为网络出口',
              value: cfg.enableExitNode,
              onChanged: (v) => setState(() => cfg.enableExitNode = v),
            ),
            CustomSwitch(
              label: '系统转发',
              hint: '启用系统 IP 转发',
              value: cfg.systemForwarding,
              onChanged: (v) => setState(() => cfg.systemForwarding = v),
            ),
            CustomSwitch(
              label: '多线程',
              value: cfg.multiThread,
              onChanged: (v) => setState(() => cfg.multiThread = v),
            ),
            CustomSwitch(
              label: 'Smoltcp 协议栈',
              value: cfg.useSmoltcp,
              onChanged: (v) => setState(() => cfg.useSmoltcp = v),
            ),
            CustomSwitch(
              label: '绑定设备',
              value: cfg.bindDevice,
              onChanged: (v) => setState(() => cfg.bindDevice = v),
            ),
            CustomSwitch(
              label: '私有模式',
              hint: '仅允许白名单节点加入',
              value: cfg.privateMode,
              onChanged: (v) => setState(() => cfg.privateMode = v),
            ),
            CustomSwitch(
              label: '中转所有 RPC',
              value: cfg.relayAllPeerRpc,
              onChanged: (v) => setState(() => cfg.relayAllPeerRpc = v),
            ),
            CustomSwitch(
              label: '接受 DNS',
              value: cfg.acceptDns,
              onChanged: (v) => setState(() => cfg.acceptDns = v),
            ),
            CustomSwitch(
              label: '禁用 TUN',
              value: cfg.noTun,
              onChanged: (v) => setState(() => cfg.noTun = v),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _listenAddressesCtrl,
              decoration: const InputDecoration(
                labelText: '监听地址（逗号分隔）',
                hintText: 'tcp://0.0.0.0:11010, udp://0.0.0.0:11010',
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _proxyNetworksCtrl,
                    decoration: const InputDecoration(
                      labelText: '代理网络 CIDR（逗号分隔）',
                      hintText: '192.168.1.0/24',
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  icon: const Icon(Icons.add_circle_outline),
                  tooltip: '添加代理网络',
                  onPressed: _addProxyNetwork,
                ),
              ],
            ),
            const SizedBox(height: 10),
            CustomSwitch(
              label: '启用外部网络白名单',
              value: cfg.foreignNetworkWhitelistEnabled,
              onChanged: (v) =>
                  setState(() => cfg.foreignNetworkWhitelistEnabled = v),
            ),
            if (cfg.foreignNetworkWhitelistEnabled)
              Padding(
                padding: const EdgeInsets.only(top: 10),
                child: TextField(
                  controller: _whitelistCtrl,
                  decoration: const InputDecoration(
                    labelText: '外部网络白名单（逗号分隔）',
                    hintText: 'network1, network2',
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      children: [
        if (!_isRunning)
          Expanded(
            child: ElevatedButton.icon(
              icon: const Icon(Icons.play_arrow),
              label: Text(_isLoading ? '启动中...' : '启动网络'),
              onPressed: _isLoading ? null : _toggleNetwork,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF4CAF50),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
            ),
          )
        else
          Expanded(
            child: ElevatedButton.icon(
              icon: const Icon(Icons.stop),
              label: Text(_isLoading ? '停止中...' : '停止网络'),
              onPressed: _isLoading ? null : _toggleNetwork,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.redAccent,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
            ),
          ),
        const SizedBox(width: 12),
        Expanded(
          child: OutlinedButton.icon(
            icon: const Icon(Icons.flash_on),
            label: const Text('一键联机'),
            onPressed: () {
              _saveCurrentConfig();
              Navigator.of(context).push(MaterialPageRoute(
                builder: (_) => const OneClickPage(),
              ));
            },
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildNodeMonitor() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(Icons.monitor_heart, size: 18,
                color: Theme.of(context).colorScheme.primary),
            const SizedBox(width: 8),
            Text('节点监测',
                style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: Theme.of(context).textTheme.bodyLarge?.color)),
            const Spacer(),
            SizedBox(
              width: 14,
              height: 14,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
            const SizedBox(width: 6),
            Text('实时',
                style: TextStyle(
                    fontSize: 12,
                    color: Theme.of(context).textTheme.bodySmall?.color)),
          ],
        ),
        const SizedBox(height: 10),
        if (_nodes.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Center(
                child: Text('等待节点数据...',
                    style: TextStyle(
                        color: Theme.of(context).textTheme.bodySmall?.color)),
              ),
            ),
          )
        else
          ..._nodes.map((node) => NodeInfoCard(node: node)),
      ],
    );
  }
}

class _ServerListEditor extends StatefulWidget {
  final List<String> servers;

  const _ServerListEditor({required this.servers});

  @override
  State<_ServerListEditor> createState() => _ServerListEditorState();
}

class _ServerListEditorState extends State<_ServerListEditor> {
  late List<String> _servers;
  final _urlCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _servers = List.from(widget.servers);
  }

  @override
  void dispose() {
    _urlCtrl.dispose();
    super.dispose();
  }

  void _addServer() {
    final url = _urlCtrl.text.trim();
    if (url.isEmpty) return;
    setState(() {
      _servers.add(url);
      _urlCtrl.clear();
    });
  }

  void _removeServer(int index) {
    setState(() => _servers.removeAt(index));
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('管理入口服务器'),
      content: SizedBox(
        width: 400,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: 42,
                    child: TextField(
                      controller: _urlCtrl,
                      decoration: const InputDecoration(
                        hintText: 'wss://example.com',
                        contentPadding:
                            EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        isDense: true,
                      ),
                      onSubmitted: (_) => _addServer(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _addServer,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    minimumSize: const Size(0, 42),
                  ),
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (_servers.isEmpty)
              const Padding(
                padding: EdgeInsets.all(16),
                child: Text('暂无服务器'),
              )
            else
              Flexible(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: _servers.length,
                  itemBuilder: (context, index) {
                    return ListTile(
                      dense: true,
                      title: Text(_servers[index],
                          style: const TextStyle(fontSize: 13),
                          overflow: TextOverflow.ellipsis),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete_outline, size: 18),
                        onPressed: () => _removeServer(index),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消')),
        ElevatedButton(
          onPressed: () => Navigator.pop(context, _servers),
          child: const Text('确定'),
        ),
      ],
    );
  }
}
