package com.devil.taskzio.ui.components.richtext

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.devil.taskzio.utils.stripMarkdown

/** Rich span types supported by the editor */
enum class SpanType { Bold, Italic, Strikethrough, Heading1, Heading2, BulletList, Quote }

/** A formatting span applied to a range of text */
data class RichSpan(val start: Int, val end: Int, val type: SpanType)

/**
 * Core rich text state engine — rewritten for correctness.
 *
 * Key design: user-toggled styles are STICKY and separate from cursor-derived styles.
 * This prevents bold/italic from being lost on subsequent keystrokes.
 */
class RichTextState {

    var text by mutableStateOf("")
        private set

    private val _spans = mutableStateListOf<RichSpan>()
    // User-toggled styles — sticky until explicitly toggled off.
    // These are NOT cleared on cursor movement or text change.
    private val _userToggledStyles = mutableSetOf<SpanType>()

    // Whether the user has explicitly toggled styles since last cursor move
    private var _hasUserToggle = false

    // Observable version counter — incremented on any style change to trigger recomposition
    private var _styleVersion by mutableIntStateOf(0)

    var selection by mutableStateOf(TextRange(0))
        private set
    fun isStyleActive(type: SpanType): Boolean {
        @Suppress("UNUSED_VARIABLE") val v = _styleVersion  // subscribe for recomposition
        if (_hasUserToggle) return type in _userToggledStyles
        val min = selection.min
        return _spans.any { it.type == type && it.start <= min && it.end >= min && (it.start < min || it.end > min) }
    }

    // ── Text Change Handling ────────────────────────────────────────────

    /** Update from a new TextFieldValue (called on every keystroke) */
    fun onValueChange(newValue: TextFieldValue) {
        val oldText = text
        val newText = newValue.text
        val newSel = newValue.selection

        if (newText != oldText) {
            val delta = newText.length - oldText.length
            val commonPrefix = oldText.commonPrefixWith(newText).length
            val commonSuffix = oldText.reversed().commonPrefixWith(newText.reversed()).length
                .coerceAtMost(oldText.length - commonPrefix)
            val oldEditEnd = oldText.length - commonSuffix

            // Adjust all existing spans for the edit
            val adjusted = mutableListOf<RichSpan>()
            for (span in _spans) {
                val result = adjustSpan(span, commonPrefix, oldEditEnd, delta)
                if (result != null && result.end > result.start) {
                    adjusted.add(result)
                }
            }

            // If inserting (typing) and we have active styles, extend or create spans
            if (delta > 0) {
                val insertEnd = commonPrefix + delta
                val currentActive = if (_hasUserToggle) {
                    _userToggledStyles.toSet()
                } else {
                    getStylesAtPosition(commonPrefix, adjusted).toSet()
                }

                for (style in currentActive) {
                    // Find if there's already a span that covers or touches the insert point
                    val touchingIdx = adjusted.indexOfFirst {
                        it.type == style && it.start <= commonPrefix && it.end >= commonPrefix
                    }
                    if (touchingIdx >= 0) {
                        // Extend the existing span to include the new text
                        val existing = adjusted[touchingIdx]
                        adjusted[touchingIdx] = existing.copy(end = maxOf(existing.end, insertEnd))
                    } else {
                        // Create a new span for the inserted text
                        adjusted.add(RichSpan(commonPrefix, insertEnd, style))
                    }
                }
            }

            _spans.clear()
            _spans.addAll(mergeSpans(adjusted))
            text = newText
        }

        // Only reset user toggle state on cursor movement (not typing)
        val cursorMoved = newText == oldText && newSel != selection
        if (cursorMoved) {
            _hasUserToggle = false
            _userToggledStyles.clear()
            _styleVersion++
        }

        selection = newSel
    }

    // ── Style Toggling ──────────────────────────────────────────────────

