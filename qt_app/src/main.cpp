#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQmlComponent>
#include <QDebug>
#include "application/EasyTierApplication.hpp"
#include "application/MainWindowController.hpp"
#include "service/EasyTierService.hpp"
#include "service/SettingsRepository.hpp"

int main(int argc, char *argv[]) {
    EasyTierApplication app(argc, argv);

    MainWindowController mainController;

    QQmlApplicationEngine engine;

    // 注册 C++ 对象到 QML 上下文
    engine.rootContext()->setContextProperty("mainController", &mainController);
    engine.rootContext()->setContextProperty("settingsRepo",
        app.settingsRepository());
    engine.rootContext()->setContextProperty("easyTierService",
        EasyTierService::instance());

    // 加载 Theme.qml 并注册为全局上下文属性
    QQmlComponent themeComponent(&engine,
        QUrl(QStringLiteral("qrc:/com/easytier/qml/theme/Theme.qml")));
    QObject* themeObject = themeComponent.create();
    if (themeObject) {
        engine.rootContext()->setContextProperty("Theme", themeObject);
    } else {
        qWarning() << "[EasyTier] Failed to load Theme.qml, errors:" << themeComponent.errors();
        // 创建一个备用 Theme 避免白屏
        QQmlComponent fallback(&engine);
        fallback.setData(
            "import QtQuick\nQtObject {\n"
            "  property bool isDark: false\n"
            "  readonly property color accent: \"#66CCFF\"\n"
            "  readonly property color surface: \"#FFFFFF\"\n"
            "  readonly property color bg: \"#F5F5F5\"\n"
            "  readonly property color text: \"#2D2D2D\"\n"
            "  readonly property color surfaceVariant: \"#EEEEEE\"\n"
            "  readonly property color divider: \"#E0E0E0\"\n"
            "  readonly property int fontSizeSmall: 12\n"
            "  readonly property int fontSizeBody: 14\n"
            "  readonly property int fontSizeTitle: 16\n"
            "  readonly property int fontSizeLarge: 18\n"
            "  readonly property int radiusSmall: 4\n"
            "  readonly property int radiusMedium: 8\n"
            "  readonly property int radiusLarge: 12\n"
            "}\n", QUrl(QStringLiteral("qrc:/com/easytier/qml/theme/FallbackTheme.qml")));
        QObject* fallbackObj = fallback.create();
        if (fallbackObj) {
            engine.rootContext()->setContextProperty("Theme", fallbackObj);
            qWarning() << "[EasyTier] Using fallback Theme";
        }
    }

    // 加载主 QML 文件
    const QUrl url(QStringLiteral("qrc:/com/easytier/qml/MainWindow.qml"));
    QObject::connect(&engine, &QQmlApplicationEngine::objectCreated,
        &app, [url, &engine](QObject *obj, const QUrl &objUrl) {
            if (!obj && url == objUrl) {
                qWarning() << "[EasyTier] MainWindow.qml failed to load!";
                for (auto err : engine.errors())
                    qWarning() << "[EasyTier]   " << err;
                QCoreApplication::exit(-1);
            }
        }, Qt::ConnectionType::DirectConnection);

    engine.load(url);

    return app.exec();
}
