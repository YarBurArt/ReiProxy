package io.yarburart.reiproxy.ui.state

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import io.yarburart.reiproxy.IntruderPayload
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.core.sendRawRequest
import io.yarburart.reiproxy.data.HistoryRepository
import io.yarburart.reiproxy.getFileContent
import io.yarburart.reiproxy.ui.components.HistorySortOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
class AutomateState(
    private val scope: CoroutineScope,
    private val context: Context,
    private val historyRepository: HistoryRepository?,
    private val activeProjectId: Long?,
) {
    var selectedTabIndex by mutableIntStateOf(0)

    var currentRecord by mutableStateOf<ProxyRequestRecord?>(null)
    var rawRequestText by mutableStateOf(TextFieldValue(""))
    var rawResponseText by mutableStateOf("")
    var isSending by mutableStateOf(false)
    var showPayloads by mutableStateOf(false)

    var selectedPayloadType by mutableStateOf<String?>(null)
    var selectedIntruderPath by mutableStateOf<String?>(null)
    val selectedFuzzIndexes = mutableStateListOf<Int>()
    val assignedPayloads = mutableStateMapOf<Int, IntruderPayload>()

    val repeaterHistory = mutableStateListOf<ProxyRequestRecord>()
    var historyIndex by mutableIntStateOf(-1)
    var selectedResultIndex by mutableStateOf<Int?>(null)

    private var attackJob: Job? = null
    var isAttacking by mutableStateOf(false)

    var sortOption by mutableStateOf(HistorySortOption.TIME)

    fun loadRequest(req: ProxyRequestRecord) {
        currentRecord = req
        rawRequestText = TextFieldValue(req.buildRawRequestText())
        rawResponseText = req.buildRawResponseText()
        if (repeaterHistory.isNotEmpty()) {
            historyIndex = repeaterHistory.indexOfFirst { it.id == req.id }.coerceAtLeast(-1)
        } else {
            historyIndex = -1
        }
    }

    fun loadFromHistory(index: Int) {
        if (repeaterHistory.isEmpty()) return
        val idx = (index % repeaterHistory.size + repeaterHistory.size) % repeaterHistory.size
        historyIndex = idx
        loadRequest(repeaterHistory[idx])
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

    fun handleAttack() {
        if (isAttacking) {
            attackJob?.cancel()
            isAttacking = false
            return
        }

        if (assignedPayloads.isEmpty()) {
            Toast.makeText(context, "No payloads assigned", Toast.LENGTH_SHORT).show()
            return
        }

        isAttacking = true
        selectedTabIndex = 1 // Switch to Results tab
        attackJob = scope.launch {
            try {
                val firstEntry = assignedPayloads.entries.firstOrNull() ?: return@launch
                val payloadLines = getFileContent(firstEntry.value.path).lineSequence()
                    .filter { it.isNotBlank() }.toList()

                for (line in payloadLines) {
                    val currentRequest = rawRequestText.text
                    val regex = "FUZZME".toRegex()
                    val matches = regex.findAll(currentRequest).toList()
                    
                    val sb = StringBuilder(currentRequest)
                    var offset = 0
                    matches.forEachIndexed { index, matchResult ->
                        if (assignedPayloads.containsKey(index)) {
                            val range = matchResult.range
                            sb.replace(range.first + offset, range.last + 1 + offset, line)
                            offset += line.length - (range.last - range.first + 1)
                        }
                    }

                    val result = sendRawRequest(
                        rawRequest = sb.toString(),
                        selectedRequest = currentRecord,
                        historyRepository = null, // Do NOT save fuzzing results to DB
                        projectId = null,
                    )
                    if (result.success && result.record != null) {
                        repeaterHistory.add(result.record)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Attack error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isAttacking = false
            }
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

    fun applyIntruderPayload(intruder: IntruderPayload, indexes: Set<Int>) {
        if (indexes.isEmpty()) {
            Toast.makeText(context, "Select at least one FUZZME slot", Toast.LENGTH_SHORT).show()
            return
        }
        indexes.forEach { assignedPayloads[it] = intruder }
        Toast.makeText(context, "Assigned payload to ${indexes.size} slots", Toast.LENGTH_SHORT).show()
        showPayloads = false
    }

    val fuzzmeCount: Int
        get() = "FUZZME".toRegex().findAll(rawRequestText.text).count()

    val fullUrl: String
        get() {
            val hostHeader = rawRequestText.text.lineSequence().find { it.startsWith("Host:", true) }
            val hostFromHeader = hostHeader?.substringAfter(":")?.trim()
            val urlPath = rawRequestText.text.lineSequence().firstOrNull()?.split(" ")?.getOrNull(1) ?: ""
            return hostFromHeader?.let { "$it$urlPath" } ?: urlPath
        }

    fun updateRepeaterHistory(requests: List<ProxyRequestRecord>) {
        if (repeaterHistory.isEmpty()) {
            repeaterHistory.addAll(requests)
        }
    }
}

@Composable
fun rememberAutomateState(
    historyRepository: HistoryRepository?,
    activeProjectId: Long?,
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
): AutomateState = remember(historyRepository, activeProjectId, scope, context) {
    AutomateState(scope, context, historyRepository, activeProjectId)
}
