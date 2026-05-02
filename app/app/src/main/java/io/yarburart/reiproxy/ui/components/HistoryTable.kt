package io.yarburart.reiproxy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.core.ProxyRequestRecord

enum class HistorySortOption {
    TIME, STATUS_CODE, LENGTH
}

@Composable
fun HistoryTableHeader(
    onSort: (HistorySortOption) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            "#",
            modifier = Modifier.width(32.dp).clickable { onSort(HistorySortOption.TIME) },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Method",
            modifier = Modifier.width(72.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Host",
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "URL",
            modifier = Modifier.weight(2f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Status",
            modifier = Modifier.width(56.dp).clickable { onSort(HistorySortOption.STATUS_CODE) },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Len",
            modifier = Modifier.width(48.dp).clickable { onSort(HistorySortOption.LENGTH) },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Type",
            modifier = Modifier.width(48.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HistoryTableRow(
    request: ProxyRequestRecord,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val methodColor = when (request.method) {
        "GET" -> MaterialTheme.colorScheme.primary
        "POST" -> MaterialTheme.colorScheme.tertiary
        "PUT" -> MaterialTheme.colorScheme.secondary
        "DELETE" -> MaterialTheme.colorScheme.error
        "PATCH" -> MaterialTheme.colorScheme.inversePrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val statusColor = when {
        request.statusCode < 300 -> MaterialTheme.colorScheme.tertiary
        request.statusCode < 400 -> MaterialTheme.colorScheme.primary
        request.statusCode < 500 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(index.toString(), modifier = Modifier.width(32.dp), fontSize = 13.sp)

        Text(
            text = request.method,
            modifier = Modifier
                .width(72.dp)
                .background(methodColor.copy(alpha = 0.15f))
                .padding(horizontal = 2.dp),
            color = methodColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )

        Text(
            text = request.host,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
        Text(
            text = request.url,
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
        Text(
            text = "${request.statusCode}",
            modifier = Modifier.width(56.dp),
            fontSize = 13.sp,
            color = statusColor
        )
        Text("${request.length}", modifier = Modifier.width(48.dp), fontSize = 13.sp)
        Text(request.mimeType, modifier = Modifier.width(48.dp), fontSize = 13.sp)
    }
}
