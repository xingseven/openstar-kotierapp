# EasyTier Android Kotlin → C++/Qt 重构方案

## 一、重构背景

当前项目 qt_easytier_mobile 是一个 Kotlin + Jetpack Compose 实现的 Android 应用，作为 EasyTier 组网工具的移动端。项目名中的 "qt" 暗示了 Qt 是原始设计目标，但目前使用的是纯 Android 技术栈。

重构目标：将整个项目迁移为 **C++/Qt 6 (QML)** 实现，保留 Rust 原生网络引擎 (`libeasytier_ffi.so`) 不变，实现跨平台基础。

**技术栈说明**：
- **C++** — 实现语言，负责所有业务逻辑层（数据模型、服务层、网络请求、设置持久化等）
- **Qt 6 (QML)** — 界面框架，负责 UI 层（页面、组件、导航、主题）
- **Rust (libeasytier_ffi.so)** — 原生网络引擎，C++ 通过 `QLibrary` 直接调用其 C ABI

关系：**C++ 写底层逻辑，QML 写界面**，两者通过属性绑定和信号槽通信。

---

## 二、当前项目规模与构成

| 模块 | 文件数 | 代码行数 | 重构方案 |
|------|--------|----------|----------|
| UI 页面 (pages) | 6 | 1,899 | QML 重写 |
| 服务层 (service) | 5 | 597 | C++ 重写 |
| UI 组件 (components) | 3 | 228 | QML 重写 |
| 数据模型 (data) | 4 | 225 | C++ struct/class |
| JNI 接口 | 1 | 23 | 移除 (直接用 C ABI) |
| 入口/主题 | 3 | 90 | C++ + QML |
| **总计** | **22** | **3,062** | |

---

## 三、目标架构

```
┌───────────────────────────────────────────────────┐
│               QML UI 层 (Qt Quick Controls 2)      │
│   MainWindow.qml                                   │
│   ├─ NetworkConfigPage.qml     (配置管理)           │
│   ├─ OneClickPage.qml          (一键联机)           │
│   ├─ ServersPage.qml           (服务器管理)          │
│   ├─ SettingsPage.qml          (设置)               │
│   └─ LogPage.qml               (日志)               │
│   components/   (CompactTopBar, NodeInfoCard 等)    │
└───────────────────────┬───────────────────────────┘
                        │ C++ 属性绑定 / 信号槽
┌───────────────────────▼───────────────────────────┐
│           C++ 业务逻辑层 (Qt Core + Network)        │
│   EasyTierService     — 核心服务单例                 │
│   PublicNodeService   — HTTP 公共节点获取            │
│   LogService          — 内存环形日志                 │
│   SettingsRepository  — QSettings 持久化             │
│   Base32              — 联机码编解码                 │
│   Models              — NetworkConfig, NodeInfo 等   │
│   JniBridge           — QJniObject VPN 桥接          │
└───────────────────────┬───────────────────────────┘
                        │ QLibrary::resolve() / dlopen
┌───────────────────────▼───────────────────────────┐
│           Rust 原生引擎 (保留不变)                   │
│   libeasytier_ffi.so  — 6 个 C ABI 函数             │
│   (set_tun_fd, parse_config, run_network_instance,  │
│    retain_network_instance, collect_network_infos,  │
│    get_error_msg / free_string)                     │
└───────────────────────┬───────────────────────────┘
                        │ QJniObject / JNI
┌───────────────────────▼───────────────────────────┐
│          Android Java 薄层 (最小残留)                │
│   VpnService.java     — TUN fd 获取 (约 80 行)      │
└───────────────────────────────────────────────────┘
```

### 架构要点

- **Rust FFI 层保持不变**：`libeasytier_ffi.so` 导出的 6 个 C ABI 函数通过 `QLibrary::resolve()` 直接调用
- **移除 JNI 包装层**：不再需要 `libeasytier_android_jni.so` 和 `EasyTierJNI.kt`
- **Android 残留降到最低**：仅保留 VpnService (~80 行 Java) 获取 TUN fd
- **跨平台基础**：业务逻辑和 UI 层 (QML) 天然跨平台，仅 VpnService 是 Android 特有

