package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.ui.components.EmptyState
import io.yarburart.reiproxy.ui.components.HistoryTableHeader
import io.yarburart.reiproxy.ui.components.HistoryTableRow
import io.yarburart.reiproxy.ui.components.ResizableSplitPane
import io.yarburart.reiproxy.ui.components.ScreenTitle
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedText

@PreviewScreenSizes
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    requests: List<ProxyRequestRecord> = emptyList(),
    onClear: () -> Unit = {},
    onDeleteRequest: (ProxyRequestRecord) -> Unit = {},
    onRequestSelected: (ProxyRequestRecord?) -> Unit = {},
    onNavigateToRepeat: () -> Unit = {},
    onNavigateToAutomate: () -> Unit = {},
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var viewTab by remember { mutableIntStateOf(0) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    val currentSelectedIndex = selectedIndex
    if (currentSelectedIndex != null && currentSelectedIndex >= requests.size) {
        selectedIndex = null
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle("History")
            if (requests.isNotEmpty()) {
                IconButton(onClick = { showClearConfirmation = true }) {
                    Text("🗑", fontSize = 20.sp)
                }
            }
        }

        if (requests.isEmpty()) {
            EmptyState("No requests recorded")
        } else {
            ResizableSplitPane(
                firstPane = {
                    Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                        HistoryTableHeader()
                        HorizontalDivider()
                        LazyColumn {
                            itemsIndexed(requests) { index, request ->
                                HistoryTableRow(
                                    request = request,
                                    index = index,
                                    isSelected = selectedIndex == index,
                                    onClick = {
                                        selectedIndex = index
                                        onRequestSelected(request)
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                },
                secondPane = {
                    Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                        val currentSelectedIdx = selectedIndex
                        if (currentSelectedIdx != null && requests.isNotEmpty()) {
                            val selected = requests[currentSelectedIdx]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val options = listOf("Request", "Response")
                                SingleChoiceSegmentedButtonRow {
                                    options.forEachIndexed { i, label ->
                                        SegmentedButton(
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = i,
                                                count = options.size
                                            ),
                                            onClick = { viewTab = i },
                                            selected = viewTab == i,
                                        ) {
                                            Text(label)
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        onRequestSelected(selected)
                                        onNavigateToRepeat()
                                    },
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Text("To Repeater", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        onRequestSelected(selected)
                                        onNavigateToAutomate()
                                    },
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Text("To Automate", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        onDeleteRequest(selected)
                                        selectedIndex = null
                                    },
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            val content = if (viewTab == 0) {
                                selected.rawRequest.ifBlank { buildRequestText(selected) }
                            } else {
                                selected.rawResponse.ifBlank { buildResponseText(selected) }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                SyntaxHighlightedText(content = content)
                            }
                        } else {
                            EmptyState("Select a request to view details")
                        }
                    }
                }
            )
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to delete all recorded requests?") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showClearConfirmation = false
                    selectedIndex = null
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun buildRequestText(record: ProxyRequestRecord): String {
    return buildString {
        append("${record.method} ${record.url} HTTP/1.1\n")
        if (record.requestHeaders.isNotBlank()) {
            append(record.requestHeaders)
            append("\n")
        }
        if (record.requestBody.isNotBlank()) {
            append("\n")
            append(record.requestBody)
        }
    }
}

private fun buildResponseText(record: ProxyRequestRecord): String {
    return buildString {
        append("HTTP/1.1 ${record.statusCode}\n")
        if (record.responseHeaders.isNotBlank()) {
            append(record.responseHeaders)
            append("\n")
        }
        if (record.responseBody.isNotBlank()) {
            append("\n")
            append(record.responseBody)
        }
    }
}