    /** Toggle a style on the current selection or at cursor */
    fun toggleStyle(type: SpanType) {
        if (selection.collapsed) {
            // Cursor mode: toggle sticky user style for future typing
            _hasUserToggle = true
            if (type in _userToggledStyles) {
                _userToggledStyles.remove(type)
            } else {
                _userToggledStyles.add(type)
            }
            _styleVersion++  // trigger recomposition
        } else {
            // Selection mode: apply/remove formatting on selected range
            val start = selection.min
            val end = selection.max

            val coveringSpan = _spans.find { it.type == type && it.start <= start && it.end >= end }
            if (coveringSpan != null) {
                // Remove style from selection
                _spans.remove(coveringSpan)
                if (coveringSpan.start < start) _spans.add(RichSpan(coveringSpan.start, start, type))
                if (coveringSpan.end > end) _spans.add(RichSpan(end, coveringSpan.end, type))
            } else {
                // Remove any partial overlaps
                val overlapping = _spans.filter { it.type == type && it.start < end && it.end > start }
                _spans.removeAll(overlapping.toSet())
                // Add new span covering entire selection
                _spans.add(RichSpan(start, end, type))
            }
            val merged = mergeSpans(_spans.toList())
            _spans.clear()
            _spans.addAll(merged)
        }
    }

    // ── Rendering ───────────────────────────────────────────────────────

    /** Build AnnotatedString for rendering */
    fun toAnnotatedString(baseColor: Color): AnnotatedString = buildAnnotatedString {
        append(text)
        for (span in _spans) {
            val s = span.start.coerceIn(0, text.length)
            val e = span.end.coerceIn(0, text.length)
            if (s >= e) continue
            addStyle(spanTypeToStyle(span.type, baseColor), s, e)
        }
    }

    /** Get a TextFieldValue for the editor */
    fun toTextFieldValue(baseColor: Color): TextFieldValue = TextFieldValue(
        annotatedString = toAnnotatedString(baseColor),
        selection = selection
    )

    // ── HTML Serialization ──────────────────────────────────────────────

    /** Serialize to HTML for database storage */
    fun toHtml(): String {
        if (text.isEmpty()) return ""

        data class TagEvent(val pos: Int, val isOpen: Boolean, val tag: String, val priority: Int)
        val events = mutableListOf<TagEvent>()

        for (span in _spans) {
            val s = span.start.coerceIn(0, text.length)
            val e = span.end.coerceIn(0, text.length)
            if (s >= e) continue
            val tag = spanTypeToHtmlTag(span.type)
            events.add(TagEvent(s, true, tag, span.type.ordinal))
            events.add(TagEvent(e, false, tag, span.type.ordinal))
        }

        events.sortWith(compareBy(
            { it.pos },
            { if (it.isOpen) 1 else 0 },
            { if (it.isOpen) it.priority else -it.priority }
        ))

        val sb = StringBuilder()
        var lastPos = 0
        for (event in events) {
            if (event.pos > lastPos) sb.append(escapeHtml(text.substring(lastPos, event.pos)))
            lastPos = event.pos
            sb.append(if (event.isOpen) "<${event.tag}>" else "</${event.tag}>")
        }
        if (lastPos < text.length) sb.append(escapeHtml(text.substring(lastPos)))

        return sb.toString().replace("\n", "<br>")
    }

