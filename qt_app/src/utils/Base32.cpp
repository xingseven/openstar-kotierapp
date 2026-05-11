#include "Base32.hpp"
#include <QRandomGenerator>
#include <QDebug>

// 自定义 Base32 字母表：不含 I/O，含 89（与 Qt 版兼容）
const char Base32::ALPHABET[35] = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

int Base32::charToValue(char c) {
    if (c >= 'A' && c <= 'Z') {
        // 排除 I (索引 8) 和 O (索引 14) 后的偏移映射
        int idx = c - 'A';
        if (idx >= 8) idx--; // 跳过 I
        if (idx >= 14) idx--; // 跳过 O
        return idx;
    }
    if (c >= 'a' && c <= 'z') {
        int idx = c - 'a';
        if (idx >= 8) idx--;
        if (idx >= 14) idx--;
        return idx;
    }
    if (c >= '0' && c <= '9') {
        // 数字对应索引 24-33，但不需要修正
        return 24 + (c - '0');
    }
    return -1;
}

QString Base32::base32Encode(const QByteArray& data) {
    if (data.isEmpty()) return {};
    QString result;
    int bits = 0;
    int bitCount = 0;

    for (unsigned char byte : data) {
        bits = (bits << 8) | byte;
        bitCount += 8;
        while (bitCount >= 5) {
            bitCount -= 5;
            int index = (bits >> bitCount) & 0x1F;
            result.append(ALPHABET[index]);
        }
    }

    if (bitCount > 0) {
        int index = (bits << (5 - bitCount)) & 0x1F;
        result.append(ALPHABET[index]);
    }

    return result;
}

QByteArray Base32::base32Decode(const QString& encoded) {
    QByteArray result;
    int bits = 0;
    int bitCount = 0;

    for (const QChar& c : encoded) {
        if (c == '-') continue; // 跳过连字符
        int val = charToValue(c.toLatin1());
        if (val < 0) continue;

        bits = (bits << 5) | val;
        bitCount += 5;

        if (bitCount >= 8) {
            bitCount -= 8;
            result.append(static_cast<char>((bits >> bitCount) & 0xFF));
        }
    }

    return result;
}

QString Base32::encodeConnectionCode(const QByteArray& netId, const QByteArray& password) {
    // 组合 netId (7 字节) + password (8 字节) = 15 字节
    QByteArray combined = netId + password;
    QString encoded = base32Encode(combined);

    // 格式化为 XXXX-XXXX-XXXX-XXXXX-XXXXX-XXX
    // 25 字符：5 组，前 4 组 4 字符，第 5 组 5 字符，第 6 组 3 字符 + 后缀
    // 实际：24 字符数据 + 1 校验 = 25 字符
    // 编码后应为 24 字符，但我们需要检查
    QString formatted;
    int pos = 0;
    int segments[] = {4, 4, 4, 5, 5, 3};
    for (int seg : segments) {
        if (!formatted.isEmpty()) formatted.append('-');
        formatted.append(encoded.mid(pos, seg));
        pos += seg;
    }

    return formatted;
}

bool Base32::decodeConnectionCode(const QString& code, QByteArray& outNetId, QByteArray& outPassword) {
    // 移除连字符
    QString clean;
    for (const QChar& c : code) {
        if (c != '-') clean.append(c.toUpper());
    }

    QByteArray decoded = base32Decode(clean);

    // 期望 15 字节：7 (netId) + 8 (password)
    if (decoded.size() < 15) {
        qWarning() << "Base32: decoded size too small:" << decoded.size();
        return false;
    }

    outNetId = decoded.left(7);
    outPassword = decoded.mid(7, 8);
    return true;
}

QPair<QByteArray, QByteArray> Base32::generateRoomCredentials() {
    QByteArray netId(7, 0);
    QByteArray password(8, 0);

    for (int i = 0; i < 7; ++i) {
        netId[i] = static_cast<char>(QRandomGenerator::global()->bounded(256));
    }
    for (int i = 0; i < 8; ++i) {
        password[i] = static_cast<char>(QRandomGenerator::global()->bounded(256));
    }

    return {netId, password};
}
