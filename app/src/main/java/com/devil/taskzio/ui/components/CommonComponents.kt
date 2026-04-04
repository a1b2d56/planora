package com.devil.taskzio.ui.components

import androidx.annotation.DrawableRes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.devil.taskzio.R
import com.devil.taskzio.data.database.entities.Priority
import com.devil.taskzio.theme.ExpenseRed
import com.devil.taskzio.theme.IncomeGreen
import com.devil.taskzio.theme.PriorityHigh
import com.devil.taskzio.theme.PriorityLow
import com.devil.taskzio.theme.PriorityMedium
import com.devil.taskzio.theme.TaskzioGreen

// ── Spacing system ────────────────────────────────────────────────────────────
val SpacingSmall  = 8.dp
val SpacingMedium = 16.dp
val SpacingLarge  = 24.dp

// ── Animated FAB ──────────────────────────────────────────────────────────────

/**
 * FAB with subtle scale animation on press.
 * Drop-in replacement for FloatingActionButton with a Google-style micro-interaction.
 */
@Composable
fun TaskzioFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "fab_scale"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource
    ) { content() }
}

// ── Top bars ──────────────────────────────────────────────────────────────────

@Composable
fun TaskzioTopBar(
    title: String,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = SpacingMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
    }
}

/** Top bar used by detail/editor screens (back arrow + title). */
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(title, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f))
    }
}

// ── Section headers ───────────────────────────────────────────────────────────

/**
 * Shared section header.
 * [compact] = true → label style + tighter padding (used by Dashboard).
 * [action] + [onAction] = optional trailing text button.
 */
@Composable
fun SectionHeader(
    title: String,
    compact: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = if (compact) 24.dp else 20.dp, vertical = if (compact) 6.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (compact) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Card wrappers ─────────────────────────────────────────────────────────────

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientStart: Color = TaskzioGreen,
    gradientEnd: Color = IncomeGreen,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = remember(gradientStart, gradientEnd) {
        Brush.linearGradient(listOf(gradientStart, gradientEnd))
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = modifier.clip(RoundedCornerShape(20.dp))
                .background(brush)
                .padding(20.dp),
            content = content
        )
    }
}

/**
 * Standard app card with zero elevation and surface color.
 * Eliminates the repeated `CardDefaults.cardElevation(0.dp)` on every Card.
 */
@Composable
fun TaskzioCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val defaultSurface = MaterialTheme.colorScheme.surface
    val useGradient = containerColor == defaultSurface

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (useGradient) Color.Transparent else containerColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        if (useGradient) {
            val gradientStart = MaterialTheme.colorScheme.surfaceContainerLow
            val gradientEnd = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            val brush = Brush.linearGradient(listOf(gradientStart, gradientEnd))
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Box(modifier = Modifier.background(brush)) {
                    Column(content = content)
                }
            }
        } else {
            Column(content = content)
        }
    }
}

// ── Swipe to delete ───────────────────────────────────────────────────────────

/**
 * Wraps content in a right-to-left swipe gesture that triggers [onDelete].
 * Replaces the copy-pasted SwipeToDismissBox block in Tasks, Money, and Calendar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> ExpenseRed
                    else -> Color.Transparent
                }, label = "delete_bg"
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = "Delete",
                        tint = Color.White, modifier = Modifier.padding(end = 20.dp))
                }
            }
        }
    ) { content() }
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    TaskzioCard(modifier = modifier, containerColor = containerColor) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                icon()
            }
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Progress bar ──────────────────────────────────────────────────────────────

@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "progress"
    )
    val brush = remember(progressColor) {
        Brush.horizontalGradient(listOf(progressColor, progressColor.copy(alpha = 0.8f)))
    }
    Box(modifier = modifier.fillMaxWidth().height(height).clip(CircleShape).background(trackColor)) {
        Box(
            modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight()
                .clip(CircleShape)
                .background(brush)
        )
    }
}

// ── Chips ─────────────────────────────────────────────────────────────────────

@Composable
fun PriorityChip(priority: Priority) {
    val (color, label) = when (priority) {
        Priority.LOW    -> PriorityLow    to "Low"
        Priority.MEDIUM -> PriorityMedium to "Medium"
        Priority.HIGH   -> PriorityHigh   to "High"
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null,
            modifier = Modifier.size(72.dp).alpha(0.7f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        action?.invoke()
    }
}


// ── Search bar ────────────────────────────────────────────────────────────────

/**
 * Shared search field used by TasksScreen (inline) and NotesScreen.
 * Standardised shape, colors, and clear button in one place.
 */
@Composable
fun TaskzioSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…"
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), "Clear")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

// ── Text field ────────────────────────────────────────────────────────────────

@Composable
fun TaskzioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine, maxLines = maxLines,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
