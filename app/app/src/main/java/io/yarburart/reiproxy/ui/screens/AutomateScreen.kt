package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import io.yarburart.reiproxy.PayloadRecord
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.data.HistoryRepository
import io.yarburart.reiproxy.ui.screens.automate.EditorTab
import io.yarburart.reiproxy.ui.screens.automate.ResultsTab
import io.yarburart.reiproxy.ui.state.AutomateState
import io.yarburart.reiproxy.ui.state.rememberAutomateState

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
    val state = rememberAutomateState(historyRepository, activeProjectId)

    LaunchedEffect(requests) {
        state.updateRepeaterHistory(requests)
        if (state.repeaterHistory.isNotEmpty() && (selectedRequest != null) && (state.currentRecord == null)) {
            state.loadRequest(selectedRequest)
        }
    }

    LaunchedEffect(selectedRequest) {
        selectedRequest?.let { state.loadRequest(it) }
    }

    Column(modifier = modifier.statusBarsPadding()) {
        val tabs = listOf("Editor", "Results")
        PrimaryTabRow(selectedTabIndex = state.selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedTabIndex == index,
                    onClick = { state.selectedTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        when (state.selectedTabIndex) {
            0 -> EditorTab(state)
            1 -> ResultsTab(state)
        }
    }

    if (state.showPayloads) {
        PayloadLibraryDialog(state, payloads)
    }
}

@Composable
private fun PayloadLibraryDialog(
    state: AutomateState,
    payloads: List<PayloadRecord>
) {
    AlertDialog(
        onDismissRequest = { state.showPayloads = false },
        title = { Text("Payload Library") },
        text = {
            PayloadScreen(
                payloads = payloads,
                fuzzmeCount = state.fuzzmeCount,
                selectedPayloadType = state.selectedPayloadType,
                selectedIntruderPath = state.selectedIntruderPath,
                selectedFuzzIndexes = state.selectedFuzzIndexes.toSet(),
                onSelectPayload = { state.selectedPayloadType = it },
                onToggleFuzzIndex = { state.toggleFuzzIndex(it) },
            ) { _, intruder, indexes ->
                state.selectedIntruderPath = intruder.path
                state.applyIntruderPayload(intruder, indexes)
            }
        },
        confirmButton = {
            TextButton(onClick = { state.showPayloads = false }) { Text("Close") }
        },
    )
}
