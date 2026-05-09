import 'package:flutter/material.dart';
import '../core/config/node_info.dart';

class NodeInfoCard extends StatelessWidget {
  final NodeInfo node;

  const NodeInfoCard({super.key, required this.node});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    Color connColor;
    String connLabel;
    switch (node.connTypeEnum) {
      case ConnectionType.direct:
        connColor = Colors.green;
        connLabel = '直连';
      case ConnectionType.relay:
        connColor = Colors.orange;
        connLabel = '中转';
      case ConnectionType.server:
        connColor = Colors.blue;
        connLabel = '服务器';
      case ConnectionType.unknown:
        connColor = Colors.grey;
        connLabel = '未知';
    }

    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF0f3460) : Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: node.isLocal
              ? const Color(0xFF66ccff).withOpacity(0.3)
              : Colors.transparent,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isDark ? 0.2 : 0.04),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: connColor.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    connLabel,
                    style: TextStyle(
                      color: connColor,
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                if (node.isLocal)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: const Color(0xFF66ccff).withOpacity(0.15),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Text(
                      '本机',
                      style: TextStyle(
                        color: Color(0xFF66ccff),
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                const Spacer(),
                if (node.latencyMs > 0)
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.signal_cellular_alt,
                          size: 14,
                          color: node.latencyMs < 50
                              ? Colors.green
                              : node.latencyMs < 100
                                  ? Colors.orange
                                  : Colors.red),
                      const SizedBox(width: 4),
                      Text(
                        node.latencyText,
                        style: TextStyle(
                          fontSize: 12,
                          color: Theme.of(context)
                              .textTheme
                              .bodySmall
                              ?.color,
                        ),
                      ),
                    ],
                  ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Icon(Icons.computer, size: 16, color: Theme.of(context).textTheme.bodySmall?.color),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    node.hostname,
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: Theme.of(context).textTheme.bodyLarge?.color,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Icon(Icons.lan, size: 14, color: Theme.of(context).textTheme.bodySmall?.color),
                const SizedBox(width: 6),
                Text(
                  node.virtualIp,
                  style: TextStyle(
                    fontSize: 13,
                    color: Theme.of(context).textTheme.bodyMedium?.color,
                    fontFamily: 'monospace',
                  ),
                ),
                const Spacer(),
                if (node.protocol.isNotEmpty)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: isDark
                          ? Colors.white.withOpacity(0.05)
                          : Colors.grey.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      node.protocol,
                      style: TextStyle(
                        fontSize: 11,
                        color: Theme.of(context).textTheme.bodySmall?.color,
                      ),
                    ),
                  ),
              ],
            ),
            if (node.rxBytes > 0 || node.txBytes > 0) ...[
              const SizedBox(height: 6),
              Row(
                children: [
                  Icon(Icons.swap_vert, size: 14, color: Theme.of(context).textTheme.bodySmall?.color),
                  const SizedBox(width: 6),
                  Text(
                    node.trafficText,
                    style: TextStyle(
                      fontSize: 12,
                      color: Theme.of(context).textTheme.bodySmall?.color,
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
