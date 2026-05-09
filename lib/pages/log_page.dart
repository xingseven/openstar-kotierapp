import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../core/log_service.dart';

class LogPage extends StatefulWidget {
  const LogPage({super.key});

  @override
  State<LogPage> createState() => _LogPageState();
}

class _LogPageState extends State<LogPage> {
  final LogService _logService = LogService.instance;
  final ScrollController _scrollCtrl = ScrollController();
  bool _autoScroll = true;
  String _searchQuery = '';
  LogLevel? _levelFilter;
  bool _showFilter = false;

  @override
  void initState() {
    super.initState();
    _logService.addListener(_onLogChanged);
    _scrollCtrl.addListener(_onScroll);
  }

  @override
  void dispose() {
    _logService.removeListener(_onLogChanged);
    _scrollCtrl.removeListener(_onScroll);
    _scrollCtrl.dispose();
    super.dispose();
  }

  void _onLogChanged() {
    if (!mounted) return;
    setState(() {});
    if (_autoScroll) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollCtrl.hasClients) {
          _scrollCtrl.animateTo(
            _scrollCtrl.position.maxScrollExtent,
            duration: const Duration(milliseconds: 150),
            curve: Curves.easeOut,
          );
        }
      });
    }
  }

  void _onScroll() {
    if (!_scrollCtrl.hasClients) return;
    final isAtBottom = _scrollCtrl.position.pixels >=
        _scrollCtrl.position.maxScrollExtent - 50;
    if (_autoScroll != isAtBottom) {
      setState(() => _autoScroll = isAtBottom);
    }
  }

  void _copyAll() {
    final text = _logService.getPlainText();
    if (text.isEmpty) {
      _showSnackBar('没有日志可复制');
      return;
    }
    Clipboard.setData(ClipboardData(text: text));
    _showSnackBar('已复制 ${_logService.logs.length} 条日志');
  }

  void _copyFiltered() {
    final filtered = _logService.getFilteredLogs(
        minLevel: _levelFilter, query: _searchQuery);
    if (filtered.isEmpty) {
      _showSnackBar('没有匹配的日志');
      return;
    }
    final text = filtered.map((e) => e.toFormattedString()).join('\n');
    Clipboard.setData(ClipboardData(text: text));
    _showSnackBar('已复制 ${filtered.length} 条日志');
  }

  void _clearLogs() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('清除日志'),
        content: const Text('确定清除所有日志吗？'),
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
      _logService.clear();
      _showSnackBar('日志已清除');
    }
  }

  void _showSnackBar(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), behavior: SnackBarBehavior.floating),
    );
  }

  @override
  Widget build(BuildContext context) {
    final logs = _logService.getFilteredLogs(
        minLevel: _levelFilter, query: _searchQuery);

    final logCounts = {
      LogLevel.debug: _logService.logs.where((e) => e.level == LogLevel.debug).length,
      LogLevel.info: _logService.logs.where((e) => e.level == LogLevel.info).length,
      LogLevel.warn: _logService.logs.where((e) => e.level == LogLevel.warn).length,
      LogLevel.error: _logService.logs.where((e) => e.level == LogLevel.error).length,
    };

    return Scaffold(
      appBar: AppBar(
        title: const Text('日志'),
        actions: [
          IconButton(
            icon: Icon(
                _showFilter ? Icons.filter_alt_off : Icons.filter_alt_outlined),
            tooltip: '筛选',
            onPressed: () => setState(() => _showFilter = !_showFilter),
          ),
          IconButton(
            icon: const Icon(Icons.copy),
            tooltip: '复制筛选结果',
            onPressed: _copyFiltered,
          ),
          IconButton(
            icon: const Icon(Icons.copy_all),
            tooltip: '复制全部',
            onPressed: _copyAll,
          ),
          IconButton(
            icon: const Icon(Icons.delete_sweep_outlined),
            tooltip: '清除日志',
            onPressed: _clearLogs,
          ),
        ],
      ),
      body: Column(
        children: [
          if (_showFilter) _buildFilterBar(logCounts),
          _buildStatusBar(logs.length, logCounts),
          Expanded(
            child: logs.isEmpty
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.terminal,
                            size: 48,
                            color: Theme.of(context)
                                .textTheme
                                .bodySmall
                                ?.color),
                        const SizedBox(height: 12),
                        Text('暂无日志',
                            style: TextStyle(
                                color: Theme.of(context)
                                    .textTheme
                                    .bodySmall
                                    ?.color)),
                      ],
                    ),
                  )
                : ListView.builder(
                    controller: _scrollCtrl,
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 4),
                    itemCount: logs.length,
                    itemBuilder: (context, index) {
                      return _LogEntryWidget(entry: logs[index]);
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.small(
        onPressed: () {
          _scrollCtrl.animateTo(
            0,
            duration: const Duration(milliseconds: 200),
            curve: Curves.easeOut,
          );
        },
        child: const Icon(Icons.vertical_align_top),
      ),
    );
  }

  Widget _buildFilterBar(Map<LogLevel, int> counts) {
    final levels = LogLevel.values;
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
      child: Column(
        children: [
          TextField(
            decoration: const InputDecoration(
              hintText: '搜索日志...',
              prefixIcon: Icon(Icons.search),
              isDense: true,
              contentPadding:
                  EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            ),
            onChanged: (v) => setState(() => _searchQuery = v),
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _FilterChip(
                  label: '全部',
                  count: _logService.logs.length,
                  selected: _levelFilter == null,
                  color: Colors.grey,
                  onTap: () => setState(() => _levelFilter = null),
                ),
                const SizedBox(width: 6),
                ...levels.map((level) {
                  final colors = {
                    LogLevel.debug: Colors.grey,
                    LogLevel.info: Colors.blue,
                    LogLevel.warn: Colors.orange,
                    LogLevel.error: Colors.red,
                  };
                  final labels = {
                    LogLevel.debug: 'DEBUG',
                    LogLevel.info: 'INFO',
                    LogLevel.warn: 'WARN',
                    LogLevel.error: 'ERROR',
                  };
                  return _FilterChip(
                    label: labels[level]!,
                    count: counts[level] ?? 0,
                    selected: _levelFilter == level,
                    color: colors[level]!,
                    onTap: () =>
                        setState(() => _levelFilter = level),
                  );
                }),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusBar(int filteredCount, Map<LogLevel, int> counts) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Row(
        children: [
          Text(
            '共 ${_logService.logs.length} 条',
            style: TextStyle(
              fontSize: 12,
              color: Theme.of(context).textTheme.bodySmall?.color,
            ),
          ),
          if (filteredCount != _logService.logs.length)
            Text(
              ' (筛选 $filteredCount)',
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
          const Spacer(),
          if (!_autoScroll)
            GestureDetector(
              onTap: () {
                setState(() => _autoScroll = true);
                _scrollCtrl.animateTo(
                  _scrollCtrl.position.maxScrollExtent,
                  duration: const Duration(milliseconds: 200),
                  curve: Curves.easeOut,
                );
              },
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: Colors.orange.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: const Text(
                  '新日志 ↓',
                  style: TextStyle(fontSize: 11, color: Colors.orange),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final int count;
  final bool selected;
  final Color color;
  final VoidCallback onTap;

  const _FilterChip({
    required this.label,
    required this.count,
    required this.selected,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: selected ? color.withOpacity(0.2) : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: selected ? color : color.withOpacity(0.3),
            width: selected ? 1.5 : 1,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                color: selected ? color : color.withOpacity(0.5),
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 6),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
                color: selected ? color : null,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LogEntryWidget extends StatelessWidget {
  final LogEntry entry;

  const _LogEntryWidget({required this.entry});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    Color levelColor;
    switch (entry.level) {
      case LogLevel.debug:
        levelColor = isDark ? Colors.grey : Colors.grey;
      case LogLevel.info:
        levelColor = Colors.blue;
      case LogLevel.warn:
        levelColor = Colors.orange;
      case LogLevel.error:
        levelColor = Colors.red;
    }

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 1),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
      decoration: BoxDecoration(
        color: levelColor.withOpacity(isDark ? 0.06 : 0.04),
        borderRadius: BorderRadius.circular(6),
        border: Border(
          left: BorderSide(color: levelColor.withOpacity(0.5), width: 3),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                entry.formattedTime,
                style: TextStyle(
                  fontSize: 11,
                  fontFamily: 'monospace',
                  color: Theme.of(context).textTheme.bodySmall?.color,
                ),
              ),
              const SizedBox(width: 6),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                decoration: BoxDecoration(
                  color: levelColor.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(3),
                ),
                child: Text(
                  entry.levelLabel,
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w600,
                    color: levelColor,
                    fontFamily: 'monospace',
                  ),
                ),
              ),
              if (entry.source.isNotEmpty) ...[
                const SizedBox(width: 4),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                  decoration: BoxDecoration(
                    color: isDark
                        ? Colors.white.withOpacity(0.05)
                        : Colors.black.withOpacity(0.04),
                    borderRadius: BorderRadius.circular(3),
                  ),
                  child: Text(
                    entry.source,
                    style: TextStyle(
                      fontSize: 10,
                      color: Theme.of(context).textTheme.bodySmall?.color,
                      fontFamily: 'monospace',
                    ),
                  ),
                ),
              ],
            ],
          ),
          const SizedBox(height: 3),
          Text(
            entry.message,
            style: TextStyle(
              fontSize: 12,
              fontFamily: 'monospace',
              color: Theme.of(context).textTheme.bodyMedium?.color,
              height: 1.3,
            ),
          ),
        ],
      ),
    );
  }
}
