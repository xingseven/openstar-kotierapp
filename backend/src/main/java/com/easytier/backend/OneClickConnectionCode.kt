package com.easytier.backend

import kotlin.random.Random

private const val PRIMARY_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
private const val LEGACY_BASE32 = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

fun base32Encode(data: ByteArray, alphabet: String = PRIMARY_BASE32): String {
    val result = StringBuilder()
    var buffer = 0
    var bitsLeft = 0

    for (byte in data) {
        buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
        bitsLeft += 8
        while (bitsLeft >= 5) {
            result.append(alphabet[(buffer shr (bitsLeft - 5)) and 0x1F])
            bitsLeft -= 5
        }
    }

    if (bitsLeft > 0) {
        result.append(alphabet[(buffer shl (5 - bitsLeft)) and 0x1F])
    }

    return result.toString()
}

private fun base32Decode(encoded: String, alphabet: String): ByteArray {
    val bytes = ArrayList<Byte>()
    var buffer = 0
    var bitsLeft = 0

    for (ch in encoded.uppercase()) {
        if (ch == '=') continue
        val index = alphabet.indexOf(ch)
        if (index < 0) continue

        buffer = (buffer shl 5) or index
        bitsLeft += 5

        if (bitsLeft >= 8) {
            bytes.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
            bitsLeft -= 8
        }
    }

    return bytes.toByteArray()
}

private fun decodeConnectionCodeWithAlphabet(code: String, alphabet: String): Pair<String, String> {
    val encodedNetworkId = code.substring(0, 12)
    val encodedPassword = code.substring(12, 25)
    val networkIdData = base32Decode(encodedNetworkId, alphabet)
    val passwordData = base32Decode(encodedPassword, alphabet)

    if (networkIdData.size != 7 || passwordData.size != 8) return "" to ""
    if (networkIdData[0] != 'Q'.code.toByte() || networkIdData[1] != 'E'.code.toByte()) return "" to ""

    return "QtET-OneClick-" + networkIdData.decodeToString() to passwordData.decodeToString()
}

fun generateRoomCredentials(): Pair<String, String> {
    val charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789*&#$!"
    val networkIdBytes = ByteArray(7).apply {
        this[0] = 'Q'.code.toByte()
        this[1] = 'E'.code.toByte()
        for (index in 2 until 7) {
            this[index] = charset[Random.nextInt(charset.length)].code.toByte()
        }
    }
    val passwordBytes = ByteArray(8) { charset[Random.nextInt(charset.length)].code.toByte() }
    val networkId = "QtET-OneClick-" + networkIdBytes.decodeToString()
    val password = passwordBytes.decodeToString()
    return networkId to password
}

fun encodeConnectionCode(networkId: String, password: String): String {
    val pureId = networkId.removePrefix("QtET-OneClick-")
    val networkIdData = pureId.toByteArray()
    val passwordData = password.toByteArray()

    if (networkIdData.size < 2 || networkIdData[0] != 'Q'.code.toByte() || networkIdData[1] != 'E'.code.toByte()) {
        return ""
    }

    // Qt 端当前仍使用旧字母表编码联机码，Android 房主也按同一套生成，避免 PC 房客侧判“联机码错误”。
    val encodedNetworkId = base32Encode(networkIdData, LEGACY_BASE32)
    val encodedPassword = base32Encode(passwordData, LEGACY_BASE32)
    if (encodedNetworkId.length != 12 || encodedPassword.length != 13) {
        return ""
    }

    return listOf(
        encodedNetworkId.substring(0, 4),
        encodedNetworkId.substring(4, 8),
        encodedNetworkId.substring(8, 12),
        encodedPassword.substring(0, 5),
        encodedPassword.substring(5, 10),
        encodedPassword.substring(10, 13)
    ).joinToString("-").uppercase()
}

fun decodeConnectionCode(code: String): Pair<String, String> {
    val cleanCode = code.uppercase().replace(Regex("[^A-Z2-9]"), "")
    if (cleanCode.length != 25) return "" to ""

    val primaryResult = decodeConnectionCodeWithAlphabet(cleanCode, PRIMARY_BASE32)
    if (primaryResult.first.isNotEmpty() && primaryResult.second.isNotEmpty()) {
        return primaryResult
    }

    return decodeConnectionCodeWithAlphabet(cleanCode, LEGACY_BASE32)
}
