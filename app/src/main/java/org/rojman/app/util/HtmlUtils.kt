package org.rojman.app.util

import android.text.Html
import androidx.core.text.HtmlCompat

fun String.htmlToText(): String = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

fun String.cleanHtmlPreview(max: Int = 180): String {
    val plain = htmlToText().replace("\\s+".toRegex(), " ").trim()
    return if (plain.length <= max) plain else plain.take(max).trimEnd() + "…"
}
