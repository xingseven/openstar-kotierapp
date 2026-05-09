import 'dart:convert';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/easy_tier_service.dart';
import '../core/config/network_config.dart';
import '../core/log_service.dart';

class OneClickPage extends StatefulWidget {
  const OneClickPage({super.key});

  @override
  State<OneClickPage> createState() => _OneClickPageState();
}

class _OneClickPageState extends State<OneClickPage> {
  bool _isHostMode = true;
  bool _isRunning = false;
  bool _isLoading = false;
  String _generatedCode = '';
  NetworkConfig? _hostConfig;
  String _guestCode = '';
  String _statusMessage = '';
  bool _statusIsError = false;

  @override
  void dispose() {
    _stopIfRunning();
    super.dispose();
  }

  Future<void> _stopIfRunning() async {
    if (_isRunning && _hostConfig != null) {
      await EasyTierServiceFactory.instance
          .stopNetwork(_hostConfig!.instanceName);
    }
  }

  Future<void> _startAsHost() async {
    setState(() {
      _isLoading = true;
      _statusMessage = '';
    });

    final config = NetworkConfig(
      hostname: 'Host-${_randomSuffix(4)}',
      dhcp: true,
      privateMode: false,
    );

    final result = await EasyTierServiceFactory.instance.startNetwork(config);
    if (!mounted) return;

    if (result.success) {
      _hostConfig = config;
      LogService.instance.info(
          '一键联机: 创建网络 ${config.networkName}',
          source: 'OneClick');
      final payload = {
        'n': config.networkName,
        's': config.networkSecret,
        'v': config.servers,
      };
      final jsonStr = jsonEncode(payload);
      final encoded = _base32Encode(utf8.encode(jsonStr));

      // 自检: 解码验证
      try {
        final testDecoded = utf8.decode(_base32Decode(encoded));
        final testPayload = jsonDecode(testDecoded);
        if (testPayload == null) {
          LogService.instance.error('编码自检失败: 解码结果为空',
              source: 'OneClick');
        }
      } catch (e) {
        LogService.instance.error('编码自检失败: $e', source: 'OneClick');
      }

      setState(() {
        _isRunning = true;
        _generatedCode = encoded;
        _statusMessage = '网络已启动，分享下方编码给好友';
      });
    } else {
      setState(() {
        _statusMessage = '启动失败: ${result.errorMessage}';
        _statusIsError = true;
      });
    }

    setState(() => _isLoading = false);
  }

  Future<void> _stopAsHost() async {
    if (_hostConfig == null) return;
    setState(() => _isLoading = true);

    await EasyTierServiceFactory.instance
        .stopNetwork(_hostConfig!.instanceName);

    if (!mounted) return;
    LogService.instance.info(
        '一键联机: 停止网络 ${_hostConfig!.networkName}',
        source: 'OneClick');
    setState(() {
      _isRunning = false;
      _generatedCode = '';
      _statusMessage = '网络已停止';
      _statusIsError = false;
    });
    setState(() => _isLoading = false);
  }

