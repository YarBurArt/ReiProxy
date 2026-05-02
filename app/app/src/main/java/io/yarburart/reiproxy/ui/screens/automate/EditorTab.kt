package io.yarburart.reiproxy.ui.screens.automate

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.ui.components.ResizableSplitPane
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedEditor
import io.yarburart.reiproxy.ui.components.SyntaxHighlightedText
import io.yarburart.reiproxy.ui.state.AutomateState

@Composable
fun EditorTab(state: AutomateState) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextButton(
                onClick = { state.handleSend() },
                enabled = !state.isSending && !state.isAttacking
            ) { Text("Send") }
            
            if (state.isSending) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            
            TextButton(onClick = { state.handleAttack() }) { 
                Text(if (state.isAttacking) "Stop Attack" else "Attack") 
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = state.repeaterHistory.isNotEmpty()) { 
                            state.loadFromHistory(state.historyIndex - 1) 
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos, 
                        contentDescription = "Previous", 
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = state.repeaterHistory.isNotEmpty()) { 
                            state.loadFromHistory(state.historyIndex + 1) 
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos, 
                        contentDescription = "Next", 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (state.repeaterHistory.isNotEmpty() && state.historyIndex >= 0) {
                Text(
                    "${state.historyIndex + 1}/${state.repeaterHistory.size}", 
                    fontSize = 12.sp, 
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            TextButton(onClick = { state.handleAddFuzzme() }) { Text("Add FUZZME") }
            TextButton(onClick = { state.handleClearFuzzme() }) { Text("Clear FUZZME") }
            TextButton(onClick = { state.showPayloads = true }) { Text("Payloads lib") }
        }

        HorizontalDivider()
        Text(
            text = "URL: ${state.fullUrl}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
        )
        Text(
            text = "FUZZME slots found: ${state.fuzzmeCount}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        
        if (state.assignedPayloads.isNotEmpty()) {
            Text(
                text = "Assignments: " + state.assignedPayloads.entries.asSequence().sortedBy { it.key }
                    .joinToString(" | ") { "#${it.key + 1}: ${it.value.name}" },
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
                        value = state.rawRequestText,
                        onValueChange = { state.rawRequestText = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            },
            secondPane = {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                    if (state.rawResponseText.isNotBlank()) {
                        SyntaxHighlightedText(content = state.rawResponseText)
                    } else {
                        Text("No response captured", modifier = Modifier.padding(8.dp))
                    }
                }
            },
        )
    }
}
