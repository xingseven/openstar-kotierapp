#ifndef JNIBRIDGE_HPP
#define JNIBRIDGE_HPP

#include <QString>
#include <QStringList>

// Android JNI 桥接 —— 调用 Java 侧的 VpnService 等 Android 特有 API
// 桌面平台：这些函数为空实现

class JniBridge {
public:
    // 启动 VPN 服务（Android 专用）
    static void startVpnService(const QString& instanceName, const QString& ipv4,
                                 int prefix, const QStringList& routes);

    // 停止 VPN 服务
    static void stopVpnService();

    // 请求 VPN 授权（返回 Intent，需在 Activity 中处理）
    // 此方法需要从 QML/UI 层调用，处理 Activity result
};

#endif // JNIBRIDGE_HPP
