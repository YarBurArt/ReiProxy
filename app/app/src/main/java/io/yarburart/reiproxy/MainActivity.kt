package io.yarburart.reiproxy

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp

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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize repositories and settings
        val database = ReiProxyDatabase.getDatabase(this)
        val projectRepository = ProjectRepository(database.projectDao())
        val historyRepository = HistoryRepository(database.requestHistoryDao())
        val settingsManager = SettingsManager(this)

        // Configure proxy manager with history tracking
        ProxyManager.setHistoryRepository(historyRepository)

        setContent {
            var appTheme by remember { mutableStateOf(AppTheme.SYSTEM) }
            var activeProjectId by rememberSaveable { mutableStateOf<Long?>(null) }

            // Set active project for proxy history operations
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
                )
            }
        }
    }
}

data class AppScreen(
    val isRunning: Boolean,
    val projects: List<Project>,
    val requestHistory: List<ProxyRequestRecord>,
)

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
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var selectedRequest by remember { mutableStateOf<ProxyRequestRecord?>(null) }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var payloads by remember { mutableStateOf(loadPayloads(appContext)) }

    val screen = rememberScreenState(
        activeProjectId, projectRepository, historyRepository, settingsManager)
    val projectSettings = rememberProjectSettings(activeProjectId, settingsManager)
    val proxyConfig by ProxyManager.activeConfig.collectAsState()
    val scope = rememberCoroutineScope()

    val settings = rememberSettingsState(projectSettings, proxyConfig)
    val actions = rememberProxyActions(
        scope = scope,
        activeProjectId = activeProjectId,
        settingsManager = settingsManager,
        settings = settings,
        screen = screen,
        onThemeChange = onThemeChange,
    )

    var currentCert by remember { mutableStateOf<CertInfo?>(null) }
    val settingsState = SettingsState(
        proxyHost = settings.host,
        proxyPort = settings.port,
        interceptEnabled = settings.interceptEnabled,
        proxyRunning = screen.isRunning,
        handleSsl = settings.handleSsl,
        theme = theme,
        certInfo = currentCert,
        payloadCount = payloads.size,
    )

    // Navigation hub that routes to different app screens based on destination
    NavigationSuite(currentDestination, { currentDestination = it }) { padding ->
        when (currentDestination) {
            AppDestinations.HOME -> HomeContent(
                padding, screen, activeProjectId, onProjectSelected,
                projectRepository, scope, actions.startStopProxy)
            AppDestinations.HISTORY -> HistoryContent(
                padding, screen, activeProjectId, historyRepository, scope,
                onRequestSelected = { selectedRequest = it },
                onNavigateToRepeat = { currentDestination = AppDestinations.REPEAT },
            )
            AppDestinations.AUTOMATE -> AutomateContent(
                padding, selectedRequest, historyRepository, activeProjectId, screen, payloads)
            AppDestinations.REPEAT -> RepeatContent(
                padding, selectedRequest, historyRepository, activeProjectId, screen)
            AppDestinations.DECODE -> DecodeScreen(modifier = padding)
            AppDestinations.SETTINGS -> SettingsContent(
                padding, settingsState, settings, actions,
                onPayloadsImported = { imported ->
                    payloads = imported
                    Toast.makeText(appContext, "Imported ${imported.size} payload categories", Toast.LENGTH_SHORT).show()
                },
            ) { currentCert = it }
        }
    }
}

@Composable
private fun rememberProjectSettings(
    activeProjectId: Long?,
    settingsManager: SettingsManager?,
): io.yarburart.reiproxy.data.ProjectSettings? {
    return if (activeProjectId != null && settingsManager != null) {
        settingsManager.getSettingsFlow(activeProjectId).collectAsState(null).value
    } else {
        null
    }
}

