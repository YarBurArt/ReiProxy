package io.yarburart.reiproxy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import io.yarburart.reiproxy.PayloadRecord
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.core.sendRawRequest
import io.yarburart.reiproxy.data.HistoryRepository
import io.yarburart.reiproxy.ui.components.ResizableSplitPane
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedEditor
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedText
import kotlinx.coroutines.launch

@PreviewScreenSizes
@Composable
fun AutomateScreen(
    modifier: Modifier = Modifier,
    selectedRequest: ProxyRequestRecord? = null,
    historyRepository: HistoryRepository? = null,
    activeProjectId: Long? = null,
    requests: List<ProxyRequestRecord> = emptyList(),
    payloads: List<PayloadRecord> = emptyList(),
) {
    // TODO: Redesign UI for better fuzzing workflow
    // TODO: Implement start test logic (iterate through payloads and send requests)
    // TODO: Add a list/table to show requests sent during the fuzzing process
    // TODO: Separate automate history from general history to keep fuzzing results organized

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentRecord by remember { mutableStateOf<ProxyRequestRecord?>(null) }
    var rawRequestText by remember { mutableStateOf(TextFieldValue("")) }
    var rawResponseText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showPayloads by remember { mutableStateOf(false) }
    
    var selectedPayloadType by remember { mutableStateOf<String?>(null) }
    var selectedIntruderPath by remember { mutableStateOf<String?>(null) }
    val selectedFuzzIndexes = remember { mutableStateListOf<Int>() }
    val assignedPayloads = remember { mutableStateMapOf<Int, String>() }

    val repeaterHistory = remember { mutableStateListOf<ProxyRequestRecord>() }
    var historyIndex by remember { mutableIntStateOf(-1) }

    fun loadRequest(req: ProxyRequestRecord) {
        currentRecord = req
        rawRequestText = TextFieldValue(req.rawRequest.takeIf { it.isNotBlank() } ?: buildRawRequest(req))
        rawResponseText = req.rawResponse.takeIf { it.isNotBlank() } ?: buildRawResponse(req)
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

    fun handleAddFuzzme() {
        val text = rawRequestText.text
        val selection = rawRequestText.selection.start.coerceAtLeast(0)
        val updated = text.substring(0, selection) + "FUZZME" + text.substring(selection)
        rawRequestText = TextFieldValue(
            text = updated,
            selection = androidx.compose.ui.text.TextRange(selection + 6),
        )
    }

    fun handleClearFuzzme() {
        rawRequestText = TextFieldValue(rawRequestText.text.replace("FUZZME", ""))
        selectedFuzzIndexes.clear()
        assignedPayloads.clear()
        selectedIntruderPath = null
    }

    fun toggleFuzzIndex(index: Int) {
        if (selectedFuzzIndexes.contains(index)) {
            selectedFuzzIndexes.remove(index)
        } else {
            selectedFuzzIndexes.add(index)
        }
    }

    val fuzzmeCount = remember(rawRequestText.text) {
        "FUZZME".toRegex().findAll(rawRequestText.text).count()
    }

    fun applyIntruderPayload(payload: PayloadRecord, indexes: Set<Int>, intruderName: String) {
        if (indexes.isEmpty()) {
            Toast.makeText(context, "Select at least one FUZZME slot", Toast.LENGTH_SHORT).show()
            return
        }
        indexes.forEach { assignedPayloads[it] = "${payload.vulnType}: $intruderName" }
        Toast.makeText(context, "Assigned payload to ${indexes.size} slots", Toast.LENGTH_SHORT).show()
        showPayloads = false
    }

    val hostHeader = rawRequestText.text.lineSequence().find { it.startsWith("Host:", true) }
    val hostFromHeader = hostHeader?.substringAfter(":")?.trim()
    val urlPath = rawRequestText.text.lineSequence().firstOrNull()?.split(" ")?.getOrNull(1) ?: ""
    val fullUrl = hostFromHeader?.let { "$it$urlPath" } ?: urlPath

    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextButton(onClick = { handleSend() }, enabled = !isSending) { Text("Send") }
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            TextButton(onClick = {}) { Text("Cancel") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = repeaterHistory.isNotEmpty()) { loadFromHistory(historyIndex - 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.ArrowBackIos, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = repeaterHistory.isNotEmpty()) { loadFromHistory(historyIndex + 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            }

            if (repeaterHistory.isNotEmpty() && historyIndex >= 0) {
                Text("${historyIndex + 1}/${repeaterHistory.size}", fontSize = 12.sp)
            }
            TextButton(onClick = { handleAddFuzzme() }) { Text("Add FUZZME") }
            TextButton(onClick = { handleClearFuzzme() }) { Text("Clear FUZZME") }
            TextButton(onClick = { showPayloads = true }) { Text("Payloads lib") }
        }

        HorizontalDivider()
        Text(
            text = "URL: $fullUrl",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
        )
        Text(
            text = "FUZZME slots found: $fuzzmeCount",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        if (assignedPayloads.isNotEmpty()) {
            Text(
                text = "Assignments: " + assignedPayloads.entries.sortedBy { it.key }
                    .joinToString(" | ") { "#${it.key + 1}: ${it.value}" },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                    if (rawResponseText.isNotBlank()) {
                        SyntaxHighlightedText(content = rawResponseText)
                    } else {
                        Text("No response captured", modifier = Modifier.padding(8.dp))
                    }
                }
            },
        )
    }

    if (showPayloads) {
        AlertDialog(
            onDismissRequest = { showPayloads = false },
            title = { Text("Payload Library") },
            text = {
                PayloadScreen(
                    payloads = payloads,
                    fuzzmeCount = fuzzmeCount,
                    selectedPayloadType = selectedPayloadType,
                    selectedIntruderPath = selectedIntruderPath,
                    selectedFuzzIndexes = selectedFuzzIndexes.toSet(),
                    onSelectPayload = { selectedPayloadType = it },
                    onToggleFuzzIndex = { toggleFuzzIndex(it) },
                    onUseIntruder = { payload, intruder, indexes ->
                        selectedIntruderPath = intruder.path
                        applyIntruderPayload(payload, indexes, intruder.name)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { showPayloads = false }) { Text("Close") }
            },
        )
    }
}

// Helper functions for raw request/response building (placeholders)
private fun buildRawRequest(req: ProxyRequestRecord): String = ""
private fun buildRawResponse(req: ProxyRequestRecord): String = ""
