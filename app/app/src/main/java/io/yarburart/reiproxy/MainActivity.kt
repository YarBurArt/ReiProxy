package io.yarburart.reiproxy

import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import io.yarburart.reiproxy.ui.screens.AutomateScreen
import io.yarburart.reiproxy.ui.screens.DecodeScreen
import io.yarburart.reiproxy.ui.screens.HistoryScreen
import io.yarburart.reiproxy.ui.screens.HomeScreen
import io.yarburart.reiproxy.ui.screens.RepeatScreen
import io.yarburart.reiproxy.ui.screens.SettingsScreen
import io.yarburart.reiproxy.ui.theme.ReiProxyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReiProxyTheme {
                ReiProxyApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun ReiProxyApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

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
                AppDestinations.HOME -> HomeScreen(modifier = modifier)
                AppDestinations.HISTORY -> HistoryScreen(modifier = modifier)
                AppDestinations.AUTOMATE -> AutomateScreen(modifier = modifier)
                AppDestinations.REPEAT -> RepeatScreen(modifier = modifier)
                AppDestinations.DECODE -> DecodeScreen(modifier = modifier)
                AppDestinations.SETTINGS -> SettingsScreen(modifier = modifier)
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ReiProxyTheme {
        Greeting("Android")
    }
}