  Future<void> _joinAsGuest() async {
    if (_guestCode.trim().isEmpty) {
      setState(() {
        _statusMessage = '请输入联机编码';
        _statusIsError = true;
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _statusMessage = '';
    });

    try {
      final code = _guestCode
          .trim()
          .replaceAll(RegExp(r'[\s\r\n]'), '')
          .replaceAll('-', '');
      final decoded = utf8.decode(_base32Decode(code));
      final payload = jsonDecode(decoded) as Map<String, dynamic>;

      final config = NetworkConfig(
        hostname: 'Guest-${_randomSuffix(4)}',
        networkName: payload['n'] as String? ?? '',
        networkSecret: payload['s'] as String? ?? '',
        servers: (payload['v'] as List<dynamic>?)
                ?.map((e) => e as String)
                .toList() ??
            ['wss://qtet-public.070219.xyz'],
        dhcp: true,
        privateMode: false,
      );

      final result = await EasyTierServiceFactory.instance.startNetwork(config);
      if (!mounted) return;

      if (result.success) {
        LogService.instance.info(
            '一键联机: 已加入网络 ${config.networkName}',
            source: 'OneClick');
        setState(() {
          _isRunning = true;
          _hostConfig = config;
          _statusMessage = '已成功加入网络 ${config.networkName}';
          _statusIsError = false;
        });
      } else {
        LogService.instance.error(
            '一键联机: 加入失败 ${result.errorMessage}',
            source: 'OneClick');
        setState(() {
          _statusMessage = '加入失败: ${result.errorMessage}';
          _statusIsError = true;
        });
      }
    } catch (e) {
      LogService.instance.error(
          '编码解析失败: $e', source: 'OneClick');
      setState(() {
        _statusMessage = '编码解析失败，请检查输入是否正确';
        _statusIsError = true;
      });
    }

    setState(() => _isLoading = false);
  }

  String _randomSuffix(int length) {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    final rand = Random();
    return List.generate(length, (_) => chars[rand.nextInt(chars.length)]).join();
  }

  static const String _base32Alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

  String _base32Encode(List<int> bytes) {
    final result = StringBuffer();
    int bits = 0;
    int bitCount = 0;

    for (final byte in bytes) {
      bits = (bits << 8) | byte;
      bitCount += 8;
      while (bitCount >= 5) {
        bitCount -= 5;
        result.write(_base32Alphabet[(bits >> bitCount) & 0x1F]);
        bits &= (1 << bitCount) - 1;
      }
    }

    if (bitCount > 0) {
      bits <<= (5 - bitCount);
      result.write(_base32Alphabet[bits & 0x1F]);
    }

    final fullLen = ((bytes.length + 4) ~/ 5) * 8;
    while (result.length < fullLen) {
      result.write('=');
    }

    return result.toString();
  }

  List<int> _base32Decode(String encoded) {
    final clean = encoded.replaceAll('=', '').toUpperCase();
    final result = <int>[];
    int bits = 0;
    int bitCount = 0;

    for (int i = 0; i < clean.length; i++) {
      final idx = _base32Alphabet.indexOf(clean[i]);
      if (idx < 0) continue;
      bits = (bits << 5) | idx;
      bitCount += 5;
      if (bitCount >= 8) {
        bitCount -= 8;
        result.add((bits >> bitCount) & 0xFF);
        bits &= (1 << bitCount) - 1;
      }
    }

    return result;
  }

  void _copyCode() {
    if (_generatedCode.isEmpty) return;
    Clipboard.setData(ClipboardData(text: _generatedCode));
    setState(() {
      _statusMessage = '已复制到剪贴板';
      _statusIsError = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('一键联机')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildModeToggle(),
          const SizedBox(height: 16),
          if (_isHostMode) _buildHostMode() else _buildGuestMode(),
        ],
      ),
    );
  }

