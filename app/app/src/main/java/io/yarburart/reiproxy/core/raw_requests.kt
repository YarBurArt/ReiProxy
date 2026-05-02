package io.yarburart.reiproxy.core

import android.util.Log
import io.yarburart.reiproxy.data.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.UUID

private const val TAG = "RawRequests"

data class RawRequestResult(
    val success: Boolean,
    val record: ProxyRequestRecord? = null,
    val error: String? = null,
)

suspend fun sendRawRequest(
    rawRequest: String,
    selectedRequest: ProxyRequestRecord? = null,
    historyRepository: HistoryRepository? = null,
    projectId: Long? = null,
): RawRequestResult = withContext(Dispatchers.IO) {
    try {
        val parsed = parseRawRequest(
            rawRequest, selectedRequest
        ) ?: return@withContext errorResult("Empty or invalid request")
        val connection = openConnection(parsed)
        val response = readResponse(connection, parsed.responseHeadersHint)
        connection.disconnect()

        val record = buildRecord(parsed, response)

        if (historyRepository != null && projectId != null) {
            try {
                historyRepository.insertRequest(projectId, record)
                Log.d(TAG, "[${record.id}] Saved to DB: ${parsed.method} ${parsed.urlPath} -> ${response.code}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save request to DB: ${e.message}")
            }
        }

        RawRequestResult(success = true, record = record)
    } catch (e: Exception) {
        Log.e(TAG, "Error sending raw request: ${e.message}", e)
        RawRequestResult(success = false, error = e.message ?: "Unknown error")
    }
}

private data class ParsedRequest(
    val method: String,
    val urlPath: String,
    val host: String,
    val headers: Map<String, String>,
    val body: String,
    val responseHeadersHint: String,
    val timestamp: Long,
)

private fun parseRawRequest(raw: String, fallback: ProxyRequestRecord?): ParsedRequest? {
    val lines = raw.lineSequence().toList()
    if (lines.isEmpty()) return null
    val parts = lines[0].split(" ")
    if (parts.size < 2) return null

    val headers = mutableMapOf<String, String>()
    var bodyStart = -1
    for (i in 1 until lines.size) {
        if (lines[i].isBlank()) { bodyStart = i + 1; break }
        val colonIdx = lines[i].indexOf(':')
        if (colonIdx > 0) headers[lines[i]
            .substring(0, colonIdx).trim()] = lines[i]
                .substring(colonIdx + 1).trim()
    }

    val host = headers["Host"] ?: fallback?.host ?: return null
    val scheme = if (host.startsWith("https://") || host.contains(":443"))
        "https" else "http"
    val fullPath = if (parts[1].startsWith("http"))
        parts[1] else "$scheme://$host${parts[1]}"

    val body = if (bodyStart > 0 && bodyStart < lines.size)
        lines.subList(bodyStart, lines.size).joinToString("\n")
    else fallback?.requestBody ?: ""

    return ParsedRequest(
        method = parts[0],
        urlPath = parts[1],
        host = host,
        headers = headers,
        body = body,
        responseHeadersHint = "",
        timestamp = System.currentTimeMillis(),
    )
}

private fun openConnection(parsed: ParsedRequest): HttpURLConnection {
    val url = URL(if (parsed.urlPath.startsWith("http")) parsed.urlPath else {
        val scheme = if (parsed.host.startsWith("https://")
            || parsed.host.contains(":443")) "https" else "http"
        "$scheme://${parsed.host}${parsed.urlPath}"
    })
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = parsed.method
    conn.doInput = true
    parsed.headers.forEach { (k, v) ->
        if (k.lowercase() != "host") conn.setRequestProperty(k, v)
    }
    if (parsed.body.isNotBlank()) {
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
            it.write(parsed.body)
            it.flush()
        }
    }
    return conn
}

private fun readResponse(connection: HttpURLConnection, headersHint: String): RawResponse {
    val code = try { connection.responseCode } catch (e: Exception) {
        connection.disconnect()
        return RawResponse(0, "", error = "Failed to get response: ${e.message}")
    }

    val respHeaders = buildString {
        connection.headerFields.forEach {
            (k, vs) -> if (k != null) vs.forEach { v -> append("$k: $v\r\n") } }
    }

    val body = try {
        val stream = if (code in 200..399) connection.inputStream else connection.errorStream
        stream?.use {
            val bytes = it.readBytes()
            if (bytes.isNotEmpty()) {
                val hint = headersHint.lineSequence()
                    .find { l -> l.startsWith("Content-Type:", true) }
                    ?.substringAfter("charset=")?.takeIf { l -> l.isNotBlank() }?.trim()
                    ?.let { n -> try {
                        Charset.forName(n)
                    } catch (_: Exception) { null } }
                val contentEncoding = connection.contentEncoding
                decodeBytes(bytes, hint, contentEncoding)
            } else ""
        } ?: ""
    } catch (e: Exception) {
        Log.w(TAG, "Failed to read response body: ${e.message}")
        ""
    }

    val raw = buildString {
        append("HTTP/1.1 $code\r\n")
        append(respHeaders)
        append("\r\n")
        if (body.isNotBlank()) append(body)
    }

    return RawResponse(code, raw, body = body, headers = respHeaders.trimEnd())
}

private data class RawResponse(
    val code: Int,
    val raw: String,
    val body: String = "",
    val headers: String = "",
    val error: String? = null,
)

private fun buildRecord(parsed: ParsedRequest, response: RawResponse): ProxyRequestRecord {
    val id = "raw_${UUID.randomUUID()}"
    return ProxyRequestRecord(
        id = id,
        method = parsed.method,
        host = parsed.host,
        url = parsed.urlPath,
        statusCode = response.code,
        requestHeaders = parsed.headers.map { "${it.key}: ${it.value}" }
            .joinToString("\r\n"),
        requestBody = parsed.body,
        responseHeaders = response.headers,
        responseBody = response.body,
        mimeType = guessMimeTypeFromHeaders(response.headers),
        length = response.body.length,
        timestamp = parsed.timestamp,
        rawRequest = "",
        rawResponse = response.raw,
    )
}

private fun errorResult(msg: String) = RawRequestResult(success = false, error = msg)

private fun guessMimeTypeFromHeaders(headers: String): String {
    val ct = headers.lineSequence().find {
        it.startsWith("Content-Type:", true)
    }?.substringAfter(":")?.trim() ?: return "unknown"
    return when {
        ct.contains("json", true) -> "json"
        ct.contains("html", true) -> "html"
        ct.contains("xml", true) -> "xml"
        ct.contains("text", true) -> "text"
        ct.contains("image", true) -> "image"
        ct.contains("javascript", true) -> "js"
        ct.contains("css", true) -> "css"
        else -> "unknown"
    }
}
