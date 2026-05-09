import 'dart:convert';
import 'dart:ffi';
import 'dart:io' show Platform;
import 'package:ffi/ffi.dart';
import 'config/network_config.dart';
import 'config/node_info.dart';
import 'easy_tier_service.dart';

typedef ParseConfigNative = Int32 Function(Pointer<Utf8>);
typedef ParseConfigDart = int Function(Pointer<Utf8>);

typedef RunNetworkInstanceNative = Int32 Function(Pointer<Utf8>);
typedef RunNetworkInstanceDart = int Function(Pointer<Utf8>);

typedef RetainNetworkInstanceNative = Int32 Function(
    Pointer<Pointer<Utf8>>, IntPtr);
typedef RetainNetworkInstanceDart = int Function(
    Pointer<Pointer<Utf8>>, int);

typedef CollectNetworkInfosNative = Int32 Function(
    Pointer<KeyValuePair>, IntPtr);
typedef CollectNetworkInfosDart = int Function(
    Pointer<KeyValuePair>, int);

typedef GetErrorMsgNative = Void Function(Pointer<Pointer<Utf8>>);
typedef GetErrorMsgDart = void Function(Pointer<Pointer<Utf8>>);

typedef FreeStringNative = Void Function(Pointer<Utf8>);
typedef FreeStringDart = void Function(Pointer<Utf8>);

final class KeyValuePair extends Struct {
  external Pointer<Utf8> key;

  external Pointer<Utf8> value;
}

class NativeEasyTierService implements EasyTierService {
  DynamicLibrary? _lib;
  ParseConfigDart? _parseConfig;
  RunNetworkInstanceDart? _runNetworkInstance;
  RetainNetworkInstanceDart? _retainNetworkInstance;
  CollectNetworkInfosDart? _collectNetworkInfos;
  GetErrorMsgDart? _getErrorMsg;
  FreeStringDart? _freeString;
  bool _initialized = false;

  static const int _maxNetworkInstances = 16;

  @override
  String get platform {
    return 'native';
  }

  @override
  Future<bool> initialize() async {
    try {
      if (NativeLibrary.isAndroid) {
        _lib = DynamicLibrary.open('libeasytier_ffi.so');
      } else if (NativeLibrary.isIOS) {
        _lib = DynamicLibrary.process();
      } else {
        _lib = DynamicLibrary.open('easytier_ffi.dll');
      }

      _parseConfig = _lib!
          .lookupFunction<ParseConfigNative, ParseConfigDart>('parse_config');
      _runNetworkInstance = _lib!.lookupFunction<RunNetworkInstanceNative,
          RunNetworkInstanceDart>('run_network_instance');
      _retainNetworkInstance = _lib!
          .lookupFunction<RetainNetworkInstanceNative,
              RetainNetworkInstanceDart>('retain_network_instance');
      _collectNetworkInfos = _lib!
          .lookupFunction<CollectNetworkInfosNative, CollectNetworkInfosDart>(
              'collect_network_infos');
      _getErrorMsg =
          _lib!.lookupFunction<GetErrorMsgNative, GetErrorMsgDart>(
              'get_error_msg');
      _freeString =
          _lib!.lookupFunction<FreeStringNative, FreeStringDart>(
              'free_string');

      _initialized = true;
      return true;
    } catch (e) {
      _initialized = false;
      return false;
    }
  }

  @override
  Future<bool> parseConfig(String tomlConfig) async {
    if (!_initialized) return false;
    final cfgPtr = tomlConfig.toNativeUtf8();
    try {
      return _parseConfig!(cfgPtr) == 0;
    } finally {
      calloc.free(cfgPtr);
    }
  }

  @override
  Future<EasyTierResult> startNetwork(NetworkConfig config) async {
    final toml = config.toToml();
    final cfgPtr = toml.toNativeUtf8();
    try {
      final result = _runNetworkInstance!(cfgPtr);
      if (result == 0) {
        config.isRunning = true;
        return EasyTierResult.ok();
      }
      return EasyTierResult.fail(_getLastError());
    } finally {
      calloc.free(cfgPtr);
    }
  }

  @override
  Future<EasyTierResult> stopNetwork(String instanceName) async {
    final infos = <KeyValuePair>[];
    final maxLen = _maxNetworkInstances;
    final cInfos = calloc<KeyValuePair>(maxLen);
    try {
      final count = _collectNetworkInfos!(cInfos, maxLen);
      if (count < 0) {
        return EasyTierResult.fail(_getLastError());
      }

      final names = <String>[];
      for (int i = 0; i < count; i++) {
        final kv = cInfos[i];
        if (kv.key != nullptr) {
          final name = kv.key.toDartString();
          _freeString!(kv.key);
          if (name != instanceName) {
            names.add(name);
          }
        }
        if (kv.value != nullptr) {
          _freeString!(kv.value);
        }
      }

      final retainCount = names.length;
      if (retainCount == 0) {
        return EasyTierResult.ok();
      }

      final ptrArray = calloc<Pointer<Utf8>>(retainCount);
      final nativeStrings = <Pointer<Utf8>>[];
      for (int i = 0; i < retainCount; i++) {
        nativeStrings.add(names[i].toNativeUtf8());
        ptrArray[i] = nativeStrings[i];
      }

      try {
        final result =
            _retainNetworkInstance!(ptrArray, retainCount);
        if (result == 0) {
          return EasyTierResult.ok();
        }
        return EasyTierResult.fail(_getLastError());
      } finally {
        for (final ptr in nativeStrings) {
          calloc.free(ptr);
        }
        calloc.free(ptrArray);
      }
    } finally {
      calloc.free(cInfos);
    }
  }

  @override
  Future<List<NodeInfo>> collectNodeInfos(String instanceName) async {
    final maxLen = _maxNetworkInstances;
    final cInfos = calloc<KeyValuePair>(maxLen);
    try {
      final count = _collectNetworkInfos!(cInfos, maxLen);
      if (count < 0) return [];

      final nodes = <NodeInfo>[];
      for (int i = 0; i < count; i++) {
        final kv = cInfos[i];
        String? key;
        String? value;

        if (kv.key != nullptr) {
          key = kv.key.toDartString();
          _freeString!(kv.key);
        }
        if (kv.value != nullptr) {
          value = kv.value.toDartString();
          _freeString!(kv.value);
        }

        if (key != null && value != null) {
          try {
            final json = jsonDecode(value) as Map<String, dynamic>;
            json['is_local'] = key == instanceName;
            nodes.add(NodeInfo.fromJson(json));
          } catch (_) {}
        }
      }

      return nodes;
    } finally {
      calloc.free(cInfos);
    }
  }

  @override
  Future<bool> stopAllNetworks() async {
    final result = _retainNetworkInstance!(nullptr, 0);
    return result == 0;
  }

  @override
  Future<List<NetworkConfig>> loadConfigs() async {
    return [];
  }

  @override
  Future<bool> saveConfigs(List<NetworkConfig> configs) async {
    return true;
  }

  String _getLastError() {
    final outPtr = calloc<Pointer<Utf8>>();
    try {
      _getErrorMsg!(outPtr);
      final errorPtr = outPtr.value;
      if (errorPtr != nullptr) {
        final msg = errorPtr.toDartString();
        _freeString!(errorPtr);
        return msg;
      }
      return 'Unknown error';
    } finally {
      calloc.free(outPtr);
    }
  }

  @override
  void dispose() {
    _initialized = false;
    _lib = null;
  }
}

class NativeLibrary {
  static bool get isAndroid =>
      Platform.isAndroid;

  static bool get isIOS => Platform.isIOS;
}