---

## 四、详细模块迁移方案

### 4.1 数据模型层

| Kotlin 文件 | C++ 映射 | 说明 |
|---|---|---|
| `NetworkConfig.kt` (30 字段) | `src/data/NetworkConfig.h` struct + `toToml()`, `toJson()`, `fromJson()` | 核心配置模型，生成 TOML 字符串传给 Rust |
| `NodeInfo.kt` | `src/data/NodeInfo.h` struct + `ConnectionType` enum | 节点信息，含流量格式化 |
| `ServerEntry.kt` + `PublicNode` | `src/data/ServerEntry.h`, `src/data/PublicNode.h` | 服务器条目 + 公共节点 |
| `LogEntry.kt` + `LogLevel` | `src/data/LogEntry.h` enum class | 日志条目 |

**关键迁移点**：`NetworkConfig::toToml()` 必须与原 Kotlin 版本的 TOML 输出完全一致，否则 Rust 端解析会失败。

### 4.2 服务层

| Kotlin 文件 | C++ 映射 | 关键变更 |
|---|---|---|
| `EasyTierService.kt` (object 单例) | `src/service/EasyTierService.hpp` 单例类 | 协程 → `QTimer` + `QtConcurrent::run`；JNI → `QLibrary` 直接调 C ABI |
| `EasyTierVpnService.kt` (VpnService) | `android/VpnService.java` + `src/JniBridge.cpp` | 仅保留 Java VpnService 薄层获取 TUN fd |
| `PublicNodeService.kt` (OkHttp) | `src/service/PublicNodeService.hpp` | OkHttp → `QNetworkAccessManager` |
| `LogService.kt` (环形日志) | `src/service/LogService.hpp` | 信号槽替代轮询 |
| `SettingsRepository.kt` (SharedPrefs) | `src/service/SettingsRepository.hpp` | SharedPreferences → `QSettings` |

#### 4.2.1 EasyTierService 核心接口设计

```cpp
class EasyTierService : public QObject {
    Q_OBJECT
public:
    static EasyTierService* instance();

    // Rust FFI 初始化
    bool initialize();           // QLibrary::load("easytier_ffi")
    
    // 网络实例管理
    bool parseConfig(const QString &tomlConfig);
    EasyTierResult startNetwork(const NetworkConfig &config);
    EasyTierResult stopNetwork(const QString &instanceName);
    bool stopAllNetworks();
    
    // 节点监控
    QList<NodeInfo> collectNodeInfos(const QString &instanceName);
    void startMonitoring(const QString &instanceName, int intervalMs = 3000);
    void stopMonitoring();
    
    // VPN 管理
    void startVpnService(const QString &instanceName, const QString &ipv4,
                         int prefix, const QStringList &routes);
    void stopVpnService();

signals:
    void nodesUpdated(QList<NodeInfo> nodes);
    void networkStatusChanged(bool running);
    void errorOccurred(QString message);
    
private:
    // Rust C ABI 函数指针
    using SetTunFd = int(*)(const char*, int);
    using ParseConfig = int(*)(const char*);
    using RunNetworkInstance = int(*)(const char*);
    using RetainNetworkInstance = int(*)(const char**, size_t);
    using CollectNetworkInfos = int(*)(KeyValuePair*, size_t);
    using GetErrorMsg = void(*)(char**);
    using FreeString = void(*)(const char*);
    
    // QLibrary 句柄
    QLibrary m_ffiLib;
    QTimer *m_monitorTimer = nullptr;
};
```

#### 4.2.2 JNI/VPN 桥接设计

```java
// android/VpnService.java (约 80 行)
public class EasyTierVpnService extends VpnService {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String instanceName = intent.getStringExtra("instanceName");
        String ipv4 = intent.getStringExtra("ipv4");
        int prefix = intent.getIntExtra("prefix", 24);
        String[] routes = intent.getStringArrayExtra("routes");
        
        Builder builder = new Builder();
        builder.setMtu(1500);
        builder.addAddress(ipv4, prefix);
        for (String route : routes) {
            String[] parts = route.split("/");
            builder.addRoute(parts[0], Integer.parseInt(parts[1]));
        }
        builder.addDnsServer("223.5.5.5");
        builder.setSession("EasyTier");
        
        ParcelFileDescriptor vpnInterface = builder.establish();
        nativeSetTunFd(instanceName, vpnInterface.getFd());
        vpnInterface.close();
        return START_STICKY;
    }
    
    private native void nativeSetTunFd(String instanceName, int fd);
}
```

