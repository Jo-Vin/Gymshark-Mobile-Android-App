package com.jovinyap.productchallenge

import android.text.Html
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.os.Build
import java.util.Locale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private val removableProductMarkup = Regex(
    "<(?<tag>script|style)\\b[^>]*>.*?</\\k<tag>\\s*>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val translatorMarkup = Regex(
    "<div\\b[^>]*(?:id|class)\\s*=\\s*['\"][^'\"]*gtx-trans[^'\"]*['\"][^>]*>.*?</div\\s*>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val emptyProductBlock = Regex(
    "<(p|div)\\b[^>]*>\\s*(?:<br\\b[^>]*>\\s*)*</\\1\\s*>",
    RegexOption.IGNORE_CASE
)
private val repeatedProductBreaks = Regex(
    "(?:<br\\b[^>]*>\\s*){2,}",
    RegexOption.IGNORE_CASE
)
private val productMetadataMarkup = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
private val leadingParagraphBreaks = Regex(
    "(<p\\b[^>]*>)\\s*(?:<br\\b[^>]*>\\s*)+",
    RegexOption.IGNORE_CASE
)
private val excessProductSpacing = Regex("\\n(?:[\\t ]*\\n){2,}")
private val modelInformationPattern = Regex(
    "(?i)\\b(?:video\\s+)?model\\s+is\\s+([^\\n]+?)\\s+and\\s+wears\\s+(?:a\\s+)?size\\s+([^\\s,.;]+)"
)

internal data class ModelInformation(val height: String, val size: String)

/**
 * Converts the supported product HTML into readable text for tests and fallback rendering.
 * This removes script/style and known translator noise; it is not a general HTML sanitiser.
 */
internal fun readableProductDescription(html: String?): String? {
    return normalisedProductDescription(html)?.toString()
}

internal fun extractModelInformation(html: String?): ModelInformation? {
    val text = readableProductDescription(html) ?: return null
    val match = modelInformationPattern.find(text) ?: return null
    return ModelInformation(match.groupValues[1].trim(), match.groupValues[2].trim().uppercase(Locale.ROOT))
}

/** Keeps paragraph breaks and basic bold/italic/underline spans supported by Android Html. */
internal fun annotatedProductDescription(html: String?): AnnotatedString? {
    return normalisedProductDescription(html)?.toAnnotatedString()
}

private fun cleanProductMarkup(value: String): String = value
    .replace(removableProductMarkup, "")
    .replace(translatorMarkup, "")
    .replace(productMetadataMarkup, "")
    .replace(emptyProductBlock, "")
    .replace(leadingParagraphBreaks, "$1")
    .replace(repeatedProductBreaks, "<br>")

/** Edit styled text itself so removing spacing also updates bold/italic/underline offsets. */
private fun normalisedProductDescription(html: String?): Spanned? {
    if (html == null) return null
    val text = SpannableStringBuilder(parseProductHtml(cleanProductMarkup(html)))
    for (index in 0 until text.length) {
        if (text[index] == '\u00a0') text.replace(index, index + 1, " ")
    }
    // Preserve one blank line between paragraphs and single line breaks within feature lists.
    // Work backwards so an edit cannot move the positions of a later pending edit.
    excessProductSpacing.findAll(text.toString()).toList().asReversed().forEach { match ->
        text.replace(match.range.first, match.range.last + 1, "\n\n")
    }
    val firstContent = text.indexOfFirst { !it.isWhitespace() }
    if (firstContent == -1) return null
    val contentEnd = text.indexOfLast { !it.isWhitespace() } + 1
    text.delete(contentEnd, text.length)
    text.delete(0, firstContent)
    return text
}

private fun parseProductHtml(value: String): Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
} else {
    @Suppress("DEPRECATION")
    Html.fromHtml(value)
}

private fun Spanned.toAnnotatedString(): AnnotatedString {
    val builder = AnnotatedString.Builder(toString())
    getSpans(0, length, Any::class.java).forEach { span ->
        val start = getSpanStart(span).coerceAtLeast(0)
        val end = getSpanEnd(span).coerceAtMost(length)
        if (start >= end) return@forEach
        when (span) {
            is StyleSpan -> builder.addStyle(
                SpanStyle(
                    fontWeight = if (span.style == android.graphics.Typeface.BOLD) FontWeight.Bold else null,
                    fontStyle = if (span.style == android.graphics.Typeface.ITALIC) FontStyle.Italic else null
                ),
                start,
                end
            )
            is UnderlineSpan -> builder.addStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), start, end)
        }
    }
    return builder.toAnnotatedString()
}
