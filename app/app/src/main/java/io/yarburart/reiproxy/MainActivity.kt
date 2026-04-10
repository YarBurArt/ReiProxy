package io.yarburart.reiproxy

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.github.monkeywie.proxyee.crt.CertUtil
import io.yarburart.reiproxy.core.ProxyConfig
import io.yarburart.reiproxy.core.ProxyManager
import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.data.HistoryRepository
import io.yarburart.reiproxy.data.Project
import io.yarburart.reiproxy.data.ProjectRepository
import io.yarburart.reiproxy.data.SettingsManager
import io.yarburart.reiproxy.db.ReiProxyDatabase
import io.yarburart.reiproxy.ui.screens.AppTheme
import io.yarburart.reiproxy.ui.screens.AutomateScreen
import io.yarburart.reiproxy.ui.screens.CertInfo
import io.yarburart.reiproxy.ui.screens.DecodeScreen
import io.yarburart.reiproxy.ui.screens.HistoryScreen
import io.yarburart.reiproxy.ui.screens.HomeScreen
import io.yarburart.reiproxy.ui.screens.RepeatScreen
import io.yarburart.reiproxy.ui.screens.SettingsScreen
import io.yarburart.reiproxy.ui.screens.SettingsState
import io.yarburart.reiproxy.ui.theme.ReiProxyTheme
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ReiProxyDatabase.getDatabase(this)
        val projectRepository = ProjectRepository(database.projectDao())
        val historyRepository = HistoryRepository(database.requestHistoryDao())
        val settingsManager = SettingsManager(this)

        ProxyManager.setHistoryRepository(historyRepository)

        setContent {
            var appTheme by remember { mutableStateOf(AppTheme.SYSTEM) }
            var activeProjectId by rememberSaveable { mutableStateOf<Long?>(null) }

            // Sync active project to ProxyManager for DB saves
            activeProjectId?.let { ProxyManager.setActiveProjectId(it) }

            ReiProxyTheme(themeMode = appTheme.mode) {
                ReiProxyApp(
                    theme = appTheme,
                    onThemeChange = { appTheme = it },
                    projectRepository = projectRepository,
                    historyRepository = historyRepository,
                    settingsManager = settingsManager,
                    activeProjectId = activeProjectId,
                    onProjectSelected = { activeProjectId = it },
                    onThemeFromSettings = { appTheme = it },
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun ReiProxyApp(
    theme: AppTheme = AppTheme.SYSTEM,
    onThemeChange: (AppTheme) -> Unit = {},
    projectRepository: ProjectRepository? = null,
    historyRepository: HistoryRepository? = null,
    settingsManager: SettingsManager? = null,
    activeProjectId: Long? = null,
    onProjectSelected: (Long?) -> Unit = {},
    onThemeFromSettings: (AppTheme) -> Unit = {},
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val proxyConfig by ProxyManager.activeConfig.collectAsState()
    val isRunning by ProxyManager.isRunning.collectAsState()

    // DB-backed history flow
    val requestHistory: List<ProxyRequestRecord> = if (activeProjectId != null && historyRepository != null) {
        historyRepository.getHistoryByProject(activeProjectId).collectAsState(emptyList()).value
    } else {
        ProxyManager.requestHistory.collectAsState().value
    }

    // Projects flow
    val projects: List<Project> = if (projectRepository != null) {
        projectRepository.getAllProjects().collectAsState(emptyList()).value
    } else {
        emptyList()
    }

    // Per-project settings flow
    val projectSettings: io.yarburart.reiproxy.data.ProjectSettings? = if (activeProjectId != null && settingsManager != null) {
        settingsManager.getSettingsFlow(activeProjectId).collectAsState(null).value
    } else {
        null
    }

    val scope = rememberCoroutineScope()

    // Settings state (from DB or defaults)
    var settingsHost by remember { mutableStateOf(projectSettings?.host ?: proxyConfig.host) }
    var settingsPort by remember { mutableStateOf(projectSettings?.port?.toString() ?: proxyConfig.port.toString()) }
    var interceptEnabled by remember { mutableStateOf(projectSettings?.interceptEnabled ?: false) }
    var handleSsl by remember { mutableStateOf(projectSettings?.handleSsl ?: true) }
    var currentCert by remember { mutableStateOf<CertInfo?>(null) }

    // Sync settings when projectSettings changes
    if (projectSettings != null) {
        settingsHost = projectSettings!!.host
        settingsPort = projectSettings!!.port.toString()
        interceptEnabled = projectSettings!!.interceptEnabled
        handleSsl = projectSettings!!.handleSsl
    }

    // Shared selected request for Repeat/Automate
    var selectedRequest by remember { mutableStateOf<ProxyRequestRecord?>(null) }

    val settingsState = SettingsState(
        proxyHost = settingsHost,
        proxyPort = settingsPort,
        interceptEnabled = interceptEnabled,
        proxyRunning = isRunning,
        handleSsl = handleSsl,
        theme = theme,
        certInfo = currentCert,
    )

    fun handleStartStopProxy() {
        if (activeProjectId == null) return
        scope.launch {
            if (isRunning) {
                ProxyManager.stop()
            } else {
                val port = settingsPort.toIntOrNull() ?: 8080
                val config = ProxyConfig(
                    host = settingsHost,
                    port = port,
                    handleSsl = handleSsl,
                    interceptEnabled = interceptEnabled,
                )
                ProxyManager.updateConfig(config)
                // Save settings for this project
                settingsManager?.updateHost(activeProjectId, settingsHost)
                settingsManager?.updatePort(activeProjectId, port)
                settingsManager?.updateInterceptEnabled(activeProjectId, interceptEnabled)
                settingsManager?.updateHandleSsl(activeProjectId, handleSsl)
                ProxyManager.start(config)
            }
        }
    }

    fun handleGenerateCert(): CertInfo {
        val cert = generateDefaultCert()
        currentCert = cert
        return cert
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    modifier = modifier,
                    projects = projects,
                    activeProjectId = activeProjectId,
                    historyCount = requestHistory.size,
                    proxyRunning = isRunning,
                    onProjectSelected = onProjectSelected,
                    onProjectCreated = { name, desc ->
                        projectRepository?.let { repo ->
                            scope.launch { repo.insertProject(name, desc) }
                        }
                    },
                    onProjectDeleted = { project ->
                        projectRepository?.let { repo ->
                            scope.launch { repo.deleteProject(project) }
                        }
                    },
                    onStartProxy = { handleStartStopProxy() },
                    onStopProxy = { handleStartStopProxy() },
                )
                AppDestinations.HISTORY -> HistoryScreen(
                    modifier = modifier,
                    requests = requestHistory,
                    onClear = {
                        if (activeProjectId != null) {
                            scope.launch { historyRepository?.clearHistoryByProject(activeProjectId) }
                        } else {
                            ProxyManager.clearHistory()
                        }
                    },
                    onRequestSelected = { selectedRequest = it },
                )
                AppDestinations.AUTOMATE -> AutomateScreen(
                    modifier = modifier,
                    selectedRequest = selectedRequest,
                )
                AppDestinations.REPEAT -> RepeatScreen(
                    modifier = modifier,
                    selectedRequest = selectedRequest,
                )
                AppDestinations.DECODE -> DecodeScreen(modifier = modifier)
                AppDestinations.SETTINGS -> SettingsScreen(
                    modifier = modifier,
                    state = settingsState,
                    onHostChange = { settingsHost = it },
                    onPortChange = { settingsPort = it },
                    onInterceptToggle = { interceptEnabled = it },
                    onStartStopProxy = { handleStartStopProxy() },
                    onThemeChange = { newTheme ->
                        onThemeChange(newTheme)
                        if (activeProjectId != null) {
                            scope.launch {
                                settingsManager?.updateThemeMode(activeProjectId, newTheme.mode)
                            }
                        }
                    },
                    onGenerateCert = { handleGenerateCert() },
                    onExportCert = { cert, ctx -> exportCert(cert, ctx) },
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.outline_deployed_code_24),
    HISTORY("History", R.drawable.rounded_format_list_bulleted_24),
    AUTOMATE("Auto", R.drawable.outline_eye_tracking_24),
    REPEAT("Repeat", R.drawable.outline_autorenew_24),
    DECODE("Decode", R.drawable.outline_encrypted_24),
    SETTINGS("Settings", R.drawable.rounded_dashboard_2_gear_24)
}

// ---------- Certificate helpers (moved from SettingsScreen) ----------

private fun generateDefaultCert(): CertInfo {
    val keyPair = CertUtil.genKeyPair()
    val now = Date()
    val expiry = Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650))
    val caCert = CertUtil.genCACert(
        "C=CN, ST=GD, L=SZ, O=ReiProxy, OU=Dev, CN=ReiProxy CA",
        now,
        expiry,
        keyPair,
    )

    return CertInfo(
        subject = CertUtil.getSubject(caCert),
        issuer = caCert.issuerX500Principal.name,
        notBefore = caCert.notBefore,
        notAfter = caCert.notAfter,
        serialNumber = caCert.serialNumber.toString(16).uppercase(),
        certPem = toPem(caCert.encoded, "CERTIFICATE"),
        privateKeyDer = keyPair.private.encoded,
    )
}

private fun toPem(der: ByteArray, label: String): String {
    val base64 = android.util.Base64.encodeToString(der, android.util.Base64.NO_WRAP)
    val body = base64.windowed(64, 64, true).joinToString("\n")
    return "-----BEGIN $label-----\n$body\n-----END $label-----\n"
}

private fun exportCert(certInfo: CertInfo, context: Context) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/x-x509-ca-cert"
        putExtra(Intent.EXTRA_TITLE, "reiproxy_ca.crt")
    }
    try {
        (context as? Activity)?.startActivityForResult(intent, 1001)
        Toast.makeText(context, "Select a location to save the certificate", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open file picker: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