```cpp
// src/JniBridge.cpp — Qt 侧通过 QJniObject 启动 VpnService
void JniBridge::startVpnService(const QString &instanceName, const QString &ipv4,
                                 int prefix, const QStringList &routes) {
    auto *androidApp = qGuiApp->nativeInterface<QNativeInterface::QAndroidApplication>();
    QJniObject context = androidApp->context();
    
    QJniObject intent("android/content/Intent");
    intent.callObjectMethod("setClassName",
        "(Landroid/content/Context;Ljava/lang/String;)Landroid/content/Intent;",
        context.object(),
        QJniObject::fromString("com.easytier.service.EasyTierVpnService").object());
    intent.callObjectMethod("putExtra", "(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;",
        QJniObject::fromString("instanceName").object(),
        QJniObject::fromString(instanceName).object());
    // ... 更多参数
    
    context.callMethod<void>("startForegroundService",
        "(Landroid/content/Intent;)V", intent.object());
}
```

### 4.3 UI 层 (QML)

| 页面 | QML 文件 | 关键 QML 组件 | 对应 Compose 特性 |
|---|---|---|---|
| 主导航 | `qml/MainWindow.qml` | `SwipeView` + `TabBar` | `Scaffold` + 底部导航 |
| 网络配置 | `qml/pages/NetworkConfigPage.qml` | `TabBar`(配置标签) + `Flickable` + 大量 `Switch` | `LazyRow` + `Column` + `CustomSwitch` |
| 一键联机 | `qml/pages/OneClickPage.qml` | `TabButton`(双模式) + 状态文字 | ModeButton + HostMode/GuestMode |
| 服务器 | `qml/pages/ServersPage.qml` | `ListView` + 折叠面板 | `LazyColumn` + 可折叠 Card |
| 设置 | `qml/pages/SettingsPage.qml` | `ListView` + `Switch` + `ComboBox` | `Column` + SectionCard + 开关 |
| 日志 | `qml/pages/LogPage.qml` | `ListView` + 颜色代理 | `LazyColumn` + 着色 |

### 4.4 UI 组件

| 组件 | 作用 | QML 方案 |
|---|---|---|
| `CompactTopBar` | 顶部导航栏 | 自定义 `Item` + `Rectangle` + `Row` |
| `NodeInfoCard` | 节点信息卡片 | `Rectangle` + `Row` + 状态灯 (Canvas) |
| `CustomSwitch` | 带标签的开关 | `Row` + `Text` + `Switch` |
| `SectionCard` | 分区卡片 | `Rectangle` + `Column` |

---

## 五、颜色/主题映射

| 原始色 | 值 | Qt/QML 映射 |
|---|---|---|
| Accent | `#66CCFF` | `accent: "#66CCFF"` |
| 浅色背景 | `#F5F5F5` | `bgLight: "#F5F5F5"` |
| 深色背景 | `#1A1A2E` | `bgDark: "#1A1A2E"` |
| 浅色 Surface | `#FFFFFF` | `surfaceLight: "#FFFFFF"` |
| 深色 Surface | `#16213E` | `surfaceDark: "#16213E"` |
| 浅色文字 | `#2D2D2D` | `textLight: "#2D2D2D"` |
| 深色文字 | `#FFFFFF` | `textDark: "#FFFFFF"` |
| 浅色 SurfaceVariant | `#EEEEEE` | `surfaceVariantLight: "#EEEEEE"` |
| 深色 SurfaceVariant | `#0F3460` | `surfaceVariantDark: "#0F3460"` |

---

## 六、硬编码常量对照

