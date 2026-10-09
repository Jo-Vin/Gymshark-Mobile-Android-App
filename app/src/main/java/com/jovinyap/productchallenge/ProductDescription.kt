package com.jovinyap.productchallenge

import android.text.Html
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.os.Build
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

/**
 * Converts the supported product HTML into readable text for tests and fallback rendering.
 * This removes script/style and known translator noise; it is not a general HTML sanitiser.
 */
internal fun readableProductDescription(html: String?): String? {
    val cleaned = html?.replace(removableProductMarkup, "")?.replace(translatorMarkup, "") ?: return null
    val spanned = parseProductHtml(cleaned)
    return spanned.toString().replace("\u00a0", " ").trim().takeIf { it.isNotBlank() }
}

/** Keeps paragraph breaks and basic bold/italic/underline spans supported by Android Html. */
internal fun annotatedProductDescription(html: String?): AnnotatedString? {
    val cleanedText = readableProductDescription(html) ?: return null
    val cleaned = html!!.replace(removableProductMarkup, "").replace(translatorMarkup, "")
    val spanned = parseProductHtml(cleaned)
    return spanned.toAnnotatedString(cleanedText)
}

private fun parseProductHtml(value: String): Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
} else {
    @Suppress("DEPRECATION")
    Html.fromHtml(value)
}

private fun Spanned.toAnnotatedString(normalisedText: String): AnnotatedString {
    val builder = AnnotatedString.Builder(normalisedText)
    getSpans(0, length, Any::class.java).forEach { span ->
        val start = getSpanStart(span).coerceAtLeast(0)
        val end = getSpanEnd(span).coerceAtMost(normalisedText.length)
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
