package com.planora.app.ui.components.richtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp

/**
 * Rich text editor composable.
 * Wraps BasicTextField and integrates with RichTextState for real formatting.
 */
@Composable
fun RichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    placeholderColor: Color = textColor.copy(alpha = 0.4f),
    placeholder: String = "Start writing..."
) {
    val value = state.toTextFieldValue(textColor)
    val textStyle = remember(textColor) {
        // lineHeight and color are the only non-default customizations â€” memoize the whole style
        androidx.compose.ui.text.TextStyle(
            color = textColor,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.5.sp,
        )
    }

    BasicTextField(
        value = value,
        onValueChange = { newVal -> state.onValueChange(newVal) },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxSize(),
        decorationBox = { inner ->
            Box {
                if (state.text.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = placeholderColor
                    )
                }
                inner()
            }
        }
    )
}
