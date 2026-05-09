import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../core/easy_tier_service.dart';
import 'log_page.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  bool _autoReconnect = false;
  bool _startOnBoot = false;
  bool _darkMode = false;
  bool _followSystem = true;
  bool _notifyOnConnect = true;
  bool _notifyOnDisconnect = true;
  String _logLevel = 'info';
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _autoReconnect = prefs.getBool('auto_reconnect') ?? false;
      _startOnBoot = prefs.getBool('start_on_boot') ?? false;
      _darkMode = prefs.getBool('dark_mode') ?? false;
      _followSystem = prefs.getBool('follow_system') ?? true;
      _notifyOnConnect = prefs.getBool('notify_on_connect') ?? true;
      _notifyOnDisconnect = prefs.getBool('notify_on_disconnect') ?? true;
      _logLevel = prefs.getString('log_level') ?? 'info';
      _isLoading = false;
    });
  }

  Future<void> _setSetting(String key, dynamic value) async {
    final prefs = await SharedPreferences.getInstance();
    if (value is bool) {
      await prefs.setBool(key, value);
    } else if (value is String) {
      await prefs.setString(key, value);
    }
    if (key == 'dark_mode' || key == 'follow_system') {
      _updateThemeMode();
    }
  }

  void _updateThemeMode() {
    // The app uses ThemeMode.system by default in app.dart.
    // In a full implementation, this would update a ThemeMode notifier.
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _buildSectionHeader('通用'),
                _buildSettingsCard([
                  _buildSwitchTile(
                    icon: Icons.brightness_6,
                    title: '跟随系统',
                    subtitle: '自动切换明暗主题',
                    value: _followSystem,
                    onChanged: (v) {
                      setState(() {
                        _followSystem = v;
                        if (v) _darkMode = false;
                      });
                      _setSetting('follow_system', v);
                      _setSetting('dark_mode', false);
                    },
                  ),
                  if (!_followSystem)
                    _buildSwitchTile(
                      icon: Icons.dark_mode,
                      title: '深色模式',
                      value: _darkMode,
                      onChanged: (v) {
                        setState(() => _darkMode = v);
                        _setSetting('dark_mode', v);
                      },
                    ),
                  _buildSwitchTile(
                    icon: Icons.power_settings_new,
                    title: '开机自启',
                    subtitle: '系统启动时自动运行',
                    value: _startOnBoot,
                    onChanged: (v) {
                      setState(() => _startOnBoot = v);
                      _setSetting('start_on_boot', v);
                    },
                  ),
                ]),
                const SizedBox(height: 16),
                _buildSectionHeader('网络'),
                _buildSettingsCard([
                  _buildSwitchTile(
                    icon: Icons.replay,
                    title: '自动回连',
                    subtitle: '网络断开后自动重新连接',
                    value: _autoReconnect,
                    onChanged: (v) {
                      setState(() => _autoReconnect = v);
                      _setSetting('auto_reconnect', v);
                    },
                  ),
                ]),
                const SizedBox(height: 16),
                _buildSectionHeader('通知'),
                _buildSettingsCard([
                  _buildSwitchTile(
                    icon: Icons.check_circle_outline,
                    title: '连接成功通知',
                    value: _notifyOnConnect,
                    onChanged: (v) {
                      setState(() => _notifyOnConnect = v);
                      _setSetting('notify_on_connect', v);
                    },
                  ),
                  _buildSwitchTile(
                    icon: Icons.error_outline,
                    title: '断开连接通知',
                    value: _notifyOnDisconnect,
                    onChanged: (v) {
                      setState(() => _notifyOnDisconnect = v);
                      _setSetting('notify_on_disconnect', v);
                    },
                  ),
                ]),
                const SizedBox(height: 16),
                _buildSectionHeader('日志'),
                _buildSettingsCard([
                  _buildLogLevelSelector(),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.terminal, size: 22),
                    title: const Text('查看运行日志',
                        style: TextStyle(fontSize: 14)),
                    trailing: const Icon(Icons.chevron_right),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 8),
                    onTap: () {
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => const LogPage(),
                      ));
                    },
                  ),
                ]),
                const SizedBox(height: 16),
                _buildSectionHeader('关于'),
                _buildSettingsCard([
                  _buildInfoTile(
                    icon: Icons.info_outline,
                    title: '版本',
                    subtitle: '1.0.0+1',
                  ),
                  _buildInfoTile(
                    icon: Icons.language,
                    title: '运行平台',
                    subtitle: EasyTierServiceFactory.instance.platform,
                  ),
                  _buildInfoTile(
                    icon: Icons.memory,
                    title: '后端',
                    subtitle: 'EasyTier FFI',
                  ),
                ]),
                const SizedBox(height: 32),
                Center(
                  child: TextButton.icon(
                    icon: const Icon(Icons.delete_sweep_outlined),
                    label: const Text('清除所有数据'),
                    style: TextButton.styleFrom(
                      foregroundColor: Colors.redAccent,
                    ),
                    onPressed: _clearAllData,
                  ),
                ),
                const SizedBox(height: 32),
              ],
            ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: Theme.of(context).colorScheme.primary,
          letterSpacing: 0.5,
        ),
      ),
    );
  }

  Widget _buildSettingsCard(List<Widget> children) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: Column(children: children),
      ),
    );
  }

  Widget _buildSwitchTile({
    required IconData icon,
    required String title,
    String? subtitle,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return ListTile(
      leading: Icon(icon, size: 22),
      title: Text(title, style: const TextStyle(fontSize: 14)),
      subtitle: subtitle != null
          ? Text(subtitle,
              style: TextStyle(
                  fontSize: 12,
                  color: Theme.of(context).textTheme.bodySmall?.color))
          : null,
      trailing: Switch(value: value, onChanged: onChanged),
      contentPadding: const EdgeInsets.symmetric(horizontal: 8),
    );
  }

  Widget _buildInfoTile({
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    return ListTile(
      leading: Icon(icon, size: 22),
      title: Text(title, style: const TextStyle(fontSize: 14)),
      trailing: Text(
        subtitle,
        style: TextStyle(
          fontSize: 13,
          color: Theme.of(context).textTheme.bodySmall?.color,
          fontFamily: 'monospace',
        ),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 8),
    );
  }

  Widget _buildLogLevelSelector() {
    final levels = ['debug', 'info', 'warn', 'error'];
    return ListTile(
      leading: const Icon(Icons.terminal, size: 22),
      title: const Text('日志级别', style: TextStyle(fontSize: 14)),
      trailing: DropdownButton<String>(
        value: _logLevel,
        underline: const SizedBox.shrink(),
        items: levels.map((level) {
          return DropdownMenuItem(
            value: level,
            child: Text(
              level.toUpperCase(),
              style: TextStyle(
                fontSize: 13,
                fontFamily: 'monospace',
                color: Theme.of(context).textTheme.bodyMedium?.color,
              ),
            ),
          );
        }).toList(),
        onChanged: (v) {
          if (v != null) {
            setState(() => _logLevel = v);
            _setSetting('log_level', v);
          }
        },
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 8),
    );
  }

  Future<void> _clearAllData() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('清除所有数据'),
        content: const Text(
          '这将清除所有配置、服务器收藏和设置。此操作不可撤销。',
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('取消')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(
                backgroundColor: Colors.redAccent,
                foregroundColor: Colors.white),
            child: const Text('清除'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();
      if (!mounted) return;
      setState(() {
        _autoReconnect = false;
        _startOnBoot = false;
        _darkMode = false;
        _followSystem = true;
        _notifyOnConnect = true;
        _notifyOnDisconnect = true;
        _logLevel = 'info';
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('所有数据已清除'),
          behavior: SnackBarBehavior.floating,
        ),
      );
    }
  }
}
