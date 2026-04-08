package io.yarburart.reiproxy.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowWidthSizeClass

@Suppress("DEPRECATION")
@Composable
fun AdaptiveSplitPane(
    modifier: Modifier = Modifier,
    firstPane: @Composable () -> Unit,
    secondPane: @Composable () -> Unit,
) {
    val windowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    if (windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
        Row(modifier = modifier.fillMaxSize()) {
            firstPane()
            secondPane()
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            firstPane()
            secondPane()
        }
    }
}