  Widget _buildModeToggle() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(4),
        child: Row(
          children: [
            Expanded(
              child: GestureDetector(
                onTap: () => setState(() {
                  _isHostMode = true;
                  _statusMessage = '';
                }),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(
                    color: _isHostMode
                        ? Theme.of(context).colorScheme.primary
                        : Colors.transparent,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.lan,
                        size: 18,
                        color: _isHostMode ? Colors.white : null,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        '创建网络（房主）',
                        style: TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 14,
                          color: _isHostMode
                              ? Colors.white
                              : Theme.of(context).textTheme.bodyLarge?.color,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            Expanded(
              child: GestureDetector(
                onTap: () => setState(() {
                  _isHostMode = false;
                  _statusMessage = '';
                }),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  decoration: BoxDecoration(
                    color: !_isHostMode
                        ? Theme.of(context).colorScheme.primary
                        : Colors.transparent,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.group_add,
                        size: 18,
                        color: !_isHostMode ? Colors.white : null,
                      ),
                      const SizedBox(width: 6),
                      Text(
                        '加入网络（房客）',
                        style: TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 14,
                          color: !_isHostMode
                              ? Colors.white
                              : Theme.of(context).textTheme.bodyLarge?.color,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHostMode() {
    return Column(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                Icon(
                  _isRunning ? Icons.wifi : Icons.wifi_off,
                  size: 48,
                  color: _isRunning
                      ? const Color(0xFF4CAF50)
                      : Theme.of(context).textTheme.bodySmall?.color,
                ),
                const SizedBox(height: 12),
                Text(
                  _isRunning ? '网络运行中' : '点击下方按钮创建网络',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Theme.of(context).textTheme.bodyLarge?.color,
                  ),
                ),
                if (_isRunning && _hostConfig != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    '网络: ${_hostConfig!.networkName}',
                    style: TextStyle(
                      fontSize: 14,
                      color: Theme.of(context).textTheme.bodyMedium?.color,
                    ),
                  ),
                ],
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: _isRunning
                      ? ElevatedButton.icon(
                          icon: const Icon(Icons.stop),
                          label: Text(_isLoading ? '停止中...' : '停止网络'),
                          onPressed: _isLoading ? null : _stopAsHost,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.redAccent,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 16),
                          ),
                        )
                      : ElevatedButton.icon(
                          icon: const Icon(Icons.play_arrow),
                          label: Text(_isLoading ? '启动中...' : '启动网络'),
                          onPressed: _isLoading ? null : _startAsHost,
                          style: ElevatedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 16),
                          ),
                        ),
                ),
              ],
            ),
          ),
        ),
        if (_generatedCode.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.share, size: 18,
                          color: Theme.of(context).colorScheme.primary),
                      const SizedBox(width: 8),
                      Text('联机编码',
                          style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                              color:
                                  Theme.of(context).textTheme.bodyLarge?.color)),
                      const Spacer(),
                      IconButton(
                        icon: const Icon(Icons.copy, size: 20),
                        tooltip: '复制编码',
                        onPressed: _copyCode,
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Theme.of(context).brightness == Brightness.dark
                          ? const Color(0xFF0f3460)
                          : Colors.grey.withOpacity(0.05),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: SelectableText(
                      _generatedCode,
                      style: const TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 12,
                        letterSpacing: 1.5,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '将此编码发送给好友，对方在一键联机页面选择"加入网络"并粘贴即可',
                    style: TextStyle(
                      fontSize: 12,
                      color: Theme.of(context).textTheme.bodySmall?.color,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
        if (_statusMessage.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Text(
              _statusMessage,
              style: TextStyle(
                color: _statusIsError ? Colors.redAccent : const Color(0xFF4CAF50),
                fontSize: 14,
              ),
              textAlign: TextAlign.center,
            ),
          ),
      ],
    );
  }

  Widget _buildGuestMode() {
    return Column(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.vpn_key, size: 18,
                        color: Theme.of(context).colorScheme.primary),
                    const SizedBox(width: 8),
                    Text('输入联机编码',
                        style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color:
                                Theme.of(context).textTheme.bodyLarge?.color)),
                  ],
                ),
                const SizedBox(height: 12),
                TextField(
                  decoration: const InputDecoration(
                    hintText: '粘贴房主分享的编码',
                    contentPadding:
                        EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  ),
                  maxLines: 3,
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
                  onChanged: (v) => _guestCode = v,
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton.icon(
                    icon: const Icon(Icons.login),
                    label: Text(_isLoading ? '加入中...' : '加入网络'),
                    onPressed: _isLoading || _isRunning ? null : _joinAsGuest,
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        if (_isRunning) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  const Icon(Icons.check_circle, color: Color(0xFF4CAF50)),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('已加入网络',
                            style: TextStyle(
                                fontWeight: FontWeight.w600,
                                color: Theme.of(context)
                                    .textTheme
                                    .bodyLarge
                                    ?.color)),
                        if (_hostConfig != null)
                          Text(_hostConfig!.networkName,
                              style: TextStyle(
                                  fontSize: 13,
                                  color: Theme.of(context)
                                      .textTheme
                                      .bodySmall
                                      ?.color)),
                      ],
                    ),
                  ),
                  TextButton(
                    onPressed: _stopAsHost,
                    child: const Text('离开'),
                  ),
                ],
              ),
            ),
          ),
        ],
        if (_statusMessage.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Text(
              _statusMessage,
              style: TextStyle(
                color: _statusIsError ? Colors.redAccent : const Color(0xFF4CAF50),
                fontSize: 14,
              ),
              textAlign: TextAlign.center,
            ),
          ),
      ],
    );
  }
}
