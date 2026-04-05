package com.planora.app.ui.components

import androidx.annotation.DrawableRes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.planora.app.R
import com.planora.app.data.database.entities.Priority
import com.planora.app.theme.ExpenseRed
import com.planora.app.theme.IncomeGreen
import com.planora.app.theme.PriorityHigh
import com.planora.app.theme.PriorityLow
import com.planora.app.theme.PriorityMedium
import com.planora.app.theme.PlanoraGreen
import com.planora.app.utils.DateUtils

// Spacing and Layout tokens
val SpacingSmall  = 8.dp
val SpacingMedium = 16.dp
val SpacingLarge  = 24.dp

// Standardised Shape tokens
val PillShape   = CircleShape
val CurvedShape = RoundedCornerShape(14.dp)
val CardShape   = RoundedCornerShape(20.dp)

// Interactive Components

/**
 * FAB with subtle scale animation on press.
 * Drop-in replacement for FloatingActionButton with a Google-style micro-interaction.
 */
@Composable
fun PlanoraFAB(
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

// Navigation & Top Bars

@Composable
fun PlanoraTopBar(
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

// Section Headers

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

// Layout Containers & Cards

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientStart: Color = PlanoraGreen,
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
fun PlanoraCard(
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

// Swipe Actions

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

// Stat Card

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    PlanoraCard(modifier = modifier, containerColor = containerColor) {
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

// Progress Indicators

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

// Chips & Selection

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
        shape = CircleShape,
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

// Empty States

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


// Search Input

/**
 * Shared search field used by TasksScreen (inline) and NotesScreen.
 * Standardised shape, colors, and clear button in one place.
 */
@Composable
fun PlanoraSearchBar(
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


// Modular Screen Foundation

/**
 * Standardized layout for editor screens (Add/Edit).
 * Handles the top bar, padding, scrolling, and primary action button.
 */
@Composable
fun PlanoraScreen(
    title: String,
    onBack: () -> Unit,
    actionButtonLabel: String,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailTopBar(title, onBack) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            content()

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onActionButtonClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(actionButtonLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Row-based Action Elements

/**
 * Standard height (52dp) row for switches, info, or other actions.
 */
@Composable
fun PlanoraActionRow(
    label: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    description: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape: Shape = CardShape,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) icon()
                Column {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    if (description != null) {
                        Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingContent()
            }
        }
    }
}

// Date Picker Field

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanoraDatePickerField(
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit,
    label: String = "Date",
    placeholder: String = "Select date",
    showClearButton: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PlanoraActionRow(
        label = label,
        description = if (selectedDate != null) DateUtils.formatDate(selectedDate) else placeholder,
        icon = { Icon(painterResource(R.drawable.ic_date_range), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
        onClick = { showDatePicker = true }
    ) {
        if (showClearButton && selectedDate != null) {
            IconButton(onClick = { onDateSelected(null) }, modifier = Modifier.size(24.dp)) {
                Icon(painterResource(R.drawable.ic_close), "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }

    if (showDatePicker) {
        ModalBottomSheet(
            onDismissRequest = { showDatePicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    TextButton(onClick = { 
                        onDateSelected(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) { Text("Confirm") }
                }
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    title = null,
                    headline = null,
                    colors = DatePickerDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

// Toggle & Selection Groups

@Composable
fun <T> PlanoraToggleGroup(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selectedOption
            Surface(
                modifier = Modifier.weight(1f).height(52.dp).clickable { onOptionSelected(value) },
                shape = CurvedShape,
                color = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, if (isSelected) activeColor else Color.Transparent)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, 
                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// Horizontal Chip Group

@Composable
fun PlanoraChipSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(options) { label ->
                CategoryChip(
                    label = label,
                    selected = label == selectedOption,
                    onClick = { onOptionSelected(label) }
                )
            }
        }
    }

// Text Inputs

@Composable
fun PlanoraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine, maxLines = maxLines,
        leadingIcon = leadingIcon, 
        trailingIcon = trailingIcon ?: if (value.isNotBlank() && singleLine) {
            { IconButton(onClick = { onValueChange("") }) { Icon(painterResource(R.drawable.ic_close), "Clear", modifier = Modifier.size(18.dp)) } }
        } else null,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