    /** Load from HTML (or plain text for backward compat) */
    fun fromHtml(html: String) {
        if (html.isBlank()) {
            text = ""; _spans.clear(); selection = TextRange(0); return
        }

        // Plain text backward compat
        if (!html.contains("<") || !html.contains(">")) {
            text = stripMarkdown(html)
            _spans.clear(); selection = TextRange(text.length); return
        }

        val plainText = StringBuilder()
        val spans = mutableListOf<RichSpan>()
        val tagStack = mutableListOf<Pair<String, Int>>()

        val src = html.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
        var i = 0
        while (i < src.length) {
            if (src[i] == '<') {
                val close = src.indexOf('>', i)
                if (close == -1) { plainText.append(src[i]); i++; continue }
                val tag = src.substring(i + 1, close).trim()
                if (tag.startsWith("/")) {
                    val name = tag.substring(1).trim().lowercase()
                    val openIdx = tagStack.indexOfLast { it.first == name }
                    if (openIdx >= 0) {
                        val (_, startPos) = tagStack.removeAt(openIdx)
                        val spanType = htmlTagToSpanType(name)
                        if (spanType != null && plainText.length > startPos) {
                            spans.add(RichSpan(startPos, plainText.length, spanType))
                        }
                    }
                } else {
                    tagStack.add(tag.lowercase().split(" ").first() to plainText.length)
                }
                i = close + 1
            } else if (src.startsWith("&amp;", i)) { plainText.append('&'); i += 5 }
            else if (src.startsWith("&lt;", i)) { plainText.append('<'); i += 4 }
            else if (src.startsWith("&gt;", i)) { plainText.append('>'); i += 4 }
            else if (src.startsWith("&quot;", i)) { plainText.append('"'); i += 6 }
            else { plainText.append(src[i]); i++ }
        }

        text = plainText.toString()
        _spans.clear()
        _spans.addAll(mergeSpans(spans))
        selection = TextRange(text.length)
        _hasUserToggle = false
        _userToggledStyles.clear()
    }



    // ── Internal Helpers ────────────────────────────────────────────────

    private fun getStylesAtPosition(pos: Int, spanList: List<RichSpan> = _spans): List<SpanType> {
        return spanList.filter { it.start < pos && it.end >= pos || it.start <= pos && it.end > pos }
            .map { it.type }
            .distinct()
    }

    private fun adjustSpan(span: RichSpan, editStart: Int, editEnd: Int, delta: Int): RichSpan? {
        var s = span.start
        var e = span.end

        when {
            editEnd <= s -> {
                // Edit entirely before span — shift both
                s += delta; e += delta
            }
            editStart >= e -> {
                // Edit entirely after span — no change
            }
            editStart <= s && editEnd >= e -> {
                // Edit engulfs span — remove
                return null
            }
            editStart >= s && editEnd <= e -> {
                // Edit within span — grow/shrink end
                e += delta
            }
            editStart < s -> {
                // Edit overlaps start
                s = editStart + maxOf(delta, 0)
                e += delta
            }
            else -> {
                // Edit overlaps end
                e = editStart
            }
        }

        return RichSpan(s.coerceAtLeast(0), e.coerceAtLeast(0), span.type)
    }

    private fun mergeSpans(input: List<RichSpan>): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        for ((type, spans) in input.groupBy { it.type }) {
            val sorted = spans.filter { it.end > it.start }.sortedBy { it.start }
            if (sorted.isEmpty()) continue
            var cur = sorted.first()
            for (next in sorted.drop(1)) {
                if (next.start <= cur.end) {
                    cur = RichSpan(cur.start, maxOf(cur.end, next.end), type)
                } else {
                    result.add(cur); cur = next
                }
            }
            result.add(cur)
        }
        return result
    }

    private fun spanTypeToStyle(type: SpanType, baseColor: Color): SpanStyle = when (type) {
        SpanType.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
        SpanType.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
        SpanType.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough, color = baseColor.copy(alpha = 0.6f))
        SpanType.Heading1 -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        SpanType.Heading2 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        SpanType.BulletList -> SpanStyle()
        SpanType.Quote -> SpanStyle(fontStyle = FontStyle.Italic, color = baseColor.copy(alpha = 0.7f))
    }

    private fun spanTypeToHtmlTag(type: SpanType): String = when (type) {
        SpanType.Bold -> "b"
        SpanType.Italic -> "i"
        SpanType.Strikethrough -> "s"
        SpanType.Heading1 -> "h1"
        SpanType.Heading2 -> "h2"
        SpanType.BulletList -> "li"
        SpanType.Quote -> "blockquote"
    }

    private fun htmlTagToSpanType(tag: String): SpanType? = when (tag) {
        "b", "strong" -> SpanType.Bold
        "i", "em" -> SpanType.Italic
        "s", "del", "strike" -> SpanType.Strikethrough
        "h1" -> SpanType.Heading1
        "h2" -> SpanType.Heading2
        "li" -> SpanType.BulletList
        "blockquote" -> SpanType.Quote
        else -> null
    }

    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
