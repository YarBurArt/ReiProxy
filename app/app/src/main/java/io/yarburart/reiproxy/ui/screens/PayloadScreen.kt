package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.IntruderPayload
import io.yarburart.reiproxy.PayloadRecord
import io.yarburart.reiproxy.getFileContent
import io.yarburart.reiproxy.ui.components.EmptyState

@Composable
fun PayloadScreen(
    modifier: Modifier = Modifier,
    payloads: List<PayloadRecord>,
    fuzzmeCount: Int,
    selectedPayloadType: String?,
    selectedIntruderPath: String?,
    selectedFuzzIndexes: Set<Int>,
    onSelectPayload: (String?) -> Unit,
    onToggleFuzzIndex: (Int) -> Unit,
    onUseIntruder: (PayloadRecord, IntruderPayload, Set<Int>) -> Unit,
) {
    val selectedPayload = payloads.firstOrNull { it.vulnType == selectedPayloadType }

    Surface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Payloads Library", style = MaterialTheme.typography.titleMedium)

            if (fuzzmeCount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Target Slots:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(fuzzmeCount) { index ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedFuzzIndexes.contains(index),
                                    onCheckedChange = { onToggleFuzzIndex(index) },
                                )
                                Text("#${index + 1}", fontSize = 12.sp)
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            if (payloads.isEmpty()) {
                EmptyState("No payloads loaded. Please sync from settings.")
                return@Column
            }

            payloads.forEach { payload ->
                val isSelected = selectedPayload?.vulnType == payload.vulnType
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = {
                                onSelectPayload(if (isSelected) null else payload.vulnType)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(payload.vulnType, modifier = Modifier.fillMaxWidth())
                        }
                        if (payload.intruders.isNotEmpty()) {
                            Text(
                                "${payload.intruders.size} sets",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSelected) {
                        // Show README explanation if exists
                        payload.readmePath?.let { path ->
                            val content = getFileContent(path)
                            if (content.isNotBlank()) {
                                Text(
                                    text = "Explanation:",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                Text(
                                    text = content.take(1000) + if (content.length > 1000) "..." else "",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Show Intruder sets if exists
                        if (payload.intruders.isEmpty()) {
                            Text(
                                text = "No direct intruder files, check README for manual payloads.",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "Available Intruder Sets:",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            payload.intruders.forEach { intruder ->
                                IntruderRow(
                                    intruder = intruder,
                                    selectedIntruderPath = selectedIntruderPath,
                                    onUse = { onUseIntruder(payload, intruder, selectedFuzzIndexes) },
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun IntruderRow(
    intruder: IntruderPayload,
    selectedIntruderPath: String?,
    onUse: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(intruder.name, style = MaterialTheme.typography.bodyMedium)
                Text("${intruder.lineCount} payloads", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onUse) {
                Text(if (selectedIntruderPath == intruder.path) "Use again" else "Use")
            }
        }
    }
}
