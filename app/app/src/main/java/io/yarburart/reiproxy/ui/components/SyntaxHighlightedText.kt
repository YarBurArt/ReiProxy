package io.yarburart.reiproxy.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

private data class SyntaxSpan(
    val text: String,
    val color: Color,
)
@Composable
fun SyntaxHighlightedText(
    content: String,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(content) { buildAnnotatedForSyntax(content) }

    Text(
        text = annotated,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = modifier.fillMaxWidth(),
    )
}
private fun buildAnnotatedForSyntax(content: String): AnnotatedString {
    val normalized = content.replace(
        "\r\n", "\n"
    ).replace("\r", "\n")
    return buildAnnotatedString {
        val format = detectFormat(normalized)
        val spans = when (format) {
            Format.JSON -> highlightJson(normalized)
            Format.HTTP -> highlightHttp(normalized)
            Format.XML -> highlightXml(normalized)
            Format.PLAIN -> listOf(SyntaxSpan(normalized, Color(0xFFD4D4D4)))
        }
        spans.forEach { span ->
            pushStyle(SpanStyle(color = span.color))
            append(span.text)
            pop()
        }
    }
}

private enum class Format { JSON, HTTP, XML, PLAIN }

private fun detectFormat(content: String): Format {
    val trimmed = content.trim()
    if (trimmed.isEmpty()) return Format.PLAIN

    if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
        (trimmed.startsWith("[") && trimmed.endsWith("]"))
    ) {
        return Format.JSON
    }

    val httpMethods = listOf(
        "GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "TRACE ", "CONNECT ")
    val firstLine = trimmed.lineSequence().firstOrNull() ?: ""
    if (httpMethods.any { firstLine.startsWith(it) } ||
        firstLine.matches(Regex("HTTP/\\d\\.\\d \\d{3} .*"))
    ) {
        return Format.HTTP
    }

    if (trimmed.startsWith("<") && trimmed.contains("</")) {
        return Format.XML
    }

    return Format.PLAIN
}

private data class JsonColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val boolean: Color,
    val null_: Color,
    val brace: Color,
    val colon: Color,
    val comma: Color,
)

private fun highlightJson(content: String): List<SyntaxSpan> {
    val colors = JsonColors(
        key = Color(0xFF9CDCFE),
        string = Color(0xFFCE9178),
        number = Color(0xFFB5CEA8),
        boolean = Color(0xFF569CD6),
        null_ = Color(0xFFC586C0),
        brace = Color(0xFFFFD700),
        colon = Color(0xFFD4D4D4),
        comma = Color(0xFF808080),
    )
    val spans = mutableListOf<SyntaxSpan>()
    var i = 0
    val len = content.length

    while (i < len) {
        val c = content[i]

        if (c.isWhitespace()) {
            val start = i
            while (i < len && content[i].isWhitespace()) i++
            spans.add(SyntaxSpan(content.substring(start, i), Color(0xFF808080)))
            continue
        }

        if (c in "{}[]") {
            spans.add(SyntaxSpan(c.toString(), colors.brace))
            i++
            continue
        }

        if (c == ':') {
            spans.add(SyntaxSpan(":", colors.colon))
            i++
            continue
        }

        if (c == ',') {
            spans.add(SyntaxSpan(",", colors.comma))
            i++
            continue
        }

        if (c == '"') {
            val start = i
            i++
            while (i < len) {
                if (content[i] == '\\' && i + 1 < len) {
                    i += 2
                } else if (content[i] == '"') {
                    i++
                    break
                } else {
                    i++
                }
            }
            val str = content.substring(start, i)
            val rest = content.substring(i).takeWhile { it.isWhitespace() }
            val isKey = content.substring(i + rest.length).startsWith(":")
            spans.add(SyntaxSpan(str, if (isKey) colors.key else colors.string))
            continue
        }

        if (c.isDigit() || c == '-') {
            val start = i
            while (i < len && (content[i].isDigit() || content[i] in ".eE+-")) i++
            spans.add(SyntaxSpan(content.substring(start, i), colors.number))
            continue
        }

        if (content.startsWith("true", i)) {
            spans.add(SyntaxSpan("true", colors.boolean))
            i += 4
            continue
        }
        if (content.startsWith("false", i)) {
            spans.add(SyntaxSpan("false", colors.boolean))
            i += 5
            continue
        }
        if (content.startsWith("null", i)) {
            spans.add(SyntaxSpan("null", colors.null_))
            i += 4
            continue
        }

        spans.add(SyntaxSpan(c.toString(), Color(0xFFD4D4D4)))
        i++
    }

    return spans
}

