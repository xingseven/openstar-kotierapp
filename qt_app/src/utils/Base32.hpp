#ifndef BASE32_HPP
#define BASE32_HPP

#include <QString>
#include <QByteArray>

class Base32 {
public:
    // 编码：7 字节 netId + 8 字节 password → 25 字符联机码 (XXXX-XXXX-...)
    static QString encodeConnectionCode(const QByteArray& netId, const QByteArray& password);

    // 解码：联机码 → (netId, password)
    static bool decodeConnectionCode(const QString& code, QByteArray& outNetId, QByteArray& outPassword);

    // 生成房主凭证：返回 (netId, password)
    // netId: 7 字节, password: 8 字节
    static QPair<QByteArray, QByteArray> generateRoomCredentials();

private:
    // 自定义 Base32 字母表（不含 I/O，含 89，与 Qt 版兼容）
    static const char ALPHABET[35];
    static int charToValue(char c);

    // Base32 编码/解码核心
    static QString base32Encode(const QByteArray& data);
    static QByteArray base32Decode(const QString& encoded);
};

#endif // BASE32_HPP
