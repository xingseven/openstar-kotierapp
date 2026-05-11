import QtQuick

QtObject {
    // ── 配色方案 ──
    readonly property color accent: "#66CCFF"
    readonly property color accentLight: "#8ED6FF"

    // 浅色主题
    readonly property color bgLight: "#F5F5F5"
    readonly property color surfaceLight: "#FFFFFF"
    readonly property color textLight: "#2D2D2D"
    readonly property color surfaceVariantLight: "#EEEEEE"
    readonly property color dividerLight: "#E0E0E0"

    // 深色主题
    readonly property color bgDark: "#1A1A2E"
    readonly property color surfaceDark: "#16213E"
    readonly property color textDark: "#FFFFFF"
    readonly property color surfaceVariantDark: "#0F3460"
    readonly property color dividerDark: "#2A2A4E"

    // 状态颜色
    readonly property color statusOnline: "#4CAF50"
    readonly property color statusDirect: "#4CAF50"
    readonly property color statusRelay: "#FFA726"
    readonly property color statusServer: "#42A5F5"
    readonly property color statusOffline: "#9E9E9E"
    readonly property color errorRed: "#EF5350"
    readonly property color successGreen: "#66BB6A"

    // 日志级别颜色
    readonly property color logDebug: "#9E9E9E"
    readonly property color logInfo: "#FFFFFF"
    readonly property color logWarn: "#FFA726"
    readonly property color logError: "#EF5350"

    // 字体大小
    readonly property int fontSizeSmall: 12
    readonly property int fontSizeBody: 14
    readonly property int fontSizeTitle: 16
    readonly property int fontSizeLarge: 18

    // 间距
    readonly property int spacingSmall: 4
    readonly property int spacingMedium: 8
    readonly property int spacingLarge: 12
    readonly property int spacingXLarge: 16

    // 圆角
    readonly property int radiusSmall: 4
    readonly property int radiusMedium: 8
    readonly property int radiusLarge: 12

    // 是否深色模式（由 C++ SettingsRepository 控制）
    property bool isDark: false

    // 便捷访问当前主题色
    readonly property color bg: isDark ? bgDark : bgLight
    readonly property color surface: isDark ? surfaceDark : surfaceLight
    readonly property color text: isDark ? textDark : textLight
    readonly property color surfaceVariant: isDark ? surfaceVariantDark : surfaceVariantLight
    readonly property color divider: isDark ? dividerDark : dividerLight
}