@Composable
private fun rememberScreenState(
    activeProjectId: Long?,
    projectRepository: ProjectRepository?,
    historyRepository: HistoryRepository?,
    settingsManager: SettingsManager?,
): AppScreen {
    val isRunning by ProxyManager.isRunning.collectAsState()

    val requestHistory: List<ProxyRequestRecord> =
        if (activeProjectId != null && historyRepository != null) {
            historyRepository.getHistoryByProject(
                activeProjectId).collectAsState(emptyList()).value
        } else {
            ProxyManager.requestHistory.collectAsState().value
        }

    val projects = projectRepository?.getAllProjects()?.collectAsState(
        emptyList()
    )?.value ?: emptyList()

    return remember(isRunning, projects, requestHistory) {
        AppScreen(isRunning, projects, requestHistory)
    }
}

data class SettingsStateHolder(
    var host: String,
    var port: String,
    var interceptEnabled: Boolean,
    var handleSsl: Boolean,
)

@Composable
private fun rememberSettingsState(
    projectSettings: io.yarburart.reiproxy.data.ProjectSettings?,
    proxyConfig: ProxyConfig,
): SettingsStateHolder {
    val state = remember {
        mutableStateOf(
            SettingsStateHolder(
                host = projectSettings?.host ?: proxyConfig.host,
                port = projectSettings?.port?.toString() ?: proxyConfig.port.toString(),
                interceptEnabled = projectSettings?.interceptEnabled ?: false,
                handleSsl = projectSettings?.handleSsl ?: true,
            )
        )
    }

    LaunchedEffect(projectSettings, proxyConfig) {
        state.value = SettingsStateHolder(
            host = projectSettings?.host ?: proxyConfig.host,
            port = projectSettings?.port?.toString() ?: proxyConfig.port.toString(),
            interceptEnabled = projectSettings?.interceptEnabled ?: false,
            handleSsl = projectSettings?.handleSsl ?: true,
        )
    }

    return state.value
}

// Core proxy operations: start/stop server, generate SSL cert, change theme
data class ProxyActions(
    val startStopProxy: () -> Unit,
    val generateCert: () -> CertInfo,
    val changeTheme: (AppTheme) -> Unit,
)

@Composable
private fun rememberProxyActions(
    scope: CoroutineScope,
    activeProjectId: Long?,
    settingsManager: SettingsManager?,
    settings: SettingsStateHolder,
    screen: AppScreen,
    onThemeChange: (AppTheme) -> Unit,
): ProxyActions {
    val currentActiveId by rememberUpdatedState(activeProjectId)
    val currentSettings by rememberUpdatedState(settings)
    val currentScreen by rememberUpdatedState(screen)
    val currentOnThemeChange by rememberUpdatedState(onThemeChange)

    return remember {
        ProxyActions(
            startStopProxy = start@{
                val projectId = currentActiveId ?: return@start
                scope.launch {
                    if (currentScreen.isRunning) {
                        ProxyManager.stop()
                    } else {
                        val port = currentSettings.port.toIntOrNull() ?: 8080
                        val config = ProxyConfig(
                            currentSettings.host, port,
                            currentSettings.handleSsl,
                            currentSettings.interceptEnabled)
                        ProxyManager.updateConfig(config)
                        settingsManager?.updateHost(projectId, currentSettings.host)
                        settingsManager?.updatePort(projectId, port)
                        settingsManager?.updateInterceptEnabled(
                            projectId, currentSettings.interceptEnabled)
                        settingsManager?.updateHandleSsl(
                            projectId, currentSettings.handleSsl)
                        ProxyManager.start(config)
                    }
                }
            },
            generateCert = { generateDefaultCert() },
            changeTheme = { newTheme ->
                currentOnThemeChange(newTheme)
                val projectId = currentActiveId
                if (projectId != null) {
                    scope.launch {
                        settingsManager?.updateThemeMode(projectId, newTheme.mode)
                    }
                }
            },
        )
    }
}

