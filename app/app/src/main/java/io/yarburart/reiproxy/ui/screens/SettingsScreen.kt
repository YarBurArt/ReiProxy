package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.yarburart.reiproxy.ui.components.ScreenTitle

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var proxyHost by remember { mutableStateOf("") }
    var proxyPort by remember { mutableStateOf("") }
    var darkTheme by remember { mutableStateOf(false) }
    var logLevel by remember { mutableStateOf("info") }
    var interceptEnabled by remember { mutableStateOf(false) }
    var sslStrip by remember { mutableStateOf(false) }
    var autoDecode by remember { mutableStateOf(true) }

    Column(modifier = modifier.padding(16.dp)) {
        ScreenTitle("Settings")

        LazyColumn {
            item {
                Text("Proxy", modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                TextField(
                    value = proxyHost,
                    onValueChange = { proxyHost = it },
                    label = { Text("Proxy Host") },
                    placeholder = { Text("127.0.0.1") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                TextField(
                    value = proxyPort,
                    onValueChange = { proxyPort = it },
                    label = { Text("Proxy Port") },
                    placeholder = { Text("8080") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { Text("Appearance", modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Dark Theme") },
                    trailingContent = {
                        Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { Text("Interception", modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Enable Interception") },
                    trailingContent = {
                        Switch(checked = interceptEnabled, onCheckedChange = { interceptEnabled = it })
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("SSL Strip") },
                    trailingContent = {
                        Switch(checked = sslStrip, onCheckedChange = { sslStrip = it })
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { Text("Decoding", modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Auto-decode Responses") },
                    trailingContent = {
                        Switch(checked = autoDecode, onCheckedChange = { autoDecode = it })
                    }
                )
            }
            item {
                TextField(
                    value = logLevel,
                    onValueChange = { logLevel = it },
                    label = { Text("Log Level") },
                    placeholder = { Text("debug, info, warn, error") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
