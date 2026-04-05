package com.planora.app.utils

/** Strip HTML tags for plain-text preview display. */
private fun stripHtml(html: String): String =
    html.replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"")

/**
 * Strip legacy Markdown syntax for plain-text display.
 * Also used by RichTextState for backward-compat loading â€” kept internal to avoid duplication.
 */
internal fun stripMarkdown(text: String): String =
    text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("~~(.+?)~~"), "$1")
        .replace(Regex("^#{1,3}\\s", RegexOption.MULTILINE), "")
        .replace(Regex("^[-*]\\s", RegexOption.MULTILINE), "â€¢ ")

/** Handles both HTML (new notes) and legacy Markdown (old notes). */
fun stripContentForPreview(content: String): String =
    if ("<" in content && ">" in content) stripHtml(content) else stripMarkdown(content)
