package io.yarburart.reiproxy.core

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun decodeBytes(bytes: ByteArray, hintCharset: Charset? = null): String {
    if (bytes.isEmpty()) return ""

    hintCharset?.let { charset ->
        return try {
            val result = String(bytes, charset)
            if (!result.contains('\uFFFD') && result.any { it.isPrintableExtended() })
                result else null
        } catch (_: Exception) {
            null
        } ?: tryDecoding(bytes)
    }

    return tryDecoding(bytes)
}

private fun tryDecoding(bytes: ByteArray): String {
    try {
        val str = String(bytes, StandardCharsets.UTF_8)
        val replacements = str.count { it == '\uFFFD' }
        val printable = str.count { it.isPrintableExtended() }
        if (replacements < 3 && (str.isEmpty() || printable.toFloat() / str.length > 0.7f)) {
            return str
        }
    } catch (_: Exception) { }

    try {
        val str = String(bytes, Charset.forName("windows-1251"))
        val printable = str.count { it.isPrintableExtended() }
        if (str.isNotEmpty() && printable.toFloat() / str.length > 0.7f) {
            return str
        }
    } catch (_: Exception) { }

    return String(bytes, StandardCharsets.ISO_8859_1)
}

fun Char.isPrintableExtended(): Boolean {
    if (this in ' '..'~') return true
    if (this in "\t\n\r") return true
    if (this in '\u0400'..'\u04FF') return true
    if (this in '\u0500'..'\u052F') return true
    if (this in '\uA640'..'\uA69F') return true
    if (this.code in 0x00A0..0x00FF) return true
    return false
}
