#ifndef RUSTFFI_HPP
#define RUSTFFI_HPP

#include <QString>
#include <QByteArray>
#include <cstddef>

// ── Rust C ABI 数据结构 ──

extern "C" {

struct KeyValuePair {
    const char* key;
    const char* value;
};

using SetTunFd = int (*)(const char* inst_name, int fd);
using GetErrorMsgFn = void (*)(char** out);
using FreeStringFn = void (*)(const char* s);
using ParseConfigFn = int (*)(const char* cfg_str);
using RunNetworkInstanceFn = int (*)(const char* cfg_str);
using RetainNetworkInstanceFn = int (*)(const char** inst_names, size_t length);
using CollectNetworkInfosFn = int (*)(KeyValuePair* infos, size_t max_length);

}

// ── C++ 封装 ──

class RustFfi {
public:
    RustFfi();
    ~RustFfi();

    bool load(const QString& libraryPath);
    bool isLoaded() const;
    void unload();

    // ── FFI 调用 ──
    int setTunFd(const QString& instanceName, int fd);
    int parseConfig(const QString& config);
    int runNetworkInstance(const QString& config);
    int retainNetworkInstance(const char** names, size_t length);
    int collectNetworkInfos(KeyValuePair* infos, size_t maxLength);
    QString getLastError();
    void freeString(const char* s);

private:
    void* m_handle = nullptr;

    // 函数指针
    SetTunFd m_setTunFd = nullptr;
    GetErrorMsgFn m_getErrorMsg = nullptr;
    FreeStringFn m_freeString = nullptr;
    ParseConfigFn m_parseConfig = nullptr;
    RunNetworkInstanceFn m_runNetworkInstance = nullptr;
    RetainNetworkInstanceFn m_retainNetworkInstance = nullptr;
    CollectNetworkInfosFn m_collectNetworkInfos = nullptr;
};

#endif // RUSTFFI_HPP
