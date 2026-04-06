package com.planora.app.core.ui.components.richtext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Formatting toolbar with active state indicators.
 * Buttons highlight when the cursor is inside a formatted span.
 */
@Composable
fun FormatToolbar(
    state: RichTextState,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FormatButton("B", SpanType.Bold, state, tintColor)
        FormatButton("I", SpanType.Italic, state, tintColor)
        FormatButton("S", SpanType.Strikethrough, state, tintColor)
        FormatButton("H1", SpanType.Heading1, state, tintColor)
        FormatButton("H2", SpanType.Heading2, state, tintColor)
        FormatButton("\u2022", SpanType.BulletList, state, tintColor)
        FormatButton("\u201C", SpanType.Quote, state, tintColor)
    }
}

@Composable
private fun FormatButton(
    label: String,
    type: SpanType,
    state: RichTextState,
    tintColor: Color
) {
    val isActive  = state.isStyleActive(type)
    val primary   = MaterialTheme.colorScheme.primary
    // Memoize color variants - only recompute when tintColor or primary changes
    val activeBg      = remember(primary)    { primary.copy(alpha = 0.25f) }
    val inactiveBg    = remember(tintColor)  { tintColor.copy(alpha = 0.1f) }
    val inactiveText  = remember(tintColor)  { tintColor.copy(alpha = 0.7f) }

    Surface(
        modifier = Modifier.clickable { state.toggleStyle(type) },
        shape    = RoundedCornerShape(8.dp),
        color    = if (isActive) activeBg else inactiveBg
    ) {
        Text(
            text       = label,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            color      = if (isActive) primary else inactiveText
        )
    }
}