| 常量 | 值 | 位置 |
|---|---|---|
| MTU | 1500 | VpnService |
| DNS | 223.5.5.5 | VpnService |
| 默认服务器 | `wss://qtet-public.070219.xyz` | NetworkConfig |
| 默认监听器 | `["tcp://0.0.0.0:11010", "udp://0.0.0.0:11010"]` | NetworkConfig |
| 实例名前缀 | `EasyTierET-` | NetworkConfig |
| 监控间隔 | 3000ms | EasyTierService |
| 日志上限 | 2000 条 | LogService |
| 轮询超时 | 30 次 × 500ms | NetworkConfigPage |
| 公共节点 URL | `https://info.qtet.cn/uptime/status/easytier` | PublicNodeService |
| 心跳 URL | `https://info.qtet.cn/uptime/api/status-page/heartbeat/easytier` | PublicNodeService |

---

## 七、项目目录结构

```
android_app/                      ← 当前 Kotlin 项目（保留为参考，逐步弃用）
qt_app/                           ← 新 Qt 项目（新建）
├── CMakeLists.txt                # Qt 6 CMake 构建配置
├── main.cpp                      # 应用入口
├── qtquickcontrols2.conf         # Qt Quick 样式配置
├── qml/
│   ├── MainWindow.qml            # 主窗口 + 底部导航
│   ├── theme/
│   │   ├── Theme.qml             # 颜色/字体定义
│   │   └── ThemeSettings.qml     # 浅色/深色切换
│   ├── pages/
│   │   ├── NetworkConfigPage.qml
│   │   ├── OneClickPage.qml
│   │   ├── ServersPage.qml
│   │   ├── SettingsPage.qml
│   │   └── LogPage.qml
│   └── components/
│       ├── CompactTopBar.qml
│       ├── NodeInfoCard.qml
│       ├── CustomSwitch.qml
│       └── SectionCard.qml
├── src/
│   ├── main.cpp
│   ├── application/
│   │   ├── EasyTierApplication.hpp/.cpp   # QApplication 子类
│   │   └── MainWindowController.hpp/.cpp  # QML 窗口控制器
│   ├── service/
│   │   ├── EasyTierService.hpp/.cpp       # 核心服务单例
│   │   ├── PublicNodeService.hpp/.cpp     # HTTP 公共节点
│   │   ├── LogService.hpp/.cpp            # 内存日志
│   │   └── SettingsRepository.hpp/.cpp    # QSettings 持久化
│   ├── data/
│   │   ├── NetworkConfig.hpp/.cpp         # 30 字段 + TOML/JSON 序列化
│   │   ├── NodeInfo.hpp                   # 节点信息
│   │   ├── ServerEntry.hpp                # 服务器条目
│   │   ├── PublicNode.hpp                 # 公共节点
│   │   └── LogEntry.hpp                   # 日志条目
│   ├── utils/
│   │   ├── Base32.hpp/.cpp                # 联机码编解码
│   │   └── JniBridge.hpp/.cpp             # Qt Android JNI 封装
│   └── ffi/
│       ├── RustFfi.hpp                    # Rust C ABI 函数声明
│       └── RustFfi.cpp                    # QLibrary::resolve 加载
├── android/
│   ├── AndroidManifest.xml
│   ├── res/
│   │   └── values/
│   │       ├── strings.xml
│   │       ├── colors.xml
│   │       └── themes.xml
│   └── src/com/easytier/service/
│       └── EasyTierVpnService.java        # VpnService 薄层
├── libs/
│   └── arm64-v8a/
│       └── libeasytier_ffi.so             # Rust 原生库（从外部复制）
└── i18n/
    ├── qt_app_zh_CN.ts
    └── qt_app_en.ts
```

---

## 八、分阶段实施计划

### Phase 1: 基础设施搭建 (Day 1-2)
1. 创建 `qt_app/` 项目骨架和 CMakeLists.txt
2. 搭建 Qt 6 开发环境（确认 NDK/SDK 路径）
3. 实现 Rust FFI 加载层 (`RustFfi.cpp`)
4. 实现 `EasyTierService` 单例（调用 FFI 函数）
5. 实现 `LogService` 环形日志
6. 实现 `SettingsRepository` (QSettings 封装)
7. 验证：初始化 + 解析配置 + 启动/停止网络实例

