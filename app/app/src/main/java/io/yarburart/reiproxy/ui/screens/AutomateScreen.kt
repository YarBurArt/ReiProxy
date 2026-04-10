package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.ui.components.AdaptiveSplitPane
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedEditor
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedText

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes()
@Composable
fun AutomateScreen(
    modifier: Modifier = Modifier,
    selectedRequest: ProxyRequestRecord? = null,
) {
    val rawReq = selectedRequest?.rawRequest?.takeIf { it.isNotBlank() }
        ?: buildRawRequest(selectedRequest)
    var rawRequestText by remember { mutableStateOf(TextFieldValue(rawReq)) }
    val rawResp = selectedRequest?.rawResponse?.takeIf { it.isNotBlank() }
        ?: buildRawResponse(selectedRequest)

    val requestLines = rawRequestText.text.split("\n")
    val urlLine = requestLines.firstOrNull() ?: ""
    val hostHeader = requestLines.find { it.startsWith("Host:", ignoreCase = true) }
    val hostFromHeader = hostHeader?.substringAfter(":")?.trim()
    val fullUrl = hostFromHeader?.let { h ->
        val path = urlLine.split(" ").getOrNull(1) ?: ""
        "$h$path"
    } ?: urlLine

    Column(modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(onClick = { /* send */ }) { Text("Send") }
            TextButton(onClick = { /* cancel */ }) { Text("Cancel") }
            TextButton(onClick = { /* prev */ }) { Text("<") }
            TextButton(onClick = { /* next */ }) { Text(">") }
        }

        AdaptiveSplitPane(
            firstPane = {
                Column(modifier = Modifier.fillMaxSize().weight(1f).padding(8.dp)) {
                    Text("Request", modifier = Modifier.padding(bottom = 4.dp))
                    HorizontalDivider(modifier = Modifier, thickness = 1.dp)
                    Text("URL: $fullUrl", modifier = Modifier.padding(vertical = 4.dp))
                    SyntaxHighlightedEditor(
                        value = rawRequestText,
                        onValueChange = { rawRequestText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            },
            secondPane = {
                Column(modifier = Modifier.fillMaxSize().weight(1f).padding(8.dp)) {
                    Text("Response", modifier = Modifier.padding(bottom = 4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (rawResp.isNotBlank()) {
                            SyntaxHighlightedText(content = rawResp)
                        } else {
                            Text("No response captured")
                        }
                    }
                }
            }
        )
    }
}
