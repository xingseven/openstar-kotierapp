# QtEasyTier Mobile 项目进度文档

> 更新时间: 2026-05-09
> 项目目标: Flutter 跨平台版本，支持 Web 浏览器预览 + Android/iOS 原生

---

## 一、项目概述

本项目旨在将 QtEasyTier 的 Qt/C++ 桌面界面迁移至 Flutter，实现：
1. **浏览器预览** - 方便 UI 开发和演示
2. **Android/iOS 双端** - 通过 FFI 调用 EasyTier 核心库
3. **架构解耦** - EasyTierService 抽象层，支持 Mock 和 Native 两种实现

---

## 二、当前完成进度

### 已完成 ✅

| 文件 | 说明 | 状态 |
|------|------|:----:|
| `pubspec.yaml` | Flutter 项目配置 | ✅ |
| `web/index.html` | Web 入口 HTML | ✅ |
| `analysis_options.yaml` | Lint 规则 | ✅ |
| `lib/main.dart` | 程序入口 | ✅ |
| `lib/app.dart` | App 根组件 + Breeze 主题 | ✅ |
| `lib/core/config/network_config.dart` | 网络配置数据模型 | ✅ |
| `lib/core/config/node_info.dart` | 节点信息数据模型 | ✅ |
| `lib/core/easy_tier_service.dart` | EasyTierService 抽象接口 + Web Mock 实现 | ✅ |
| `lib/core/native_easy_tier_service.dart` | FFI 原生实现（Android/iOS） | ✅ |
| `lib/widgets/custom_switch.dart` | 自定义开关控件 | ✅ |
| `lib/widgets/node_info_card.dart` | 节点信息卡片 | ✅ |
| `lib/widgets/server_dialog.dart` | 服务器选择对话框 | ✅ |
| `lib/pages/home_page.dart` | 底部导航框架（4 Tab） | ✅ |
| `lib/pages/network_config_page.dart` | 网络配置页面（完整） | ✅ |
| `lib/pages/one_click_page.dart` | 一键联机页面（房主/房客模式，Base32 编解码） | ✅ |
| `lib/pages/servers_page.dart` | 服务器收藏页面（CRUD + SharedPreferences 持久化） | ✅ |
| `lib/pages/settings_page.dart` | 设置页面（开机自启、自动回连、通知、日志、主题） | ✅ |

### 未完成 ❌

| 文件 | 说明 | 优先级 |
|------|------|:------:|
| `assets/` | 资源目录（占位） | 低 |
| `android/` | Android 原生配置（VpnService） | 高 |
| `ios/` | iOS 原生配置（NetworkExtension） | 高 |

---

## 三、架构设计

```
┌─────────────────────────────────────────────────────────┐
│                     Flutter UI Layer                      │
│  HomePage (底部导航)                                     │
│  ├── NetworkConfigPage  网络配置/节点监测                  │
│  ├── OneClickPage       一键联机（房主/房客）              │
│  ├── ServersPage        服务器收藏管理                    │
│  └── SettingsPage       全局设置                         │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│              EasyTierService 抽象层                       │
│  ├── WebEasyTierService   (Web 预览 / Mock 数据)          │
│  └── NativeEasyTierService (FFI 调用真实后端)             │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                    Platform Layer                         │
│  Web     → JavaScript Mock (已实现)                       │
│  Android → VpnService + libeasytier_ffi.so (待实现)       │
│  iOS     → NEPacketTunnelProvider + libeasytier_ffi.a     │
└─────────────────────────────────────────────────────────┘
```

### EasyTierService 接口定义

```dart
abstract class EasyTierService {
  String get platform;                              // 'web' | 'native'
  Future<bool> initialize();                        // 初始化
  Future<bool> parseConfig(String toml);            // 验证配置
  Future<EasyTierResult> startNetwork(NetworkConfig config); // 启动网络
  Future<EasyTierResult> stopNetwork(String instanceName);    // 停止网络
  Future<List<NodeInfo>> collectNodeInfos(String instanceName); // 收集节点
  Future<bool> stopAllNetworks();                   // 停止全部
  Future<List<NetworkConfig>> loadConfigs();        // 加载配置
  Future<bool> saveConfigs(List<NetworkConfig> configs); // 保存配置
  void dispose();
}
```

---

## 四、FFI 对接（移动端核心）

参考 `ThirdParty/EasyTier/include/easytier_ffi.h`，总共 5 个 C 函数：