### Phase 2: 数据模型 + Base32 (Day 2-3)
1. 实现所有数据模型 struct (NetworkConfig, NodeInfo, ServerEntry, PublicNode, LogEntry)
2. 实现 `NetworkConfig::toToml()`（必须与 Kotlin 版本完全对齐）
3. 实现 `NetworkConfig::toJson()` / `fromJson()`
4. 实现 Base32 编解码（与 Qt 版兼容）
5. 实现 `PublicNodeService` (QNetworkAccessManager)
6. 验证：TOML 输出与 Kotlin 版本对比一致，Base32 编解码测试

### Phase 3: QML UI 主体 (Day 4-6)
1. 实现主题系统 (`Theme.qml`) 和颜色常量
2. 实现 `CompactTopBar` 组件
3. 实现 `CustomSwitch` 组件
4. 实现 `MainWindow.qml`（底部导航 + 页面切换）
5. 实现 `SettingsPage.qml`（所有开关 + 日志级别 + 清除数据）
6. 实现 `LogPage.qml`（日志列表 + 自动滚动 + 级别着色）
7. 实现 `ServersPage.qml`（公共节点 + 收藏 + 管理对话框）
8. 实现 `OneClickPage.qml`（双模式 + 联机码生成/解析 + VPN 启动）
9. 实现 `NetworkConfigPage.qml`（配置管理 + 多标签 + 节点监控 + VPN 启动）
10. 验证：所有页面可导航，设置持久化正常工作

### Phase 4: VpnService + Android 集成 (Day 6-7)
1. 实现 `EasyTierVpnService.java`（Android VpnService 薄层）
2. 实现 `JniBridge.cpp`（QJniObject 调用 Java 侧）
3. 实现 VPN 授权流程 (VpnService.prepare())
4. 配置 `AndroidManifest.xml`（权限、Service 声明）
5. 配置 CMakeLists.txt 的 Android 相关设置
6. 验证：VPN 启动/停止，TUN fd 正确传递给 Rust 层

### Phase 5: 集成测试与打磨 (Day 7-8)
1. 端到端测试：
   - 新建配置 → 保存 → 重新加载 → 确认一致
   - 启动网络 → 等待 IP → 启动 VPN → 停止
   - 一键联机 (创建 + 加入)
   - 服务器管理 (添加/删除/从公共节点添加)
   - 设置所有开关 → 重启应用 → 确认持久化
   - 日志滚动/清除
2. 深色/浅色主题切换
3. 修复 bug
4. 中文字符串提取到 .ts 翻译文件

---

## 九、技术风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| TOML 输出与 Kotlin 版不一致 | Rust 解析失败 | 对照 Kotlin 的 `toToml()` 逐行比对，单元测试验证 |
| VpnService.prepare() 流程在 Qt 中复杂 | VPN 无法启动 | 参考成熟项目 (AmneziaVPN) 的 QJniObject 实现模式 |
| QML ListView 性能 | 节点列表卡顿 | 使用 `delegate` 缓存 + 控制更新频率 |
| Rust FFI 函数线程安全 | 崩溃 | 所有 FFI 调用在独立线程执行，通过信号回传结果 |
| Qt for Android 的 Service 生命周期 | 后台被杀 | 使用 Foreground Service + 通知 |

---

## 十、验证方案

1. **编译验证**: `cmake --build . --target apk` 通过
2. **TOML 一致性**: C++ `toToml()` 输出与 Kotlin `toToml()` 输出字符串完全一致
3. **Base32 编解码**: 编码后再解码得到原始数据
4. **FFI 调用**: `parse_config()` 返回 0，`run_network_instance()` 返回 0
5. **VPN 流程**: `VpnService.prepare()` → `establish()` → TUN fd 传递 → 网络可达
6. **节点监控**: 3 秒定时器正确拉取并解析节点 JSON 数据
7. **设置持久化**: 修改设置 → 重启应用 → 设置保留
8. **深色/浅色主题**: 切换后所有页面正确应用对应配色
