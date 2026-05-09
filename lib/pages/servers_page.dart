import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ServersPage extends StatefulWidget {
  const ServersPage({super.key});

  @override
  State<ServersPage> createState() => _ServersPageState();
}

class _ServersPageState extends State<ServersPage> {
  List<ServerEntry> _servers = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadServers();
  }

  Future<void> _loadServers() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonStr = prefs.getString('favorite_servers');

      final servers = jsonStr != null
          ? (jsonDecode(jsonStr) as List<dynamic>)
              .map((e) => ServerEntry.fromJson(e as Map<String, dynamic>))
              .toList()
          : [
              ServerEntry(
                name: '官方公共服务器',
                url: 'wss://qtet-public.070219.xyz',
                isDefault: true,
              ),
            ];

      if (!mounted) return;
      setState(() {
        _servers = servers;
        _isLoading = false;
      });

      if (jsonStr == null) {
        await _saveServers();
      }
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _servers = [
          ServerEntry(
            name: '官方公共服务器',
            url: 'wss://qtet-public.070219.xyz',
            isDefault: true,
          ),
        ];
        _isLoading = false;
      });
    }
  }

  Future<void> _saveServers() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonStr = jsonEncode(_servers.map((e) => e.toJson()).toList());
    await prefs.setString('favorite_servers', jsonStr);
  }

  Future<void> _addServer() async {
    final nameCtrl = TextEditingController();
    final urlCtrl = TextEditingController();

    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('添加服务器'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameCtrl,
              decoration: const InputDecoration(
                labelText: '服务器名称',
                hintText: '例如: 我的服务器',
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: urlCtrl,
              decoration: const InputDecoration(
                labelText: '服务器地址',
                hintText: 'wss://example.com',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('取消')),
          ElevatedButton(
            onPressed: () {
              if (urlCtrl.text.trim().isNotEmpty) {
                Navigator.pop(ctx, true);
              }
            },
            child: const Text('添加'),
          ),
        ],
      ),
    );

    if (result == true && urlCtrl.text.trim().isNotEmpty) {
      setState(() {
        _servers.add(ServerEntry(
          name: nameCtrl.text.trim().isEmpty
              ? urlCtrl.text.trim()
              : nameCtrl.text.trim(),
          url: urlCtrl.text.trim(),
        ));
      });
      _saveServers();
    }
  }

  Future<void> _editServer(int index) async {
    final entry = _servers[index];
    final nameCtrl = TextEditingController(text: entry.name);
    final urlCtrl = TextEditingController(text: entry.url);

    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('编辑服务器'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameCtrl,
              decoration: const InputDecoration(labelText: '服务器名称'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: urlCtrl,
              decoration: const InputDecoration(labelText: '服务器地址'),
            ),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('取消')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('保存'),
          ),
        ],
      ),
    );

    if (result == true) {
      setState(() {
        _servers[index].name = nameCtrl.text.trim();
        _servers[index].url = urlCtrl.text.trim();
      });
      _saveServers();
    }
  }

  Future<void> _deleteServer(int index) async {
    final entry = _servers[index];
    if (entry.isDefault) {
      _showSnackBar('无法删除默认服务器');
      return;
    }
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('删除服务器'),
        content: Text('确定删除 "${entry.name}" 吗？'),
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
    if (confirm == true) {
      setState(() => _servers.removeAt(index));
      _saveServers();
    }
  }

  void _showSnackBar(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), behavior: SnackBarBehavior.floating),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('服务器收藏'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: '添加服务器',
            onPressed: _addServer,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _servers.isEmpty
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.dns_outlined,
                          size: 64,
                          color:
                              Theme.of(context).textTheme.bodySmall?.color),
                      const SizedBox(height: 16),
                      Text('暂无收藏服务器',
                          style: TextStyle(
                              color:
                                  Theme.of(context).textTheme.bodySmall?.color)),
                      const SizedBox(height: 24),
                      ElevatedButton.icon(
                        icon: const Icon(Icons.add),
                        label: const Text('添加服务器'),
                        onPressed: _addServer,
                      ),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadServers,
                  child: ListView.builder(
                    padding: const EdgeInsets.all(12),
                    itemCount: _servers.length,
                    itemBuilder: (context, index) {
                      final entry = _servers[index];
                      return _ServerCard(
                        entry: entry,
                        onEdit: () => _editServer(index),
                        onDelete: () => _deleteServer(index),
                      );
                    },
                  ),
                ),
    );
  }
}

class ServerEntry {
  String name;
  String url;
  final bool isDefault;

  ServerEntry({
    required this.name,
    required this.url,
    this.isDefault = false,
  });

  Map<String, dynamic> toJson() => {
        'name': name,
        'url': url,
        'is_default': isDefault,
      };

  factory ServerEntry.fromJson(Map<String, dynamic> json) => ServerEntry(
        name: json['name'] as String? ?? '',
        url: json['url'] as String? ?? '',
        isDefault: json['is_default'] as bool? ?? false,
      );
}

class _ServerCard extends StatelessWidget {
  final ServerEntry entry;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const _ServerCard({
    required this.entry,
    required this.onEdit,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                entry.isDefault ? Icons.star : Icons.dns,
                color: Theme.of(context).colorScheme.primary,
                size: 22,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        entry.name,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Theme.of(context).textTheme.bodyLarge?.color,
                        ),
                      ),
                      if (entry.isDefault) ...[
                        const SizedBox(width: 6),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: Theme.of(context)
                                .colorScheme
                                .primary
                                .withOpacity(0.1),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: const Text(
                            '默认',
                            style: TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    entry.url,
                    style: TextStyle(
                      fontSize: 12,
                      fontFamily: 'monospace',
                      color: Theme.of(context).textTheme.bodySmall?.color,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            if (!entry.isDefault) ...[
              IconButton(
                icon: const Icon(Icons.edit_outlined, size: 20),
                tooltip: '编辑',
                onPressed: onEdit,
              ),
              IconButton(
                icon: const Icon(Icons.delete_outline, size: 20),
                tooltip: '删除',
                onPressed: onDelete,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
