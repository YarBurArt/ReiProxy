package io.yarburart.reiproxy.ui.screens.automate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.ui.components.EmptyState
import io.yarburart.reiproxy.ui.components.HistorySortOption
import io.yarburart.reiproxy.ui.components.HistoryTableHeader
import io.yarburart.reiproxy.ui.components.HistoryTableRow
import io.yarburart.reiproxy.ui.components.ResizableSplitPane
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedText
import io.yarburart.reiproxy.ui.state.AutomateState

@Composable
fun ResultsTab(state: AutomateState) {
    val sortedHistory = remember(state.repeaterHistory, state.sortOption) {
        when (state.sortOption) {
            HistorySortOption.TIME -> state.repeaterHistory
            HistorySortOption.STATUS_CODE -> state.repeaterHistory.sortedBy { it.statusCode }
            HistorySortOption.LENGTH -> state.repeaterHistory.sortedBy { it.length }
        }
    }

    if (state.repeaterHistory.isEmpty()) {
        EmptyState("No results yet. Start an attack or send a request.")
    } else {
        ResizableSplitPane(
            firstPane = {
                Column(modifier = Modifier.fillMaxSize()) {
                    HistoryTableHeader(onSort = { state.sortOption = it })
                    HorizontalDivider()
                    LazyColumn {
                        itemsIndexed(sortedHistory) { index, record ->
                            HistoryTableRow(
                                request = record,
                                index = index,
                                isSelected = state.selectedResultIndex == index,
                                onClick = { state.selectedResultIndex = index }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            secondPane = {
                val currentIdx = state.selectedResultIndex
                if (currentIdx != null && currentIdx < sortedHistory.size) {
                    val record = sortedHistory[currentIdx]
                    ResizableSplitPane(
                        firstPane = {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                                Text("Request", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                SyntaxHighlightedText(content = record.buildRawRequestText())
                            }
                        },
                        secondPane = {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                                Text("Response", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                SyntaxHighlightedText(content = record.buildRawResponseText())
                            }
                        }
                    )
                } else {
                    EmptyState("Select a result to view details")
                }
            }
        )
    }
}
