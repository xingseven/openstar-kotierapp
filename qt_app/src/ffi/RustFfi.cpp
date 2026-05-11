#include "RustFfi.hpp"
#include <QDebug>
#include <QString>
#include <QByteArray>
#include <QFile>
#include <QDir>

#ifdef Q_OS_ANDROID
#include <dlfcn.h>
#else
#include <QLibrary>
#endif

RustFfi::RustFfi() {}

RustFfi::~RustFfi() {
    unload();
}

bool RustFfi::load(const QString& libraryPath) {
    if (m_handle) {
        qWarning() << "RustFfi: already loaded";
        return true;
    }

#ifdef Q_OS_ANDROID
    // Android: 使用 dlopen 加载 libeasytier_ffi.so
    // 该库已通过 QT_ANDROID_EXTRA_LIBS 打包到 APK
    QByteArray pathBytes = libraryPath.toLocal8Bit();
    m_handle = dlopen(pathBytes.constData(), RTLD_NOW | RTLD_GLOBAL);
    if (!m_handle) {
        // 尝试不指定路径（已在 System.loadLibrary 中加载）
        m_handle = dlopen("libeasytier_ffi.so", RTLD_NOW | RTLD_GLOBAL);
    }
    if (!m_handle) {
        qCritical() << "RustFfi: dlopen failed:" << dlerror();
        return false;
    }

    auto resolve = [this](const char* name) -> void* {
        return dlsym(m_handle, name);
    };
#else
    // 桌面环境：使用 QLibrary 加载
    QLibrary lib(libraryPath);
    if (!lib.load()) {
        qCritical() << "RustFfi: load failed:" << lib.errorString();
        return false;
    }
    m_handle = reinterpret_cast<void*>(&lib); // 存储引用

    auto resolve = [&lib](const char* name) -> void* {
        return reinterpret_cast<void*>(lib.resolve(name));
    };
#endif

    // 解析所有函数指针
    m_setTunFd = reinterpret_cast<SetTunFd>(resolve("set_tun_fd"));
    m_getErrorMsg = reinterpret_cast<GetErrorMsgFn>(resolve("get_error_msg"));
    m_freeString = reinterpret_cast<FreeStringFn>(resolve("free_string"));
    m_parseConfig = reinterpret_cast<ParseConfigFn>(resolve("parse_config"));
    m_runNetworkInstance = reinterpret_cast<RunNetworkInstanceFn>(resolve("run_network_instance"));
    m_retainNetworkInstance = reinterpret_cast<RetainNetworkInstanceFn>(resolve("retain_network_instance"));
    m_collectNetworkInfos = reinterpret_cast<CollectNetworkInfosFn>(resolve("collect_network_infos"));

    // 验证所有符号都已解析
    if (!m_setTunFd || !m_getErrorMsg || !m_freeString ||
        !m_parseConfig || !m_runNetworkInstance ||
        !m_retainNetworkInstance || !m_collectNetworkInfos) {
        qCritical() << "RustFfi: failed to resolve one or more symbols";
        unload();
        return false;
    }

    qInfo() << "RustFfi: loaded successfully";
    return true;
}

bool RustFfi::isLoaded() const {
    return m_handle != nullptr;
}

void RustFfi::unload() {
    if (!m_handle) return;

#ifdef Q_OS_ANDROID
    dlclose(m_handle);
#else
    // QLibrary: 不需要手动 unload
#endif

    m_handle = nullptr;
    m_setTunFd = nullptr;
    m_getErrorMsg = nullptr;
    m_freeString = nullptr;
    m_parseConfig = nullptr;
    m_runNetworkInstance = nullptr;
    m_retainNetworkInstance = nullptr;
    m_collectNetworkInfos = nullptr;
}

int RustFfi::setTunFd(const QString& instanceName, int fd) {
    if (!m_setTunFd) return -1;
    QByteArray nameBytes = instanceName.toUtf8();
    return m_setTunFd(nameBytes.constData(), fd);
}

int RustFfi::parseConfig(const QString& config) {
    if (!m_parseConfig) return -1;
    QByteArray cfgBytes = config.toUtf8();
    return m_parseConfig(cfgBytes.constData());
}

int RustFfi::runNetworkInstance(const QString& config) {
    if (!m_runNetworkInstance) return -1;
    QByteArray cfgBytes = config.toUtf8();
    return m_runNetworkInstance(cfgBytes.constData());
}

int RustFfi::retainNetworkInstance(const char** names, size_t length) {
    if (!m_retainNetworkInstance) return -1;
    return m_retainNetworkInstance(names, length);
}

int RustFfi::collectNetworkInfos(KeyValuePair* infos, size_t maxLength) {
    if (!m_collectNetworkInfos) return -1;
    return m_collectNetworkInfos(infos, maxLength);
}

QString RustFfi::getLastError() {
    if (!m_getErrorMsg || !m_freeString) return QString();
    char* err = nullptr;
    m_getErrorMsg(&err);
    if (!err) return QString();
    QString result = QString::fromUtf8(err);
    m_freeString(err);
    return result;
}

void RustFfi::freeString(const char* s) {
    if (m_freeString) m_freeString(s);
}
