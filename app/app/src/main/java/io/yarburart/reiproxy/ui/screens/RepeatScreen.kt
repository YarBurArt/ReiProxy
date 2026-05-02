package io.yarburart.reiproxy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.core.sendRawRequest
import io.yarburart.reiproxy.data.HistoryRepository
import io.yarburart.reiproxy.ui.components.ResizableSplitPane
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedEditor
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedText
import kotlinx.coroutines.launch

@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatScreen(
    modifier: Modifier = Modifier,
    selectedRequest: ProxyRequestRecord? = null,
    historyRepository: HistoryRepository? = null,
    activeProjectId: Long? = null,
    requests: List<ProxyRequestRecord> = emptyList(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentRecord by remember { mutableStateOf<ProxyRequestRecord?>(null) }
    var rawRequestText by remember { mutableStateOf(TextFieldValue("")) }
    var rawResponseText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val repeaterHistory = remember { mutableStateListOf<ProxyRequestRecord>() }
    var historyIndex by remember { mutableIntStateOf(-1) }

    fun loadRequest(req: ProxyRequestRecord) {
        currentRecord = req
        rawRequestText = TextFieldValue(req.buildRawRequestText())
        rawResponseText = req.buildRawResponseText()
        historyIndex = repeaterHistory.indexOfFirst { it.id == req.id }.coerceAtLeast(-1)
    }

    fun loadFromHistory(index: Int) {
        if (repeaterHistory.isEmpty()) return
        val idx = (index % repeaterHistory.size + repeaterHistory.size) % repeaterHistory.size
        historyIndex = idx
        loadRequest(repeaterHistory[idx])
    }

    LaunchedEffect(requests) {
        if (repeaterHistory.isEmpty()) {
            repeaterHistory.addAll(requests)
            selectedRequest?.let { loadRequest(it) }
        }
    }

    LaunchedEffect(selectedRequest) {
        selectedRequest?.let { loadRequest(it) }
    }

    fun handleSend() {
        if (isSending) return
        isSending = true
        scope.launch {
            val result = sendRawRequest(
                rawRequest = rawRequestText.text,
                selectedRequest = currentRecord,
                historyRepository = historyRepository,
                projectId = activeProjectId,
            )
            if (result.success && result.record != null) {
                rawResponseText = result.record.rawResponse
                currentRecord = result.record
                repeaterHistory.add(result.record)
                historyIndex = repeaterHistory.size - 1
            } else {
                Toast.makeText(context, "Send failed: ${result.error}", Toast.LENGTH_LONG).show()
            }
            isSending = false
        }
    }

    val hostHeader = rawRequestText.text.lineSequence().find {
        it.startsWith("Host:", true) }
    val hostFromHeader = hostHeader?.substringAfter(":")?.trim()
    val urlPath = rawRequestText.text.lineSequence().firstOrNull()?.split(
        " "
    )?.getOrNull(1) ?: ""
    val fullUrl = hostFromHeader?.let { "$it$urlPath" } ?: urlPath

    Column(modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { handleSend() }, enabled = !isSending) { Text("Send") }
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.padding(start = 4.dp).padding(
                    horizontal = 4.dp), strokeWidth = 2.dp)
            }
            TextButton(onClick = {}) { Text("Cancel") }
            TextButton(onClick = {
                loadFromHistory(historyIndex - 1) },
                enabled = repeaterHistory.isNotEmpty()) { Text("<") }
            TextButton(onClick = {
                loadFromHistory(historyIndex + 1) },
                enabled = repeaterHistory.isNotEmpty()) { Text(">") }
            if (repeaterHistory.isNotEmpty() && historyIndex >= 0) {
                Text("${historyIndex + 1}/${repeaterHistory.size}", fontSize = 12.sp)
            }
        }

        HorizontalDivider()
        Text("URL: $fullUrl",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp
        )
        HorizontalDivider()

        ResizableSplitPane(
            firstPane = {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    SyntaxHighlightedEditor(
                        value = rawRequestText,
                        onValueChange = { rawRequestText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            },
            secondPane = {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(
                    rememberScrollState()).padding(8.dp)) {
                    if (rawResponseText.isNotBlank()) {
                        SyntaxHighlightedText(content = rawResponseText)
                    } else {
                        Text("No response captured", modifier = Modifier.padding(8.dp))
                    }
                }
            },
        )
    }
}
