import 'dart:collection';
import 'package:flutter/foundation.dart';

enum LogLevel { debug, info, warn, error }

class LogEntry {
  final DateTime timestamp;
  final LogLevel level;
  final String message;
  final String source;

  LogEntry({
    required this.level,
    required this.message,
    this.source = '',
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  String get formattedTime {
    final h = timestamp.hour.toString().padLeft(2, '0');
    final m = timestamp.minute.toString().padLeft(2, '0');
    final s = timestamp.second.toString().padLeft(2, '0');
    final ms = timestamp.millisecond.toString().padLeft(3, '0');
    return '$h:$m:$s.$ms';
  }

  String get levelLabel {
    switch (level) {
      case LogLevel.debug:
        return 'DEBUG';
      case LogLevel.info:
        return 'INFO';
      case LogLevel.warn:
        return 'WARN';
      case LogLevel.error:
        return 'ERROR';
    }
  }

  String toFormattedString() {
    final src = source.isNotEmpty ? ' [$source]' : '';
    return '[$formattedTime] [$levelLabel]$src $message';
  }
}

class LogService extends ChangeNotifier {
  static final LogService _instance = LogService._();
  static LogService get instance => _instance;

  final List<LogEntry> _logs = [];
  static const int _maxLogs = 2000;
  LogLevel _minimumLevel = LogLevel.debug;

  LogLevel get minimumLevel => _minimumLevel;
  set minimumLevel(LogLevel level) {
    _minimumLevel = level;
    notifyListeners();
  }

  LogService._();

  UnmodifiableListView<LogEntry> get logs => UnmodifiableListView(_logs);

  void debug(String message, {String source = ''}) {
    _addLog(LogLevel.debug, message, source: source);
  }

  void info(String message, {String source = ''}) {
    _addLog(LogLevel.info, message, source: source);
  }

  void warn(String message, {String source = ''}) {
    _addLog(LogLevel.warn, message, source: source);
  }

  void error(String message, {String source = ''}) {
    _addLog(LogLevel.error, message, source: source);
  }

  void _addLog(LogLevel level, String message, {String source = ''}) {
    if (level.index < _minimumLevel.index) return;
    final entry = LogEntry(level: level, message: message, source: source);
    _logs.add(entry);
    if (_logs.length > _maxLogs) {
      _logs.removeAt(0);
    }
    notifyListeners();
  }

  void clear() {
    _logs.clear();
    notifyListeners();
  }

  String getPlainText() {
    return _logs.map((e) => e.toFormattedString()).join('\n');
  }

  List<LogEntry> getFilteredLogs({LogLevel? minLevel, String? query}) {
    var result = _logs.where((e) => minLevel == null || e.level.index >= minLevel.index);
    if (query != null && query.isNotEmpty) {
      final q = query.toLowerCase();
      result = result.where((e) =>
          e.message.toLowerCase().contains(q) ||
          e.source.toLowerCase().contains(q));
    }
    return result.toList();
  }
}
