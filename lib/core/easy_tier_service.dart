import 'config/network_config.dart';
import 'config/node_info.dart';
import 'log_service.dart';

abstract class EasyTierService {
  String get platform;

  Future<bool> initialize();

  Future<bool> parseConfig(String tomlConfig);

  Future<EasyTierResult> startNetwork(NetworkConfig config);

  Future<EasyTierResult> stopNetwork(String instanceName);

  Future<List<NodeInfo>> collectNodeInfos(String instanceName);

  Future<bool> stopAllNetworks();

  Future<List<NetworkConfig>> loadConfigs();

  Future<bool> saveConfigs(List<NetworkConfig> configs);

  void dispose();
}

class EasyTierResult {
  final bool success;
  final String errorMessage;

  EasyTierResult({required this.success, this.errorMessage = ''});

  factory EasyTierResult.ok() => EasyTierResult(success: true);

  factory EasyTierResult.fail(String msg) =>
      EasyTierResult(success: false, errorMessage: msg);
}

class EasyTierServiceFactory {
  static EasyTierService _instance = _createDefault();

  static EasyTierService get instance => _instance;

  static void setInstance(EasyTierService service) {
    _instance = service;
  }

  static EasyTierService _createDefault() {
    return WebEasyTierService();
  }
}

class WebEasyTierService implements EasyTierService {
  final List<NetworkConfig> _networks = [];
  final Map<String, bool> _runningInstances = {};
  bool _initialized = false;

  @override
  String get platform => 'web';

  @override
  Future<bool> initialize() async {
    _initialized = true;
    LogService.instance.info('Web 服务初始化完成', source: 'EasyTier');
    return true;
  }

  @override
  Future<bool> parseConfig(String tomlConfig) async {
    final ok = tomlConfig.isNotEmpty;
    LogService.instance.info(
        '解析配置: ${ok ? "成功" : "失败（配置为空）"}',
        source: 'EasyTier');
    return ok;
  }

  @override
  Future<EasyTierResult> startNetwork(NetworkConfig config) async {
    await Future.delayed(const Duration(milliseconds: 800));
    config.isRunning = true;
    _runningInstances[config.instanceName] = true;

    final idx = _networks.indexWhere(
        (n) => n.instanceName == config.instanceName);
    if (idx >= 0) {
      _networks[idx] = config;
    } else {
      _networks.add(config);
    }

    LogService.instance.info(
        '启动网络: ${config.networkName} (${config.instanceName})',
        source: 'EasyTier');

    return EasyTierResult.ok();
  }

  @override
  Future<EasyTierResult> stopNetwork(String instanceName) async {
    await Future.delayed(const Duration(milliseconds: 500));
    _runningInstances.remove(instanceName);

    final idx = _networks.indexWhere((n) => n.instanceName == instanceName);
    if (idx >= 0) {
      _networks[idx].isRunning = false;
    }

    LogService.instance.info('停止网络: $instanceName', source: 'EasyTier');

    return EasyTierResult.ok();
  }

  @override
  Future<List<NodeInfo>> collectNodeInfos(String instanceName) async {
    await Future.delayed(const Duration(milliseconds: 300));
    LogService.instance.debug(
        '收集节点信息: $instanceName', source: 'EasyTier');

    return [
      NodeInfo(
        hostname: '${instanceName}-local',
        virtualIp: '10.144.144.1',
        latencyMs: 0,
        protocol: 'udp',
        connectionType: 'direct',
        isLocal: true,
        rxBytes: 1024 * 50,
        txBytes: 1024 * 30,
      ),
      NodeInfo(
        hostname: 'remote-node-1',
        virtualIp: '10.144.144.2',
        latencyMs: 23,
        protocol: 'udp',
        connectionType: 'relay',
        isLocal: false,
        rxBytes: 1024 * 20,
        txBytes: 1024 * 10,
        lossRate: 0.01,
      ),
      NodeInfo(
        hostname: 'remote-node-2',
        virtualIp: '10.144.144.3',
        latencyMs: 45,
        protocol: 'wss',
        connectionType: 'server',
        isLocal: false,
        rxBytes: 1024 * 100,
        txBytes: 1024 * 80,
        lossRate: 0.03,
      ),
    ];
  }

  @override
  Future<bool> stopAllNetworks() async {
    _runningInstances.clear();
    for (final net in _networks) {
      net.isRunning = false;
    }
    LogService.instance.info('已停止所有网络', source: 'EasyTier');
    return true;
  }

  @override
  Future<List<NetworkConfig>> loadConfigs() async {
    LogService.instance.debug(
        '加载配置: ${_networks.length} 个', source: 'EasyTier');
    return List.from(_networks);
  }

  @override
  Future<bool> saveConfigs(List<NetworkConfig> configs) async {
    _networks
      ..clear()
      ..addAll(configs);
    LogService.instance.info(
        '保存配置: ${configs.length} 个', source: 'EasyTier');
    return true;
  }

  @override
  void dispose() {
    LogService.instance.info('服务已释放', source: 'EasyTier');
  }
}