@Composable
private fun NavigationSuite(
    currentDestination: AppDestinations,
    onNavigate: (AppDestinations) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { dest ->
                item(
                    icon = { Icon(painterResource(dest.icon), contentDescription = dest.label) },
                    label = { Text(dest.label) },
                    selected = dest == currentDestination,
                    onClick = { onNavigate(dest) },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        },
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    screen: AppScreen,
    activeProjectId: Long?,
    onProjectSelected: (Long?) -> Unit,
    projectRepository: ProjectRepository?,
    scope: CoroutineScope,
    onStartStop: () -> Unit,
) {
    HomeScreen(
        modifier = modifier,
        projects = screen.projects,
        activeProjectId = activeProjectId,
        historyCount = screen.requestHistory.size,
        proxyRunning = screen.isRunning,
        onProjectSelected = onProjectSelected,
        onProjectCreated = { name, desc ->
            projectRepository?.let { scope.launch { it.insertProject(name, desc) } }
        },
        onProjectDeleted = { project ->
            projectRepository?.let { scope.launch { it.deleteProject(project) } }
        },
        onStartProxy = onStartStop,
        onStopProxy = onStartStop,
    )
}

@Composable
private fun HistoryContent(
    modifier: Modifier,
    screen: AppScreen,
    activeProjectId: Long?,
    historyRepository: HistoryRepository?,
    scope: CoroutineScope,
    onRequestSelected: (ProxyRequestRecord?) -> Unit,
    onNavigateToRepeat: () -> Unit,
) {
    HistoryScreen(
        modifier = modifier,
        requests = screen.requestHistory,
        onClear = {
            if (activeProjectId != null) {
                scope.launch { historyRepository?.clearHistoryByProject(activeProjectId) }
            } else {
                ProxyManager.clearHistory()
            }
        },
        onRequestSelected = onRequestSelected,
        onNavigateToRepeat = onNavigateToRepeat,
    )
}

@Composable
private fun RepeatContent(
    modifier: Modifier,
    selectedRequest: ProxyRequestRecord?,
    historyRepository: HistoryRepository?,
    activeProjectId: Long?,
    screen: AppScreen,
) {
    RepeatScreen(
        modifier = modifier,
        selectedRequest = selectedRequest,
        historyRepository = historyRepository,
        activeProjectId = activeProjectId,
        requests = screen.requestHistory,
    )
}

@Composable
private fun AutomateContent(
    modifier: Modifier,
    selectedRequest: ProxyRequestRecord?,
    historyRepository: HistoryRepository?,
    activeProjectId: Long?,
    screen: AppScreen,
    payloads: List<PayloadRecord>,
) {
    AutomateScreen(
        modifier = modifier,
        selectedRequest = selectedRequest,
        historyRepository = historyRepository,
        activeProjectId = activeProjectId,
        requests = screen.requestHistory,
        payloads = payloads,
    )
}

@Composable
private fun SettingsContent(
    modifier: Modifier,
    state: SettingsState,
    settings: SettingsStateHolder,
    actions: ProxyActions,
    onPayloadsImported: (List<PayloadRecord>) -> Unit,
    onCertGenerated: (CertInfo) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    SettingsScreen(
        modifier = modifier,
        state = state,
        onHostChange = { settings.host = it },
        onPortChange = { settings.port = it },
        onInterceptToggle = { settings.interceptEnabled = it },
        onStartStopProxy = { actions.startStopProxy() },
        onThemeChange = { actions.changeTheme(it) },
        onGenerateCert = {
            val cert = actions.generateCert()
            onCertGenerated(cert)
            cert
        },
        onExportCert = { _, ctx -> exportCert(ctx) },
        onSyncPayloads = {
            scope.launch {
                val results = fetchPayloadsFromGithub(context) { progress ->
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, progress, Toast.LENGTH_SHORT).show()
                    }
                }
                onPayloadsImported(results)
            }
        }
    )
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
    SETTINGS("Settings", R.drawable.rounded_dashboard_2_gear_24),
}
