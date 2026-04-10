package io.yarburart.reiproxy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

@Composable
fun ResizableSplitPane(
    modifier: Modifier = Modifier,
    firstPane: @Composable () -> Unit,
    secondPane: @Composable () -> Unit,
) {
    val windowWidthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    var splitRatio by remember { mutableFloatStateOf(0.5f) }
    var containerSizePx by remember { mutableFloatStateOf(0f) }

    if (windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
        Row(modifier = modifier.fillMaxSize()
            .onGloballyPositioned { containerSizePx = it.size.width.toFloat() }) {
            Box(modifier = Modifier.fillMaxHeight()
                .weight(splitRatio).clipToBounds()) { firstPane() }
            DraggableDivider(
                modifier = Modifier.width(6.dp).fillMaxHeight(),
                containerSizePx = containerSizePx,
                horizontal = true,
                onDrag = {
                    delta -> splitRatio = (splitRatio + delta / containerSizePx)
                        .coerceIn(0.2f, 0.8f)
                    },
            )
            Box(modifier = Modifier.fillMaxHeight()
                .weight(1f - splitRatio).clipToBounds()) { secondPane() }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()
            .onGloballyPositioned { containerSizePx = it.size.height.toFloat() }) {
            Box(modifier = Modifier.fillMaxWidth()
                .weight(splitRatio).clipToBounds()) { firstPane() }
            DraggableDivider(
                modifier = Modifier.fillMaxWidth().height(14.dp),
                containerSizePx = containerSizePx,
                horizontal = false,
                onDrag = {
                    delta -> splitRatio = (splitRatio + delta / containerSizePx)
                        .coerceIn(0.2f, 0.8f)
                    },
            )
            Box(modifier = Modifier.fillMaxWidth()
                .weight(1f - splitRatio).clipToBounds()) { secondPane() }
        }
    }
}

@Composable
private fun DraggableDivider(
    modifier: Modifier,
    containerSizePx: Float,
    horizontal: Boolean,
    onDrag: (Float) -> Unit,
) {
    Box(
        modifier = modifier
            .pointerInput(containerSizePx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (containerSizePx > 0) {
                        onDrag(if (horizontal) dragAmount.x else dragAmount.y)
                    }
                }
            },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        HomeIndicatorPill(horizontal = horizontal)
    }
}

@Composable
private fun HomeIndicatorPill(horizontal: Boolean) {
    if (horizontal) {
        Spacer(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF888888))
        )
    } else {
        Spacer(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF888888))
        )
    }
}
