import QtQuick

// 浅色/深色主题设置页面
QtObject {
    property bool followSystem: true
    property bool darkMode: false

    property bool effectiveDark: {
        if (followSystem) {
            // 后续可接入系统主题检测
            return darkMode
        }
        return darkMode
    }

    onEffectiveDarkChanged: {
        Theme.isDark = effectiveDark
    }
}