private fun highlightHttp(content: String): List<SyntaxSpan> {
    val methodColor = Color(0xFF569CD6)
    val urlColor = Color(0xFF9CDCFE)
    val httpVersionColor = Color(0xFFC586C0)
    val statusColor = Color(0xFF4EC9B0)
    val headerNameColor = Color(0xFF9CDCFE)
    val headerValueColor = Color(0xFFCE9178)
    val colonColor = Color(0xFF808080)
    val lineBreakColor = Color(0xFF808080)
    val bodyColor = Color(0xFFD4D4D4)

    val spans = mutableListOf<SyntaxSpan>()
    val lines = content.split("\n")
    var isBody = false
    val httpMethods = listOf(
        "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE", "CONNECT")

    for (line in lines) {
        if (isBody) {
            spans.add(SyntaxSpan(line, bodyColor))
            spans.add(SyntaxSpan("\n", lineBreakColor))
            continue
        }

        if (line.isBlank()) {
            spans.add(SyntaxSpan("\n", lineBreakColor))
            isBody = true
            continue
        }

        val firstPart = line.substringBefore(" ")
        if (httpMethods.contains(firstPart)) {
            val parts = line.split(" ", limit = 3)
            spans.add(SyntaxSpan(parts[0], methodColor))
            if (parts.size > 1) {
                spans.add(SyntaxSpan(" ", lineBreakColor))
                spans.add(SyntaxSpan(parts[1], urlColor))
            }
            if (parts.size > 2) {
                spans.add(SyntaxSpan(" ", lineBreakColor))
                spans.add(SyntaxSpan(parts[2], httpVersionColor))
            }
            spans.add(SyntaxSpan("\n", lineBreakColor))
            continue
        }

        if (line.matches(Regex("HTTP/\\d\\.\\d \\d{3}.*"))) {
            val match = Regex("(HTTP/\\d\\.\\d) (\\d{3}) (.*)").matchEntire(line)
            if (match != null) {
                spans.add(SyntaxSpan(match.groupValues[1], httpVersionColor))
                spans.add(SyntaxSpan(" ", lineBreakColor))
                val code = match.groupValues[2].toInt()
                val codeColor = when {
                    code < 300 -> Color(0xFF4EC9B0)
                    code < 400 -> Color(0xFF569CD6)
                    code < 500 -> Color(0xFFCE9178)
                    else -> Color(0xFFF44747)
                }
                spans.add(SyntaxSpan(match.groupValues[2], codeColor))
                spans.add(SyntaxSpan(" ", lineBreakColor))
                spans.add(SyntaxSpan(match.groupValues[3], bodyColor))
                spans.add(SyntaxSpan("\n", lineBreakColor))
            } else {
                spans.add(SyntaxSpan(line, httpVersionColor))
                spans.add(SyntaxSpan("\n", lineBreakColor))
            }
            continue
        }

        val colonIdx = line.indexOf(':')
        if (colonIdx > 0) {
            spans.add(SyntaxSpan(line.substring(0, colonIdx), headerNameColor))
            spans.add(SyntaxSpan(":", colonColor))
            if (colonIdx + 1 < line.length) {
                spans.add(SyntaxSpan(line.substring(colonIdx + 1), headerValueColor))
            }
            spans.add(SyntaxSpan("\n", lineBreakColor))
            continue
        }

        spans.add(SyntaxSpan(line, bodyColor))
        spans.add(SyntaxSpan("\n", lineBreakColor))
    }

    return spans
}

private fun highlightXml(content: String): List<SyntaxSpan> {
    val tagColor = Color(0xFF569CD6)
    val attrNameColor = Color(0xFF9CDCFE)
    val attrValueColor = Color(0xFFCE9178)
    val bracketColor = Color(0xFFFFD700)
    val textColor = Color(0xFFD4D4D4)
    val wsColor = Color(0xFF808080)

    val spans = mutableListOf<SyntaxSpan>()
    var i = 0
    val len = content.length

    while (i < len) {
        val c = content[i]

        if (c.isWhitespace()) {
            val start = i
            while (i < len && content[i].isWhitespace()) i++
            spans.add(SyntaxSpan(content.substring(start, i), wsColor))
            continue
        }

        if (c == '<') {
            val start = i
            i++
            while (i < len && content[i] != '>') i++
            if (i < len) i++
            val tag = content.substring(start, i)
            spans.add(SyntaxSpan(tag, tagColor))
            continue
        }

        if (c == '"' || c == '\'') {
            val start = i
            i++
            while (i < len && content[i] != c) i++
            if (i < len) i++
            spans.add(SyntaxSpan(content.substring(start, i), attrValueColor))
            continue
        }

        val start = i
        while (i < len && content[i] != '<' && !content[i].isWhitespace()) i++
        if (i > start) {
            spans.add(SyntaxSpan(content.substring(start, i), textColor))
        }
    }

    return spans
}

@Preview
@Composable
private fun PreviewJsonHighlight() {
    Surface {
        SyntaxHighlightedText(
            content = """{
  "username": "admin",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expires_in": 3600,
  "active": true,
  "data": null
}""".trimIndent()
        )
    }
}

@Preview
@Composable
private fun PreviewHttpHighlight() {
    Surface {
        SyntaxHighlightedText(
            content = """POST /v1/auth/login HTTP/1.1
Host: api.example.com
Content-Type: application/json
Authorization: Bearer eyJhbG...

{
  "username": "admin",
  "password": "test123"
}""".trimIndent()
        )
    }
}

@Composable
fun SyntaxHighlightedEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val annotated = remember(value.text) { buildAnnotatedForSyntax(value.text) }

    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = annotated,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            lineHeight = TextUnit(18f, TextUnitType.Sp),
            modifier = Modifier.fillMaxWidth(),
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize(),
            textStyle = LocalTextStyle.current.copy(
                color = Color.Transparent,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                lineHeight = TextUnit(18f, TextUnitType.Sp),
            ),
            cursorBrush = SolidColor(Color(0xFF6650a4)),
            interactionSource = interactionSource,
        )
    }
}
