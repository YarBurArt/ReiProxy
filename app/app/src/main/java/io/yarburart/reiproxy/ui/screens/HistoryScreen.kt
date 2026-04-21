package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.ui.components.ResizableSplitPane
import io.yarburart.reiproxy.ui.components.EmptyState
import io.yarburart.reiproxy.ui.components.ScreenTitle
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedText

@PreviewScreenSizes
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    requests: List<ProxyRequestRecord> = emptyList(),
    onClear: () -> Unit = {},
    onRequestSelected: (ProxyRequestRecord?) -> Unit = {},
    onNavigateToRepeat: () -> Unit = {},
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var viewTab by remember { mutableIntStateOf(0) }

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
                IconButton(onClick = onClear) {
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
                        HistoryHeader()
                        HorizontalDivider()
                        LazyColumn {
                            itemsIndexed(requests) { index, request ->
                                HistoryRow(
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

@Composable
private fun HistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text("#", modifier = Modifier.width(32.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Method", modifier = Modifier.width(72.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Host", modifier = Modifier.weight(1f), fontSize = 12.sp,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("URL", modifier = Modifier.weight(2f), fontSize = 12.sp,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Status", modifier = Modifier.width(56.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Len", modifier = Modifier.width(48.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Type", modifier = Modifier.width(48.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryRow(
    request: ProxyRequestRecord,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val methodColor = when (request.method) {
        "GET" -> MaterialTheme.colorScheme.primary
        "POST" -> MaterialTheme.colorScheme.tertiary
        "PUT" -> MaterialTheme.colorScheme.secondary
        "DELETE" -> MaterialTheme.colorScheme.error
        "PATCH" -> MaterialTheme.colorScheme.inversePrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val statusColor = when {
        request.statusCode < 300 -> MaterialTheme.colorScheme.tertiary
        request.statusCode < 400 -> MaterialTheme.colorScheme.primary
        request.statusCode < 500 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$index", modifier = Modifier.width(32.dp), fontSize = 13.sp)

        Text(
            text = request.method,
            modifier = Modifier
                .width(72.dp)
                .background(methodColor.copy(alpha = 0.15f))
                .padding(horizontal = 2.dp),
            color = methodColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )

        Text(
            text = request.host,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
        Text(
            text = request.url,
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
        Text(
            text = "${request.statusCode}",
            modifier = Modifier.width(56.dp),
            fontSize = 13.sp,
            color = statusColor
        )
        Text("${request.length}", modifier = Modifier.width(48.dp), fontSize = 13.sp)
        Text(request.mimeType, modifier = Modifier.width(48.dp), fontSize = 13.sp)
    }
}
