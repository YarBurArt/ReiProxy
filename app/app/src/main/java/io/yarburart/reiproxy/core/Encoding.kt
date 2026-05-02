package io.yarburart.reiproxy.core

import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

fun decodeBytes(bytes: ByteArray, hintCharset: Charset? = null, contentEncoding: String? = null): String {
    if (bytes.isEmpty()) return ""

    var data = bytes
    try {
        if (contentEncoding?.contains("gzip", true) == true) {
            data = GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()
        } else if (contentEncoding?.contains("deflate", true) == true) {
            data = InflaterInputStream(ByteArrayInputStream(bytes)).readBytes()
        }
    } catch (e: Exception) {
    }

    if (data.isEmpty()) return ""

    val bomCharset = detectBOM(data)
    if (bomCharset != null) {
        val bomSize = when(bomCharset) {
            StandardCharsets.UTF_8 -> 3
            StandardCharsets.UTF_16BE, StandardCharsets.UTF_16LE -> 2
            else -> 0
        }
        return try {
            String(data, bomSize, data.size - bomSize, bomCharset)
        } catch (_: Exception) {
            tryDecoding(data, hintCharset)
        }
    }

    if (isUTF8(data)) {
        return String(data, StandardCharsets.UTF_8)
    }

    if (hintCharset != null && !isWeakCharset(hintCharset)) {
        try {
            val result = String(data, hintCharset)
            if (!result.contains('\uFFFD') && result.any { it.isPrintableExtended() }) {
                return result
            }
        } catch (_: Exception) {}
    }

    return tryDecoding(data, hintCharset)
}

private fun detectBOM(bytes: ByteArray): Charset? {
    if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) return StandardCharsets.UTF_8
    if (bytes.size >= 2) {
        if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) return StandardCharsets.UTF_16BE
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) return StandardCharsets.UTF_16LE
    }
    return null
}

private fun isWeakCharset(charset: Charset): Boolean {
    val name = charset.name().uppercase()
    return name == "ISO-8859-1" || name == "US-ASCII" || name == "WINDOWS-1252"
}

private fun isUTF8(bytes: ByteArray): Boolean {
    var i = 0
    while (i < bytes.size) {
        val b = bytes[i].toInt() and 0xFF
        
        val len = when {
            b and 0x80 == 0 -> 1
            b and 0xE0 == 0xC0 -> 2
            b and 0xF0 == 0xE0 -> 3
            b and 0xF8 == 0xF0 -> 4
            else -> return false
        }
        
        if (i + len > bytes.size) return false
        for (j in 1 until len) {
            if (bytes[i + j].toInt() and 0xC0 != 0x80) return false
        }
        i += len
    }
    return true
}

private fun tryDecoding(bytes: ByteArray, hint: Charset?): String {
    if (isLikelyWindows1251(bytes)) {
        return String(bytes, Charset.forName("windows-1251"))
    }
    
    if (isLikelyKOI8R(bytes)) {
        return String(bytes, Charset.forName("KOI8-R"))
    }

    if (hint != null) {
        try {
            val str = String(bytes, hint)
            if (!str.contains('\uFFFD')) return str
        } catch (_: Exception) {}
    }

    return String(bytes, StandardCharsets.UTF_8)
}

private fun isLikelyWindows1251(bytes: ByteArray): Boolean {
    var russianChars = 0
    for (b in bytes) {
        val u = b.toInt() and 0xFF
        if (u in 0xC0..0xFF || u == 0xA8 || u == 0xB8) {
            russianChars++
        }
    }
    return russianChars.toFloat() / bytes.size > 0.2f
}

private fun isLikelyKOI8R(bytes: ByteArray): Boolean {
    var russianChars = 0
    for (b in bytes) {
        val u = b.toInt() and 0xFF
        if (u in 0xC0..0xFF || u == 0xA3 || u == 0xB3) {
            russianChars++
        }
    }
    return false
}

fun Char.isPrintableExtended(): Boolean {
    val code = this.code
    if (code in 0x20..0x7E) return true
    if (this == '\t' || this == '\n' || this == '\r') return true
    if (code in 0x0400..0x04FF) return true
    if (code in 0x0500..0x052F) return true
    if (code in 0x2000..0x206F) return true
    if (code in 0x2100..0x214F) return true
    if (code in 0x00A0..0x00FF) return true
    return false
}
