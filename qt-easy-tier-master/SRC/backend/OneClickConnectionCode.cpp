//
// Created by Copilot on 2026/5/13.
//

#include "backend/OneClickConnectionCode.h"

#include <QRegularExpression>

#include <random>

namespace {

static const QString PRIMARY_BASE32_CHARS = QStringLiteral("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567");
static const QString LEGACY_BASE32_CHARS = QStringLiteral("ABCDEFGHJKLMNPQRSTUVWXYZ23456789");

} // namespace

namespace EasyTierBackend {

QString base32Encode(const QByteArray &data, const QString &alphabet = PRIMARY_BASE32_CHARS)
{
    QString result;
    int buffer = 0;
    int bitsLeft = 0;

    for (int i = 0; i < static_cast<int>(data.size()); ++i) {
        buffer = (buffer << 8) | static_cast<unsigned char>(data[i]);
        bitsLeft += 8;

        while (bitsLeft >= 5) {
            result += alphabet[(buffer >> (bitsLeft - 5)) & 0x1F];
            bitsLeft -= 5;
        }
    }

    if (bitsLeft > 0) {
        result += alphabet[(buffer << (5 - bitsLeft)) & 0x1F];
    }

    return result;
}

QByteArray base32Decode(const QString &encoded, const QString &alphabet)
{
    QByteArray result;
    int buffer = 0;
    int bitsLeft = 0;

    for (int i = 0; i < static_cast<int>(encoded.length()); ++i) {
        const QChar ch = encoded[i];
        if (ch == QLatin1Char('=')) {
            continue;
        }

        const int val = static_cast<int>(alphabet.indexOf(ch.toUpper()));
        if (val == -1) {
            continue;
        }

        buffer = (buffer << 5) | val;
        bitsLeft += 5;

        if (bitsLeft >= 8) {
            result.append(static_cast<char>((buffer >> (bitsLeft - 8)) & 0xFF));
            bitsLeft -= 8;
        }
    }

    return result;
}

QPair<QString, QString> generateRoomCredentials()
{
    static const std::string charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789*&#$!";
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, static_cast<int>(charset.size() - 1));

    QByteArray networkIdRaw(7, 0);
    networkIdRaw[0] = 'Q';
    networkIdRaw[1] = 'E';
    for (int i = 2; i < 7; ++i) {
        networkIdRaw[i] = charset[dis(gen)];
    }

    QByteArray passwordRaw(8, 0);
    for (int i = 0; i < 8; ++i) {
        passwordRaw[i] = charset[dis(gen)];
    }

    const QString networkId = QStringLiteral("QtET-OneClick-") + QString::fromLatin1(networkIdRaw);
    const QString password = QString::fromLatin1(passwordRaw);

    return qMakePair(networkId, password);
}

QString encodeConnectionCode(const QString &networkId, const QString &password)
{
    QString networkPureId = networkId;
    networkPureId.remove(QStringLiteral("QtET-OneClick-"));

    const QByteArray networkIdData = networkPureId.toLatin1();
    const QByteArray passwordData = password.toLatin1();

    if (networkIdData.size() < 2 || networkIdData[0] != 'Q' || networkIdData[1] != 'E') {
        return {};
    }

    const QString encodedNetworkId = base32Encode(networkIdData);
    const QString encodedPassword = base32Encode(passwordData);

    if (encodedNetworkId.length() != 12 || encodedPassword.length() != 13) {
        return {};
    }

    const QString code = encodedNetworkId.left(4) + "-"
        + encodedNetworkId.mid(4, 4) + "-"
        + encodedNetworkId.mid(8, 4) + "-"
        + encodedPassword.left(5) + "-"
        + encodedPassword.mid(5, 5) + "-"
        + encodedPassword.mid(10, 3);

    return code.toUpper();
}

static QPair<QString, QString> decodeConnectionCodeWithAlphabet(const QString &cleanCode, const QString &alphabet)
{
    const QString encodedNetworkId = cleanCode.left(12);
    const QString encodedPassword = cleanCode.mid(12, 13);

    const QByteArray networkIdData = base32Decode(encodedNetworkId, alphabet);
    const QByteArray passwordData = base32Decode(encodedPassword, alphabet);

    if (networkIdData.size() != 7 || passwordData.size() != 8) {
        return qMakePair(QString(), QString());
    }

    if (networkIdData[0] != 'Q' || networkIdData[1] != 'E') {
        return qMakePair(QString(), QString());
    }

    const QString networkPureId = QString::fromLatin1(networkIdData);
    const QString password = QString::fromLatin1(passwordData);

    return qMakePair(QStringLiteral("QtET-OneClick-") + networkPureId, password);
}

QPair<QString, QString> decodeConnectionCode(const QString &code)
{
    QString cleanCode = code;
    static const QRegularExpression re("[^A-Za-z2-9]");
    cleanCode.remove(re);
    cleanCode = cleanCode.toUpper();

    if (cleanCode.length() != 25) {
        return qMakePair(QString(), QString());
    }

    const auto primaryDecoded = decodeConnectionCodeWithAlphabet(cleanCode, PRIMARY_BASE32_CHARS);
    if (!primaryDecoded.first.isEmpty() && !primaryDecoded.second.isEmpty()) {
        return primaryDecoded;
    }

    return decodeConnectionCodeWithAlphabet(cleanCode, LEGACY_BASE32_CHARS);
}

} // namespace EasyTierBackend