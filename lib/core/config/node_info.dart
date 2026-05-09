enum ConnectionType { direct, relay, server, unknown }

class NodeInfo {
  final String hostname;
  final String virtualIp;
  final int latencyMs;
  final String protocol;
  final String connectionType;
  final bool isLocal;
  final String natType;
  final int rxBytes;
  final int txBytes;
  final double lossRate;

  NodeInfo({
    required this.hostname,
    required this.virtualIp,
    this.latencyMs = 0,
    this.protocol = '',
    this.connectionType = 'unknown',
    this.isLocal = false,
    this.natType = '',
    this.rxBytes = 0,
    this.txBytes = 0,
    this.lossRate = 0.0,
  });

  ConnectionType get connTypeEnum {
    switch (connectionType) {
      case 'direct':
        return ConnectionType.direct;
      case 'relay':
        return ConnectionType.relay;
      case 'server':
        return ConnectionType.server;
      default:
        return ConnectionType.unknown;
    }
  }

  String get latencyText {
    if (latencyMs <= 0) return '-';
    return '${latencyMs}ms';
  }

  String get trafficText {
    return '↓${_formatBytes(rxBytes)} ↑${_formatBytes(txBytes)}';
  }

  static String _formatBytes(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }

  factory NodeInfo.fromJson(Map<String, dynamic> json) {
    return NodeInfo(
      hostname: json['hostname'] as String? ?? '',
      virtualIp: json['virtual_ip'] as String? ?? json['ipv4'] as String? ?? '',
      latencyMs: (json['latency_ms'] as num?)?.toInt() ?? 0,
      protocol: json['tunnel_proto'] as String? ?? '',
      connectionType: json['conn_type'] as String? ?? 'unknown',
      isLocal: json['is_local'] as bool? ?? false,
      natType: json['nat_type'] as String? ?? '',
      rxBytes: (json['rx_bytes'] as num?)?.toInt() ?? 0,
      txBytes: (json['tx_bytes'] as num?)?.toInt() ?? 0,
      lossRate: (json['loss_rate'] as num?)?.toDouble() ?? 0.0,
    );
  }
}
