#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQmlComponent>
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

    // 加载 Theme.qml 并注册为全局上下文属性（替代 pragma Singleton）
    QQmlComponent themeComponent(&engine,
        QUrl(QStringLiteral("qrc:/com/easytier/qml/theme/Theme.qml")));
    QObject* themeObject = themeComponent.create();
    if (themeObject) {
        engine.rootContext()->setContextProperty("Theme", themeObject);
    }

    // 加载主 QML 文件
    const QUrl url(QStringLiteral("qrc:/com/easytier/qml/MainWindow.qml"));
    QObject::connect(&engine, &QQmlApplicationEngine::objectCreated,
        &app, [url](QObject *obj, const QUrl &objUrl) {
            if (!obj && url == objUrl)
                QCoreApplication::exit(-1);
        }, Qt::ConnectionType::DirectConnection);

    engine.load(url);

    return app.exec();
}
