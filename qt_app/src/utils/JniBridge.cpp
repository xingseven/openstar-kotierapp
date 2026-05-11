#include "JniBridge.hpp"
#include "service/LogService.hpp"

#ifdef Q_OS_ANDROID
#include <QJniObject>
#include <QJniEnvironment>
#include <QGuiApplication>
#endif

void JniBridge::startVpnService(const QString& instanceName, const QString& ipv4,
                                 int prefix, const QStringList& routes) {
#ifdef Q_OS_ANDROID
    // 获取 Android Activity 上下文
    QJniObject activity = QJniObject::callStaticObjectMethod(
        "org/qtproject/qt/android/QtNative", "activity",
        "()Landroid/app/Activity;");

    if (!activity.isValid()) {
        LogService::instance()->error("JniBridge: failed to get Android activity", "JNI");
        return;
    }

    // 创建 Intent
    QJniObject intent("android/content/Intent");
    intent.callObjectMethod("setClassName",
        "(Landroid/content/Context;Ljava/lang/String;)Landroid/content/Intent;",
        activity.object(),
        QJniObject::fromString("com.easytier.service.EasyTierVpnService").object());

    // 设置基本参数
    intent.callObjectMethod("putExtra",
        "(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;",
        QJniObject::fromString("instance_name").object(),
        QJniObject::fromString(instanceName).object());
    intent.callObjectMethod("putExtra",
        "(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;",
        QJniObject::fromString("ipv4").object(),
        QJniObject::fromString(ipv4).object());

    intent.callMethod<jboolean>("putExtra",
        "(Ljava/lang/String;I)Landroid/content/Intent;",
        QJniObject::fromString("prefix").object(),
        static_cast<jint>(prefix));

    // 构造 String[] 传递 routes
    {
        QJniEnvironment env;
        jclass stringClass = env.findClass("java/lang/String");
        jobjectArray routeArray = env->NewObjectArray(routes.size(), stringClass, nullptr);
        for (int i = 0; i < routes.size(); ++i) {
            QJniObject routeStr = QJniObject::fromString(routes[i]);
            env->SetObjectArrayElement(routeArray, i, routeStr.object());
        }
        intent.callObjectMethod("putExtra",
            "(Ljava/lang/String;[Ljava/lang/String;)Landroid/content/Intent;",
            QJniObject::fromString("routes").object(),
            routeArray);
    }

    // 启动前台服务
    activity.callMethod<void>("startForegroundService",
        "(Landroid/content/Intent;)V", intent.object());

    LogService::instance()->info("JniBridge: VPN service started", "JNI");
#else
    Q_UNUSED(instanceName)
    Q_UNUSED(ipv4)
    Q_UNUSED(prefix)
    Q_UNUSED(routes)
    LogService::instance()->warn("JniBridge: VpnService not available on desktop", "JNI");
#endif
}

void JniBridge::stopVpnService() {
#ifdef Q_OS_ANDROID
    QJniObject activity = QJniObject::callStaticObjectMethod(
        "org/qtproject/qt/android/QtNative", "activity",
        "()Landroid/app/Activity;");

    if (!activity.isValid()) return;

    QJniObject intent("android/content/Intent");
    intent.callObjectMethod("setClassName",
        "(Landroid/content/Context;Ljava/lang/String;)Landroid/content/Intent;",
        activity.object(),
        QJniObject::fromString("com.easytier.service.EasyTierVpnService").object());

    activity.callMethod<void>("stopService",
        "(Landroid/content/Intent;)V", intent.object());

    LogService::instance()->info("JniBridge: VPN service stopped", "JNI");
#endif
}
