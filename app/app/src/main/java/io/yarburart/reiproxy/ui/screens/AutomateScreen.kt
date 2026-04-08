package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.yarburart.reiproxy.ui.components.AdaptiveSplitPane
import io.yarburart.reiproxy.ui.components.ExpandableSection
import io.yarburart.reiproxy.ui.components.NameValueRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomateScreen(modifier: Modifier = Modifier) {
    var url by remember { mutableStateOf("api.example.com:443/v1/users") }
    var requestBody by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val headers = listOf(
        HeaderEntry("Content-Type", "application/json"),
    )
    val params = listOf(
        ParamEntry("limit", "50"),
        ParamEntry("offset", "0"),
    )

    Column(modifier = modifier) {
        // Top toolbar: send | cancel | prev / next
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(onClick = { /* send */ }) { Text("Send") }
            TextButton(onClick = { /* cancel */ }) { Text("Cancel") }
            TextButton(onClick = { /* prev */ }) { Text("<") }
            TextButton(onClick = { /* next */ }) { Text(">") }
        }

        AdaptiveSplitPane(
            firstPane = {
                Column(modifier = Modifier.fillMaxSize().weight(1f).padding(8.dp)) {
                    Text("Request", modifier = Modifier.padding(8.dp))

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        placeholder = { Text("example.com:443") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = requestBody,
                        onValueChange = { requestBody = it },
                        label = { Text("Request Body") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        maxLines = Int.MAX_VALUE,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            secondPane = {
                Column(modifier = Modifier.fillMaxSize().weight(1f).padding(8.dp)) {
                    Text("Response", modifier = Modifier.padding(8.dp))

                    Text(
                        "Response body",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp)
                    )

                    ExpandableSection("Data") {
                        // Expandable list of params and headers
                        ExpandableSection("Headers") {
                            Column {
                                if (headers.isEmpty()) {
                                    Text("No headers", modifier = Modifier.padding(16.dp))
                                } else {
                                    for (header in headers) {
                                        NameValueRow(name = header.name, value = header.value)
                                    }
                                }
                            }
                        }

                        ExpandableSection("Params") {
                            Column {
                                if (params.isEmpty()) {
                                    Text("No params", modifier = Modifier.padding(16.dp))
                                } else {
                                    for (param in params) {
                                        NameValueRow(name = param.name, value = param.value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
