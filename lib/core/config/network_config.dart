class NetworkConfig {
  String instanceName;
  String networkLabel;
  bool isRunning;

  String hostname;
  String networkName;
  String networkSecret;
  bool dhcp;
  String ipv4;
  bool latencyFirst;
  bool privateMode;
  List<String> servers;

  bool enableKcpProxy;
  bool disableKcpInput;
  bool noTun;
  bool enableQuicProxy;
  bool disableQuicInput;
  bool disableUdpHolePunching;
  bool multiThread;
  bool useSmoltcp;
  bool bindDevice;
  bool disableP2p;
  bool enableExitNode;
  bool systemForwarding;
  bool disableSymHolePunching;
  bool disableIpv6;
  bool relayAllPeerRpc;
  bool enableEncryption;
  bool acceptDns;

  bool foreignNetworkWhitelistEnabled;
  List<String> foreignNetworkWhitelist;
  List<String> listenAddresses;
  List<String> proxyNetworks;

  NetworkConfig({
    String? instanceName,
    this.networkLabel = '',
    this.isRunning = false,
    this.hostname = '',
    this.networkName = '',
    this.networkSecret = '',
    this.dhcp = true,
    this.ipv4 = '',
    this.latencyFirst = false,
    this.privateMode = true,
    List<String>? servers,
    this.enableKcpProxy = true,
    this.disableKcpInput = false,
    this.noTun = false,
    this.enableQuicProxy = false,
    this.disableQuicInput = false,
    this.disableUdpHolePunching = false,
    this.multiThread = true,
    this.useSmoltcp = false,
    this.bindDevice = true,
    this.disableP2p = false,
    this.enableExitNode = false,
    this.systemForwarding = false,
    this.disableSymHolePunching = false,
    this.disableIpv6 = false,
    this.relayAllPeerRpc = false,
    this.enableEncryption = true,
    this.acceptDns = false,
    this.foreignNetworkWhitelistEnabled = false,
    List<String>? foreignNetworkWhitelist,
    List<String>? listenAddresses,
    List<String>? proxyNetworks,
  })  : instanceName = instanceName ?? _generateInstanceName(),
        servers = servers ?? ['wss://qtet-public.070219.xyz'],
        foreignNetworkWhitelist = foreignNetworkWhitelist ?? [],
        listenAddresses =
            listenAddresses ?? ['tcp://0.0.0.0:11010', 'udp://0.0.0.0:11010'],
        proxyNetworks = proxyNetworks ?? [];

  static String _generateInstanceName() {
    const prefix = 'FlutterET-';
    final random = DateTime.now().microsecondsSinceEpoch;
    const chars = '0123456789abcdefghijklmnopqrstuvwxyz';
    final suffix = List.generate(10, (_) => chars[random % chars.length]).join();
    return '$prefix$suffix';
  }

  String toToml() {
    final buf = StringBuffer();

    buf.writeln('instance_name = "$instanceName"');
    buf.writeln('hostname = "$hostname"');
    buf.writeln('dhcp = ${dhcp ? "true" : "false"}');
    if (ipv4.isNotEmpty && !dhcp) {
      buf.writeln('ipv4 = "$ipv4"');
    }

    if (listenAddresses.isNotEmpty) {
      buf.writeln();
      buf.writeln('listeners = [');
      for (final addr in listenAddresses) {
        buf.writeln('"$addr",');
      }
      buf.writeln(']');
    }

    buf.writeln();
    buf.writeln('[network_identity]');
    buf.writeln('network_name = "$networkName"');
    buf.writeln('network_secret = "$networkSecret"');

    for (final server in servers) {
      buf.writeln();
      buf.writeln('[[peer]]');
      buf.writeln('uri = "$server"');
    }

    for (final cidr in proxyNetworks) {
      buf.writeln();
      buf.writeln('[[proxy_network]]');
      buf.writeln('cidr = "$cidr"');
    }

    buf.writeln();
    buf.writeln('[flags]');
    buf.writeln('enable_encryption = ${enableEncryption ? "true" : "false"}');
    buf.writeln('enable_ipv6 = ${!disableIpv6 ? "true" : "false"}');
    buf.writeln('latency_first = ${latencyFirst ? "true" : "false"}');
    buf.writeln('enable_exit_node = ${enableExitNode ? "true" : "false"}');
    buf.writeln('no_tun = ${noTun ? "true" : "false"}');
    buf.writeln('use_smoltcp = ${useSmoltcp ? "true" : "false"}');

    if (foreignNetworkWhitelistEnabled) {
      buf.writeln('foreign_network_whitelist = "${foreignNetworkWhitelist.join(" ")}"');
    }

    buf.writeln('enable_quic_proxy = ${enableQuicProxy ? "true" : "false"}');
    buf.writeln('disable_quic_input = ${disableQuicInput ? "true" : "false"}');
    buf.writeln('enable_kcp_proxy = ${enableKcpProxy ? "true" : "false"}');
    buf.writeln('disable_kcp_input = ${disableKcpInput ? "true" : "false"}');
    buf.writeln('bind_device = ${bindDevice ? "true" : "false"}');
    buf.writeln('private_mode = ${privateMode ? "true" : "false"}');
    buf.writeln('disable_p2p = ${disableP2p ? "true" : "false"}');
    buf.writeln('multi_thread = ${multiThread ? "true" : "false"}');
    buf.writeln('accept_dns = ${acceptDns ? "true" : "false"}');
    buf.writeln('disable_sym_hole_punching = ${disableSymHolePunching ? "true" : "false"}');
    buf.writeln('relay_all_peer_rpc = ${relayAllPeerRpc ? "true" : "false"}');
    buf.writeln('disable_udp_hole_punching = ${disableUdpHolePunching ? "true" : "false"}');
    buf.writeln('proxy_forward_by_system = ${systemForwarding ? "true" : "false"}');

    return buf.toString();
  }

  Map<String, dynamic> toJson() {
    return {
      'instance_name': instanceName,
      'network_label': networkLabel,
      'is_running': isRunning,
      'hostname': hostname,
      'network_name': networkName,
      'network_secret': networkSecret,
      'dhcp': dhcp,
      'ipv4': ipv4,
      'latency_first': latencyFirst,
      'private_mode': privateMode,
      'servers': servers,
      'enable_kcp_proxy': enableKcpProxy,
      'disable_kcp_input': disableKcpInput,
      'no_tun': noTun,
      'enable_quic_proxy': enableQuicProxy,
      'disable_quic_input': disableQuicInput,
      'disable_udp_hole_punching': disableUdpHolePunching,
      'multi_thread': multiThread,
      'use_smoltcp': useSmoltcp,
      'bind_device': bindDevice,
      'disable_p2p': disableP2p,
      'enable_exit_node': enableExitNode,
      'system_forwarding': systemForwarding,
      'disable_sym_hole_punching': disableSymHolePunching,
      'disable_ipv6': disableIpv6,
      'relay_all_peer_rpc': relayAllPeerRpc,
      'enable_encryption': enableEncryption,
      'accept_dns': acceptDns,
      'foreign_network_whitelist_enabled': foreignNetworkWhitelistEnabled,
      'foreign_network_whitelist': foreignNetworkWhitelist,
      'listen_addresses': listenAddresses,
      'proxy_networks': proxyNetworks,
    };
  }

  factory NetworkConfig.fromJson(Map<String, dynamic> json) {
    return NetworkConfig(
      instanceName: json['instance_name'] as String?,
      networkLabel: json['network_label'] as String? ?? '',
      isRunning: json['is_running'] as bool? ?? false,
      hostname: json['hostname'] as String? ?? '',
      networkName: json['network_name'] as String? ?? '',
      networkSecret: json['network_secret'] as String? ?? '',
      dhcp: json['dhcp'] as bool? ?? true,
      ipv4: json['ipv4'] as String? ?? '',
      latencyFirst: json['latency_first'] as bool? ?? false,
      privateMode: json['private_mode'] as bool? ?? true,
      servers: (json['servers'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          [],
      enableKcpProxy: json['enable_kcp_proxy'] as bool? ?? true,
      disableKcpInput: json['disable_kcp_input'] as bool? ?? false,
      noTun: json['no_tun'] as bool? ?? false,
      enableQuicProxy: json['enable_quic_proxy'] as bool? ?? false,
      disableQuicInput: json['disable_quic_input'] as bool? ?? false,
      disableUdpHolePunching:
          json['disable_udp_hole_punching'] as bool? ?? false,
      multiThread: json['multi_thread'] as bool? ?? true,
      useSmoltcp: json['use_smoltcp'] as bool? ?? false,
      bindDevice: json['bind_device'] as bool? ?? true,
      disableP2p: json['disable_p2p'] as bool? ?? false,
      enableExitNode: json['enable_exit_node'] as bool? ?? false,
      systemForwarding: json['system_forwarding'] as bool? ?? false,
      disableSymHolePunching:
          json['disable_sym_hole_punching'] as bool? ?? false,
      disableIpv6: json['disable_ipv6'] as bool? ?? false,
      relayAllPeerRpc: json['relay_all_peer_rpc'] as bool? ?? false,
      enableEncryption: json['enable_encryption'] as bool? ?? true,
      acceptDns: json['accept_dns'] as bool? ?? false,
      foreignNetworkWhitelistEnabled:
          json['foreign_network_whitelist_enabled'] as bool? ?? false,
      foreignNetworkWhitelist:
          (json['foreign_network_whitelist'] as List<dynamic>?)
                  ?.map((e) => e as String)
                  .toList() ??
              [],
      listenAddresses: (json['listen_addresses'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          [],
      proxyNetworks: (json['proxy_networks'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          [],
    );
  }

  NetworkConfig copyWith({
    String? instanceName,
    String? networkLabel,
    bool? isRunning,
    String? hostname,
    String? networkName,
    String? networkSecret,
    bool? dhcp,
    String? ipv4,
    bool? latencyFirst,
    bool? privateMode,
    List<String>? servers,
    bool? enableKcpProxy,
    bool? disableKcpInput,
    bool? noTun,
    bool? enableQuicProxy,
    bool? disableQuicInput,
    bool? disableUdpHolePunching,
    bool? multiThread,
    bool? useSmoltcp,
    bool? bindDevice,
    bool? disableP2p,
    bool? enableExitNode,
    bool? systemForwarding,
    bool? disableSymHolePunching,
    bool? disableIpv6,
    bool? relayAllPeerRpc,
    bool? enableEncryption,
    bool? acceptDns,
    bool? foreignNetworkWhitelistEnabled,
    List<String>? foreignNetworkWhitelist,
    List<String>? listenAddresses,
    List<String>? proxyNetworks,
  }) {
    return NetworkConfig(
      instanceName: instanceName ?? this.instanceName,
      networkLabel: networkLabel ?? this.networkLabel,
      isRunning: isRunning ?? this.isRunning,
      hostname: hostname ?? this.hostname,
      networkName: networkName ?? this.networkName,
      networkSecret: networkSecret ?? this.networkSecret,
      dhcp: dhcp ?? this.dhcp,
      ipv4: ipv4 ?? this.ipv4,
      latencyFirst: latencyFirst ?? this.latencyFirst,
      privateMode: privateMode ?? this.privateMode,
      servers: servers ?? List.from(this.servers),
      enableKcpProxy: enableKcpProxy ?? this.enableKcpProxy,
      disableKcpInput: disableKcpInput ?? this.disableKcpInput,
      noTun: noTun ?? this.noTun,
      enableQuicProxy: enableQuicProxy ?? this.enableQuicProxy,
      disableQuicInput: disableQuicInput ?? this.disableQuicInput,
      disableUdpHolePunching:
          disableUdpHolePunching ?? this.disableUdpHolePunching,
      multiThread: multiThread ?? this.multiThread,
      useSmoltcp: useSmoltcp ?? this.useSmoltcp,
      bindDevice: bindDevice ?? this.bindDevice,
      disableP2p: disableP2p ?? this.disableP2p,
      enableExitNode: enableExitNode ?? this.enableExitNode,
      systemForwarding: systemForwarding ?? this.systemForwarding,
      disableSymHolePunching:
          disableSymHolePunching ?? this.disableSymHolePunching,
      disableIpv6: disableIpv6 ?? this.disableIpv6,
      relayAllPeerRpc: relayAllPeerRpc ?? this.relayAllPeerRpc,
      enableEncryption: enableEncryption ?? this.enableEncryption,
      acceptDns: acceptDns ?? this.acceptDns,
      foreignNetworkWhitelistEnabled:
          foreignNetworkWhitelistEnabled ?? this.foreignNetworkWhitelistEnabled,
      foreignNetworkWhitelist:
          foreignNetworkWhitelist ?? List.from(this.foreignNetworkWhitelist),
      listenAddresses:
          listenAddresses ?? List.from(this.listenAddresses),
      proxyNetworks: proxyNetworks ?? List.from(this.proxyNetworks),
    );
  }
}
