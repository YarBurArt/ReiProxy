package io.yarburart.reiproxy.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 24.sp,
        modifier = modifier
    )
}
