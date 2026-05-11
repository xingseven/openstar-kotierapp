#include "EasyTierApplication.hpp"
#include "service/EasyTierService.hpp"
#include "service/LogService.hpp"

EasyTierApplication::EasyTierApplication(int& argc, char** argv)
    : QGuiApplication(argc, argv)
{
    setApplicationName("EasyTier");
    setApplicationVersion("1.1.0");
    setOrganizationName("EasyTier");

    // 初始化设置仓库
    m_settingsRepo = new SettingsRepository(this);

    // 初始化日志
    LogService::instance()->info("EasyTier starting...", "App");

    // 初始化 EasyTier 服务（加载 Rust FFI）
    bool ok = EasyTierService::instance()->initialize();
    if (ok) {
        LogService::instance()->info("EasyTier service initialized", "App");
    } else {
        LogService::instance()->error("EasyTier service initialization FAILED", "App");
    }
}

EasyTierApplication::~EasyTierApplication() {
    LogService::instance()->info("EasyTier shutting down", "App");
}