```c
int parse_config(const char* cfg_str);                 // 验证 TOML
int run_network_instance(const char* cfg_str);          // 启动网络
int retain_network_instance(const char** names, size_t len); // 停止/管理
int collect_network_infos(KeyValuePair* infos, size_t max_len); // 收集节点
void get_error_msg(const char** out);                  // 错误信息
void free_string(const char* s);                      // 释放内存
```

Dart FFI 绑定已在 `native_easy_tier_service.dart` 中完整实现。

---

## 五、各页面功能清单

### network_config_page.dart ✅

- [x] 多配置管理（选项卡切换、新增、删除）
- [x] 基本设置（标签、主机名、网络名称、密钥、DHCP/IPv4）
- [x] 入口服务器管理（增删改对话框）
- [x] 高级设置折叠面板（KCP/QUIC/P2P/IPv6/加密/出口节点等 20+ 选项）
- [x] 启动/停止网络按钮
- [x] 节点监测定时器（3 秒轮询）
- [x] 配置保存/加载
- [x] 一键联机入口按钮

### one_click_page.dart ✅

- [x] 房主/房客模式切换（Toggle）
- [x] 房主模式：启动网络后生成 Base32 编码分享
- [x] 房客模式：输入 Base32 编码解析并加入网络
- [x] 编码复制到剪贴板
- [x] 启动/停止/离开网络

### servers_page.dart ✅

- [x] 服务器列表（卡片展示）
- [x] 添加服务器（名称 + URL）
- [x] 编辑服务器
- [x] 删除服务器（默认服务器不可删除）
- [x] SharedPreferences 持久化
- [x] 下拉刷新

### settings_page.dart ✅

- [x] 主题设置（跟随系统 / 深色模式）
- [x] 开机自启开关
- [x] 自动回连开关
- [x] 通知设置（连接成功/断开）
- [x] 日志级别选择（debug/info/warn/error）
- [x] 关于信息（版本/平台/后端）
- [x] 清除所有数据

---

## 六、运行项目

### Web 预览（已完成，可直接运行）

```bash
cd d:\code\code\qt_easytier_mobile
flutter run -d chrome
```

> Web 模式使用 WebEasyTierService（Mock 数据），可预览 UI 布局和交互逻辑。

### Android 构建（需先完成 VpnService 配置）

```bash
flutter build apk --release
```

### iOS 构建（需先完成 NetworkExtension 配置）

```bash
flutter build ios --release
```

---

## 七、下一步工作

### 优先级：高

1. **Android VpnService** - 实现 `android/app/src/main/kotlin/.../EasyTierVpnService.kt`
2. **交叉编译 easytier_ffi.so** - `cargo build --target aarch64-linux-android --release`

### 优先级：低

3. **iOS NetworkExtension** - 实现 `ios/Runner/EasyTierPacketTunnel.swift`
4. **assets 资源** - 图标、字体等

---

## 八、参考项目

- **QtEasyTier 桌面端**: `d:\code\code\qt-easy-tier\`
- **EasyTier 官方**: https://github.com/EasyTier/EasyTier
- **EasyTier FFI 头文件**: `ThirdParty/EasyTier/include/easytier_ffi.h`
- **FFI 工作类实现**: `SRC/ETRunWorker.cpp` (75 行核心逻辑)

---

## 九、关键文件路径

```
d:\code\code\qt_easytier_mobile\
├── pubspec.yaml
├── web/
│   └── index.html
└── lib/
    ├── main.dart
    ├── app.dart
    ├── core/
    │   ├── config/
    │   │   ├── network_config.dart   ← 对应 Qt 的 NetworkConf
    │   │   └── node_info.dart        ← 对应 Qt 的 NodeInfo
    │   ├── easy_tier_service.dart     ← 接口 + Web Mock
    │   └── native_easy_tier_service.dart  ← FFI 实现
    ├── pages/
    │   ├── home_page.dart             ← 底部导航
    │   ├── network_config_page.dart   ← ✅ 已完成
    │   ├── one_click_page.dart        ← ✅ 已完成
    │   ├── servers_page.dart          ← ✅ 已完成
    │   └── settings_page.dart         ← ✅ 已完成
    └── widgets/
        ├── custom_switch.dart
        ├── node_info_card.dart
        └── server_dialog.dart
```

---

*文档由 AI 生成，如有疑问请查看源码或联系开发者*
