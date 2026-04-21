package io.yarburart.reiproxy.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.ui.components.ScreenTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTheme(val label: String, val mode: String) {
    SYSTEM("System", "system"),
    DARK("Dark", "dark"),
    WHITE("White", "white"),
}

data class SettingsState(
    val proxyHost: String = "127.0.0.1",
    val proxyPort: String = "8080",
    val interceptEnabled: Boolean = false,
    val proxyRunning: Boolean = false,
    val handleSsl: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val certInfo: CertInfo? = null,
    val payloadCount: Int = 0,
)

data class CertInfo(
    val subject: String,
    val issuer: String,
    val notBefore: Date,
    val notAfter: Date,
    val serialNumber: String,
    val certPem: String,
    val privateKeyDer: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CertInfo
        return subject == other.subject && issuer == other.issuer &&
            notBefore == other.notBefore && notAfter == other.notAfter &&
            serialNumber == other.serialNumber && certPem == other.certPem &&
            privateKeyDer.contentEquals(other.privateKeyDer)
    }

    override fun hashCode(): Int {
        var result = subject.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + notBefore.hashCode()
        result = 31 * result + notAfter.hashCode()
        result = 31 * result + serialNumber.hashCode()
        result = 31 * result + certPem.hashCode()
        result = 31 * result + privateKeyDer.contentHashCode()
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsState = SettingsState(),
    onHostChange: (String) -> Unit = {},
    onPortChange: (String) -> Unit = {},
    onInterceptToggle: (Boolean) -> Unit = {},
    onStartStopProxy: () -> Unit = {},
    onThemeChange: (AppTheme) -> Unit = {},
    onGenerateCert: () -> Unit = {},
    onExportCert: (CertInfo, Context) -> Unit = { _, _ -> },
    onSyncPayloads: () -> Unit = {},
) {
    val context = LocalContext.current

    var showCertDialog by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    val themeOptions = AppTheme.entries.toList()

    LazyColumn(modifier = modifier.padding(16.dp)) {
        item { ScreenTitle("Settings") }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

        item { SectionHeader("Proxy") }
        item {
            OutlinedTextField(
                value = state.proxyHost,
                onValueChange = onHostChange,
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.proxyRunning,
            )
        }
        item {
            OutlinedTextField(
                value = state.proxyPort,
                onValueChange = onPortChange,
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.proxyRunning,
            )
        }
        item {
            Button(
                onClick = onStartStopProxy,
                modifier = Modifier.padding(vertical = 8.dp),
                enabled = state.proxyHost.isNotBlank() && state.proxyPort.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.proxyRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(if (state.proxyRunning) "Stop Proxy" else "Start Proxy")
            }
        }
        item {
            ListItem(
                headlineContent = { Text("Enable Interception") },
                trailingContent = { Switch(checked = state.interceptEnabled, onCheckedChange = onInterceptToggle) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Handle HTTPS (MITM)") },
                trailingContent = { Switch(checked = state.handleSsl, onCheckedChange = {}) }
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

        item { SectionHeader("Appearance") }
        item {
            ExposedDropdownMenuBox(
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = it },
            ) {
                TextField(
                    value = state.theme.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Theme") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = themeExpanded,
                    onDismissRequest = { themeExpanded = false },
                ) {
                    themeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onThemeChange(option)
                                themeExpanded = false
                            },
                        )
                    }
                }
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

        item { SectionHeader("Payloads (PayloadsAllTheThings)") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSyncPayloads, modifier = Modifier.weight(1f)) {
                    Text("Sync from GitHub")
                }
                OutlinedTextField(
                    value = if (state.payloadCount > 0) "${state.payloadCount} Categories" else "Not loaded",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    label = { Text("Status") }
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

        item { SectionHeader("Certificate") }
        item {
            if (state.certInfo != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Subject: ${state.certInfo.subject}")
                        Text("Serial: ${state.certInfo.serialNumber}")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onExportCert(state.certInfo, context) }, modifier = Modifier.weight(1f)) {
                        Text("Export")
                    }
                    Button(onClick = { showCertDialog = true }, modifier = Modifier.weight(1f)) {
                        Text("View")
                    }
                }
            } else {
                Button(onClick = onGenerateCert) { Text("Generate CA Cert") }
            }
        }
    }

    if (showCertDialog && state.certInfo != null) {
        AlertDialog(
            onDismissRequest = { showCertDialog = false },
            title = { Text("CA Certificate") },
            text = { Text(state.certInfo.certPem, fontSize = 10.sp) },
            confirmButton = { TextButton(onClick = { showCertDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
}
