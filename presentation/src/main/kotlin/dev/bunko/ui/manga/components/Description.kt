/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

internal fun parseDescription(
    raw: String,
    linkColor: Color,
    compact: Boolean = false,
): AnnotatedString {
    val links = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    val lines = normalize(raw).lines().let { all -> if (compact) all.filter { it.isNotBlank() } else all }
    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            if (index > 0) append('\n')
            val trimmed = line.trim()
            val heading = HEADING.matchEntire(trimmed)
            val bullet = BULLET.matchEntire(trimmed)
            when {
                heading != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { inline(heading.groupValues[1], links) }
                bullet != null -> {
                    append("•  ")
                    inline(bullet.groupValues[1], links)
                }
                trimmed.startsWith(">") -> inline(trimmed.removePrefix(">").trimStart(), links)
                else -> inline(line.trimEnd(), links)
            }
        }
    }
}

private fun normalize(raw: String): String =
    raw.replace("\r\n", "\n")
        .replace(BREAK, "\n")
        .replace(BLOCK_END, "\n")
        .replace(ANCHOR) { "[${it.groupValues[2].replace(TAG, "")}](${it.groupValues[1]})" }
        .replace(TAG, "")
        .let(::decodeEntities)
        .lines()
        .filterNot { RULE.matches(it.trim()) }
        .joinToString("\n")
        .replace(BLANK_RUN, "\n\n")
        .trim()

private fun decodeEntities(text: String): String =
    ENTITY.replace(text) { match ->
        val name = match.groupValues[1]
        when {
            name.startsWith("#x", ignoreCase = true) -> name.drop(2).toIntOrNull(16)?.let(::codePoint)
            name.startsWith("#") -> name.drop(1).toIntOrNull()?.let(::codePoint)
            else -> NAMED_ENTITIES[name.lowercase()]
        } ?: match.value
    }

private fun codePoint(value: Int): String? = runCatching { String(Character.toChars(value)) }.getOrNull()

private fun AnnotatedString.Builder.inline(
    text: String,
    links: TextLinkStyles,
) {
    var i = 0
    val plain = StringBuilder()
    fun flush() {
        if (plain.isNotEmpty()) {
            linkify(plain.toString(), links)
            plain.clear()
        }
    }
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && i + 1 < text.length && text[i + 1] in ESCAPABLE) {
            plain.append(text[i + 1])
            i += 2
            continue
        }
        val bare = if (c == 'h') BARE_URL.matchAt(text, i) else null
        if (bare != null) {
            plain.append(bare.value)
            i += bare.value.length
            continue
        }
        val link = if (c == '[') MARKDOWN_LINK.matchAt(text, i) else null
        if (link != null) {
            flush()
            val label = link.groupValues[1]
            val url = link.groupValues[2]
            withLink(LinkAnnotation.Url(url, links)) { inline(label.ifEmpty { url }, links) }
            i += link.value.length
            continue
        }
        val marker = MARKERS.firstOrNull { text.startsWith(it.token, i) }
        if (marker != null) {
            val close = text.indexOf(marker.token, i + marker.token.length)
            val inner = if (close > i + marker.token.length) text.substring(i + marker.token.length, close) else null
            if (inner != null && !inner.first().isWhitespace() && !inner.last().isWhitespace()) {
                flush()
                withStyle(marker.style) { inline(inner, links) }
                i = close + marker.token.length
                continue
            }
        }
        plain.append(c)
        i++
    }
    flush()
}

private fun AnnotatedString.Builder.linkify(
    text: String,
    links: TextLinkStyles,
) {
    var last = 0
    BARE_URL.findAll(text).forEach { match ->
        val url = trimUrl(match.value)
        append(text, last, match.range.first)
        withLink(LinkAnnotation.Url(url, links)) { append(url) }
        last = match.range.first + url.length
    }
    append(text, last, text.length)
}

private fun trimUrl(raw: String): String {
    var url = raw
    while (url.isNotEmpty()) {
        val end = url.last()
        url = when {
            end in TRAILING_PUNCTUATION -> url.dropLast(1)
            end == ')' && url.count { it == ')' } > url.count { it == '(' } -> url.dropLast(1)
            else -> return url
        }
    }
    return url
}

private class Marker(val token: String, val style: SpanStyle)

private val MARKERS = listOf(
    Marker("***", SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)),
    Marker("**", SpanStyle(fontWeight = FontWeight.Bold)),
    Marker("__", SpanStyle(fontWeight = FontWeight.Bold)),
    Marker("~~", SpanStyle(textDecoration = TextDecoration.LineThrough)),
    Marker("*", SpanStyle(fontStyle = FontStyle.Italic)),
)

private const val ESCAPABLE = "\\`*_{}[]()#+-.!~>|"
private val HEADING = Regex("#{1,6}\\s+(.+?)\\s*#*")
private val BULLET = Regex("[-*+•]\\s+(.+)")
private val RULE = Regex("([-*_])(\\s*\\1){2,}")
private val MARKDOWN_LINK = Regex("\\[([^\\]]*)]\\((https?://(?:[^()\\s]|\\([^()\\s]*\\))+)\\)")
private val BARE_URL = Regex("https?://[^\\s<>\"]+")
private val BREAK = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val BLOCK_END = Regex("</(p|div|li|h[1-6])>", RegexOption.IGNORE_CASE)
private val ANCHOR = Regex("<a\\s[^<>]*?href\\s*=\\s*[\"'](https?://[^\"']+)[\"'][^<>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private const val TRAILING_PUNCTUATION = ".,;:!?"
private val TAG = Regex("</?[a-zA-Z][^<>]*>")
private val ENTITY = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
private val BLANK_RUN = Regex("\n[ \t]*(\n[ \t]*)+")
private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
    "mdash" to "—",
    "ndash" to "–",
    "hellip" to "…",
    "rsquo" to "’",
    "lsquo" to "‘",
    "rdquo" to "”",
    "ldquo" to "“",
)
