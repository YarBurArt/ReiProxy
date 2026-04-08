package io.yarburart.reiproxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.yarburart.reiproxy.ui.components.AdaptiveSplitPane
import io.yarburart.reiproxy.ui.components.EmptyState
import io.yarburart.reiproxy.ui.components.ScreenTitle

data class RequestRecord(
    val id: String,
    val method: String,
    val host: String,
    val url: String,
    val statusCode: Int,
    val length: Int,
    val mimeType: String,
    val requestBody: String,
    val responseBody: String,
    val extension: String,
)

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val requests = listOf(
        RequestRecord(
            id = "1",
            method = "POST",
            host = "api.example.com",
            url = "/v1/auth/login",
            statusCode = 200,
            length = 184,
            mimeType = "json",
            extension = "",
            requestBody = """POST /v1/auth/login HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbG...

{
  "username": "admin",
  "password": "testme"
}""".trimIndent(),
            responseBody = """HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 184

{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expires_in": 3600,
  "refresh_token": "dGhpcyBpcyBhIHJlZnJl..."
}""".trimIndent(),
        ),
        RequestRecord(
            id = "2",
            method = "GET",
            host = "api.example.com",
            url = "/v1/users/profile?fields=name,email",
            statusCode = 403,
            length = 68,
            mimeType = "json",
            extension = "",
            requestBody = """GET /v1/users/profile?fields=name,email HTTP/1.1
Host: api.example.com
Authorization: Bearer invalid_token""".trimIndent(),
            responseBody = """HTTP/1.1 403 Forbidden
Content-Type: application/json
Content-Length: 68

{
  "error": "Forbidden",
  "message": "Invalid or expired token"
}""".trimIndent(),
        ),
    )
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var viewTab by remember { mutableIntStateOf(0) } // 0=Request, 1=Response

    Column(modifier = modifier) {
        ScreenTitle("History", modifier = Modifier.padding(16.dp))

        if (requests.isEmpty()) {
            EmptyState("No requests recorded")
        } else {
            AdaptiveSplitPane(
                firstPane = {
                    Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                        // Header row
                        HistoryHeader()
                        HorizontalDivider()
                        // Request list
                        LazyColumn {
                            itemsIndexed(requests) { index, request ->
                                HistoryRow(
                                    request = request,
                                    index = index,
                                    isSelected = selectedIndex == index,
                                    onClick = { selectedIndex = index }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                },
                secondPane = {
                    Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                        if (selectedIndex != null) {
                            val selected = requests[selectedIndex!!]

                            // Request / Response tabs
                            val options = listOf("Request", "Response")
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                options.forEachIndexed { i, label ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = i,
                                            count = options.size
                                        ),
                                        onClick = { viewTab = i },
                                        selected = viewTab == i,
                                    ) {
                                        Text(label)
                                    }
                                }
                            }

                            // Content
                            val content = if (viewTab == 0) selected.requestBody else selected.responseBody
                            Text(
                                text = content,
                                fontSize = 13.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        } else {
                            EmptyState("Select a request to view details")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text("#", modifier = Modifier.width(32.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Method", modifier = Modifier.width(72.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Host", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("URL", modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Status", modifier = Modifier.width(56.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Len", modifier = Modifier.width(48.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Type", modifier = Modifier.width(48.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryRow(
    request: RequestRecord,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
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
        Text("$index", modifier = Modifier.width(32.dp), fontSize = 13.sp)
        Text(
            text = request.method,
            modifier = Modifier.width(72.dp),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
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
            color = if (request.statusCode < 400)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.error
        )
        Text("${request.length}", modifier = Modifier.width(48.dp), fontSize = 13.sp)
        Text(request.mimeType, modifier = Modifier.width(48.dp), fontSize = 13.sp)
    }
}
