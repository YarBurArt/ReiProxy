package io.yarburart.reiproxy.core

import android.util.Log
import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline
import com.github.monkeywie.proxyee.server.HttpProxyServer
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig
import io.netty.buffer.ByteBufUtil
import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.LastHttpContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import io.yarburart.reiproxy.data.HistoryRepository

private fun Char.isPrintableChar(): Boolean = isPrintableExtended()

private fun extractCharset(headersString: String): Charset {
    val contentTypeLine = headersString.lineSequence().find {
        it.startsWith("Content-Type:", ignoreCase = true)
    } ?: return StandardCharsets.UTF_8
    val charsetParam = contentTypeLine.substringAfter(":").split(";")
        .map { it.trim() }
        .find { it.startsWith("charset=", ignoreCase = true) }
        ?: return StandardCharsets.UTF_8
    val charsetName = charsetParam.substringAfter("=", "UTF-8")
    return try { Charset.forName(charsetName) } catch (_: Exception) { StandardCharsets.UTF_8 }
}

data class ProxyConfig(
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val handleSsl: Boolean = true,
    val interceptEnabled: Boolean = false,
)

data class ProxyRequestRecord(
    val id: String,
    val method: String,
    val host: String,
    val url: String,
    val statusCode: Int,
    val requestHeaders: String,
    val requestBody: String,
    val responseHeaders: String,
    val responseBody: String,
    val mimeType: String,
    val length: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val rawRequest: String = "",
    val rawResponse: String = "",
)

object ProxyManager {

    private const val TAG = "ProxyManager"

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _requestHistory = MutableStateFlow<List<ProxyRequestRecord>>(emptyList())
    val requestHistory: StateFlow<List<ProxyRequestRecord>> = _requestHistory.asStateFlow()

    private val _activeConfig = MutableStateFlow(ProxyConfig())
    val activeConfig: StateFlow<ProxyConfig> = _activeConfig.asStateFlow()

    @Volatile
    private var historyRepository: HistoryRepository? = null
    private var activeProjectId: Long = 0

    fun setHistoryRepository(repo: HistoryRepository) { historyRepository = repo }
    fun setActiveProjectId(projectId: Long) { activeProjectId = projectId }

    @Volatile
    private var server: HttpProxyServer? = null
    private val isRunningFlag = AtomicBoolean(false)

    private val pendingRequests = ConcurrentHashMap<String, CapturedRequest>()
    private val responseMap = ConcurrentHashMap<String, CapturedResponse>()
    private val idCounter = java.util.concurrent.atomic.AtomicLong(0)

    private data class CapturedRequest(
        val id: String,
        var method: String,
        var host: String,
        var url: String,
        var headers: String,
        var body: StringBuilder,
        var rawRequest: StringBuilder,
        var charset: Charset = StandardCharsets.UTF_8,
    )

    private data class CapturedResponse(
        val requestId: String,
        var statusCode: Int,
        var headers: String,
        var body: StringBuilder,
        var rawResponse: StringBuilder,
        var charset: Charset = StandardCharsets.UTF_8,
    )

    fun updateConfig(config: ProxyConfig) { _activeConfig.value = config }

