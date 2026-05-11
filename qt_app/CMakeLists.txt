cmake_minimum_required(VERSION 3.21)
project(EasyTier VERSION 1.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 REQUIRED COMPONENTS Core Quick Network Concurrent)

# ── 源文件 ──
set(SOURCES
    src/main.cpp
    src/application/EasyTierApplication.cpp
    src/application/MainWindowController.cpp
    src/ffi/RustFfi.cpp
    src/data/NetworkConfig.cpp
    src/service/EasyTierService.cpp
    src/service/LogService.cpp
    src/service/SettingsRepository.cpp
    src/service/PublicNodeService.cpp
    src/utils/Base32.cpp
    src/utils/JniBridge.cpp
)

set(HEADERS
    src/application/EasyTierApplication.hpp
    src/application/MainWindowController.hpp
    src/ffi/RustFfi.hpp
    src/data/NetworkConfig.hpp
    src/data/NodeInfo.hpp
    src/data/ServerEntry.hpp
    src/data/PublicNode.hpp
    src/data/LogEntry.hpp
    src/service/EasyTierService.hpp
    src/service/LogService.hpp
    src/service/SettingsRepository.hpp
    src/service/PublicNodeService.hpp
    src/utils/Base32.hpp
    src/utils/JniBridge.hpp
)

# ── 可执行文件 ──
qt_add_executable(EasyTier
    ${SOURCES}
    ${HEADERS}
    qtquickcontrols2.conf
)

target_include_directories(EasyTier PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_link_libraries(EasyTier PRIVATE
    Qt6::Core
    Qt6::Quick
    Qt6::Network
    Qt6::Concurrent
)

# ── QML 文件作为资源嵌入 ──
qt_add_resources(EasyTier "qml"
    PREFIX "/com/easytier/qml"
    FILES
        qml/MainWindow.qml
        qml/theme/Theme.qml
        qml/theme/ThemeSettings.qml
        qml/pages/NetworkConfigPage.qml
        qml/pages/OneClickPage.qml
        qml/pages/ServersPage.qml
        qml/pages/SettingsPage.qml
        qml/pages/LogPage.qml
        qml/components/CompactTopBar.qml
        qml/components/NodeInfoCard.qml
        qml/components/CustomSwitch.qml
        qml/components/SectionCard.qml
)

# ── Android 特有设置 ──
if(ANDROID)
    set(ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android")

    # 打包 Rust 原生库
    set(EXTRA_LIBS
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/arm64-v8a/libeasytier_ffi.so
    )
    set_target_properties(EasyTier PROPERTIES
        QT_ANDROID_EXTRA_LIBS "${EXTRA_LIBS}"
        QT_ANDROID_ABIS "arm64-v8a"
    )
endif()

# ── 安装 ──
install(TARGETS EasyTier
    BUNDLE DESTINATION .
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
)