    suspend fun start(
        config: ProxyConfig = _activeConfig.value
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isRunningFlag.compareAndSet(false, true)) {
            return@withContext Result.failure(
                IllegalStateException("Proxy already running"))
        }
        _activeConfig.value = config
        server = HttpProxyServer()
        val serverConfig = HttpProxyServerConfig().apply { setHandleSsl(config.handleSsl) }
        try {
            server!!
                .serverConfig(serverConfig)
                .proxyInterceptInitializer(buildInterceptInitializer())
                .startAsync(config.port)
                .thenAccept {
                    _isRunning.value = true
                    Log.i(TAG, "Proxy started on ${config.host}:${config.port}")
                }
                .exceptionally { e ->
                    isRunningFlag.set(false)
                    _isRunning.value = false
                    Log.e(TAG, "Failed to start proxy", e)
                    null
                }
            Result.success(Unit)
        } catch (e: Exception) {
            isRunningFlag.set(false)
            _isRunning.value = false
            Log.e(TAG, "Failed to start proxy", e)
            Result.failure(e)
        }
    }

    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isRunningFlag.compareAndSet(true, false)) {
            return@withContext Result.failure(IllegalStateException("Proxy not running"))
        }
        return@withContext try {
            server?.close()
            server = null
            _isRunning.value = false
            pendingRequests.clear()
            Log.i(TAG, "Proxy stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping proxy", e)
            Result.failure(e)
        }
    }

    fun clearHistory() { _requestHistory.value = emptyList() }

    private fun nextId(): String = "req_${idCounter.incrementAndGet()}"

    private fun buildInterceptInitializer(): HttpProxyInterceptInitializer {
        return object : HttpProxyInterceptInitializer() {
            override fun init(pipeline: HttpProxyInterceptPipeline) {
                pipeline.addLast(object : HttpProxyIntercept() {
                    override fun beforeRequest(
                        clientChannel: Channel?,
                        httpRequest: HttpRequest?,
                        pipeline: HttpProxyInterceptPipeline?,
                    ) {
                        if (httpRequest != null) {
                            val id = nextId()
                            val headers = buildString {
                                httpRequest.headers().forEach { (k, v) -> append("$k: $v\r\n") }
                            }
                            val rawReq = StringBuilder().apply {
                                append("${httpRequest.method().name()} ${httpRequest.uri()} HTTP/1.1\r\n")
                                append(headers)
                                append("\r\n")
                            }
                            pendingRequests[id] = CapturedRequest(
                                id = id,
                                method = httpRequest.method().name(),
                                host = httpRequest.headers()[HttpHeaderNames.HOST] ?: "unknown",
                                url = httpRequest.uri(),
                                headers = headers.trimEnd(),
                                body = StringBuilder(),
                                rawRequest = rawReq,
                                charset = extractCharset(headers),
                            )
                            Log.d(TAG, "[$id] ${httpRequest.method().name()} ${httpRequest.uri()}")
                        }
                        super.beforeRequest(clientChannel, httpRequest, pipeline)
                    }

                    override fun beforeRequest(
                        clientChannel: Channel?,
                        httpContent: HttpContent?,
                        pipeline: HttpProxyInterceptPipeline?,
                    ) {
                        if (httpContent != null && pipeline?.httpRequest != null) {
                            val captured = findRequestForContent(pipeline.httpRequest)
                            if (captured != null) {
                                val content = try {
                                    val bytes = ByteBufUtil.getBytes(httpContent.content())
                                    decodeBytes(bytes, captured.charset)
                                } catch (_: Exception) { null }
                                if (content != null && content.any { it.isPrintableChar() }) {
                                    captured.body.append(content)
                                    captured.rawRequest.append(content)
                                }
                            }
                        }
                        super.beforeRequest(clientChannel, httpContent, pipeline)
                    }

                    override fun afterResponse(
                        clientChannel: Channel?,
                        proxyChannel: Channel?,
                        httpResponse: HttpResponse?,
                        pipeline: HttpProxyInterceptPipeline?,
                    ) {
                        if (httpResponse != null && pipeline?.httpRequest != null) {
                            val captured = findRequestForContent(pipeline.httpRequest)
                            if (captured != null) {
                                val headers = buildString {
                                    httpResponse.headers().forEach { (k, v) -> append("$k: $v\r\n") }
                                }
                                val rawResp = StringBuilder().apply {
                                    append("HTTP/1.1 ${httpResponse
                                        .status().code()} ${httpResponse
                                            .status().reasonPhrase()}\r\n")
                                    append(headers)
                                    append("\r\n")
                                }
                                responseMap[captured.id] = CapturedResponse(
                                    requestId = captured.id,
                                    statusCode = httpResponse.status().code(),
                                    headers = headers.trimEnd(),
                                    body = StringBuilder(),
                                    rawResponse = rawResp,
                                    charset = extractCharset(headers),
                                )
                            }
                        }
                        super.afterResponse(clientChannel, proxyChannel, httpResponse, pipeline)
                    }

                    override fun afterResponse(
                        clientChannel: Channel?,
                        proxyChannel: Channel?,
                        httpContent: HttpContent?,
                        pipeline: HttpProxyInterceptPipeline?,
                    ) {
                        if (httpContent != null && pipeline?.httpRequest != null) {
                            val capturedReq = findRequestForContent(pipeline.httpRequest)
                            if (capturedReq != null) {
                                val response = responseMap[capturedReq.id]
                                if (response != null) {
                                    val content = try {
                                        val bytes = ByteBufUtil.getBytes(httpContent.content())
                                        decodeBytes(bytes, response.charset)
                                    } catch (_: Exception) { null }
                                    if (content != null && content.any { it.isPrintableChar() }) {
                                        response.body.append(content)
                                        response.rawResponse.append(content)
                                    }
                                }
                                if (httpContent is LastHttpContent) {
                                    finalizeRecord(capturedReq, response)
                                }
                            }
                        }
                        super.afterResponse(clientChannel, proxyChannel, httpContent, pipeline)
                    }
                })
            }
        }
    }

    private fun findRequestForContent(httpRequest: HttpRequest): CapturedRequest? {
        val targetMethod = httpRequest.method().name()
        val targetUrl = httpRequest.uri()
        for ((_, req) in pendingRequests) {
            if (req.method == targetMethod && req.url == targetUrl) return req
        }
        for ((_, req) in pendingRequests) {
            if (req.method == targetMethod && targetUrl
                .startsWith(req.url.split("?")[0])) return req
        }
        return null
    }

    private fun finalizeRecord(captured: CapturedRequest, response: CapturedResponse?) {
        pendingRequests.remove(captured.id)

        val record = ProxyRequestRecord(
            id = captured.id,
            method = captured.method,
            host = captured.host,
            url = captured.url,
            statusCode = response?.statusCode ?: 0,
            requestHeaders = captured.headers,
            requestBody = captured.body.toString(),
            responseHeaders = response?.headers ?: "",
            responseBody = response?.body?.toString() ?: "",
            mimeType = response?.let { guessMimeTypeFromHeaders(it.headers) } ?: "unknown",
            length = response?.body?.length ?: 0,
            rawRequest = captured.rawRequest.toString(),
            rawResponse = response?.rawResponse?.toString() ?: "",
        )
        val current = _requestHistory.value.toMutableList()
        current.add(record)
        _requestHistory.value = current
        historyRepository?.let { repo ->
            try { runBlocking { repo.insertRequest(activeProjectId, record) } }
            catch (e: Exception) { Log.e(TAG, "Failed to save request to DB: ${e.message}") }
        }
        Log.d(TAG, "[${captured.id}] -> ${record.statusCode} (${record.length} bytes)")
        if (response != null) responseMap.remove(captured.id)
    }

    private fun guessMimeTypeFromHeaders(headers: String): String {
        val contentTypeLine = headers.lineSequence().find {
            it.startsWith("Content-Type:", ignoreCase = true) } ?: return "unknown"
        val contentType = contentTypeLine.substringAfter(":").trim()
        return when {
            contentType.contains("json", ignoreCase = true) -> "json"
            contentType.contains("html", ignoreCase = true) -> "html"
            contentType.contains("xml", ignoreCase = true) -> "xml"
            contentType.contains("text", ignoreCase = true) -> "text"
            contentType.contains("image", ignoreCase = true) -> "image"
            contentType.contains("javascript", ignoreCase = true) -> "js"
            contentType.contains("css", ignoreCase = true) -> "css"
            else -> "unknown"
        }
    }
}
