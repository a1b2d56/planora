package com.planora.app.feature.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.data.database.entities.CalendarEvent
import com.planora.app.core.data.database.entities.EventType
import com.planora.app.core.data.database.entities.Task
import com.planora.app.core.data.database.entities.displayName
import com.planora.app.core.ui.theme.*
import com.planora.app.core.ui.components.*
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.FormatUtils

// Pre-computed transparent white variants used throughout this screen


@Composable
fun DashboardScreen(
    onNavigateToMoney: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        HeroCard(
            userName       = state.userName,
            activeTasks    = state.activeTasks,
            doneTasks      = state.completedTasks,
            balance        = state.balance,
            symbol         = state.currencySymbol,
            onTasksClick   = onNavigateToTasks,
            onBalanceClick = onNavigateToMoney
        )

        Spacer(Modifier.height(10.dp))

        SectionHeader("Coming Up", compact = true)
        if (state.nextTask != null || state.todayEvents.isNotEmpty()) {
            state.nextTask?.let { NextTaskCard(it, onClick = onNavigateToTasks) }
            state.todayEvents.forEach { TodayEventCard(it) }
        } else {
            EmptySlot()
        }

        if (state.categorySpending.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader("This Month's Spending", compact = true)
            SpendingChartCard(
                categorySpending = state.categorySpending,
                totalExpense     = state.totalMonthExpense,
                currencySymbol   = state.currencySymbol,
                onClick          = onNavigateToMoney
            )
        }

        if (state.totalTarget > 0) {
            Spacer(Modifier.height(12.dp))
            SectionHeader("Savings", compact = true)
            SavingsRingCard(
                progress = state.savingsProgress,
                saved    = state.totalSaved,
                target   = state.totalTarget,
                symbol   = state.currencySymbol,
                onClick  = onNavigateToSavings
            )
        }

        Spacer(Modifier.height(140.dp + navBarPadding))
    }
}

@Composable
private fun HeroCard(
    userName: String,
    activeTasks: Int,
    doneTasks: Int,
    balance: Double,
    symbol: String,
    onTasksClick: () -> Unit,
    onBalanceClick: () -> Unit
) {
    // Temporal Intelligence: Greeting adapts to the user's name and the current time of day
    val hour     = remember { java.time.LocalTime.now().hour }
    val greeting = remember(userName, hour) {
        buildString {
            append(when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" })
            if (userName.isNotBlank()) append(", $userName")
        }
    }
    val dateStr = remember { DateUtils.formatDayOfWeek(System.currentTimeMillis()) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val gradientBase = MaterialTheme.colorScheme.surfaceContainerLow
    val gradientEnd  = remember(primaryColor) { primaryColor.copy(alpha = 0.15f) }
    val heroBrush    = remember(gradientBase, gradientEnd) {
        Brush.linearGradient(colorStops = arrayOf(
            0f to gradientBase, 0.6f to gradientBase.copy(alpha = 0.9f), 1f to gradientEnd
        ))
    }

    Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 16.dp)
            .clip(CardShape)
            .background(heroBrush)
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(greeting, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(dateStr, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroStat(Modifier.weight(1f), "Active",  "$activeTasks",
                    R.drawable.ic_radio_button_unchecked, primaryColor, onTasksClick)
                HeroStat(Modifier.weight(1f), "Done",    "$doneTasks",
                    R.drawable.ic_check_circle, IncomeGreen, onTasksClick)
                HeroStat(Modifier.weight(1f), "Balance",
                    FormatUtils.formatCurrency(balance, symbol, compact = true),
                    R.drawable.ic_wallet,
                    if (balance >= 0) IncomeGreen else ExpenseRed, onBalanceClick)
            }
        }
    }
}

@Composable
private fun HeroStat(
    modifier: Modifier,
    label: String,
    value: String,
    @DrawableRes icon: Int,
    accent: Color,
    onClick: () -> Unit
) {
        Box(modifier = modifier.clip(CurvedShape)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(painterResource(icon), null, tint = accent, modifier = Modifier.size(16.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NextTaskCard(task: Task, onClick: () -> Unit) {
    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        .clickable(onClick = onClick),
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CurvedShape)
                .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check_circle), null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Next task", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(task.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (task.dueDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(painterResource(R.drawable.ic_schedule), null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
                        Text(DateUtils.formatDate(task.dueDate), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TodayEventCard(event: CalendarEvent) {
    val (accent, iconRes) = when (event.type) {
        EventType.BIRTHDAY -> WarningAmber to R.drawable.ic_cake
        EventType.REMINDER -> MaterialTheme.colorScheme.primary to R.drawable.ic_alarm
        EventType.HOLIDAY  -> IncomeGreen to R.drawable.ic_beach
        EventType.EVENT    -> MaterialTheme.colorScheme.secondary to R.drawable.ic_calendar_month
    }
    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CurvedShape)
                .background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconRes), null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(event.type.displayName, style = MaterialTheme.typography.labelSmall, color = accent)
                Text(event.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (event.isYearly) {
                    Text("Repeats yearly", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(modifier = Modifier.size(width = 3.dp, height = 28.dp).clip(CircleShape).background(accent))
        }
    }
}

@Composable
private fun SpendingChartCard(
    categorySpending: Map<String, Double>,
    totalExpense: Double,
    currencySymbol: String,
    onClick: () -> Unit
) {
    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        .clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Spending by Category", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Icon(painterResource(R.drawable.ic_chevron_right), null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp))
            }
            SpendingDonutChart(
                categorySpending = categorySpending,
                totalExpense     = totalExpense,
                currencySymbol   = currencySymbol,
                donutSize        = 130.dp
            )
        }
    }
}

@Composable
private fun SavingsRingCard(progress: Float, saved: Double, target: Double, symbol: String, onClick: () -> Unit) {
    val animProg by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = EaseOutCubic), label = "ring"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant

    val ringBrush  = remember { Brush.sweepGradient(listOf(IncomeGreen.copy(alpha = 0.5f), IncomeGreen)) }
    val ringStroke = remember { Stroke(width = 9f, cap = StrokeCap.Round) }
    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(80.dp)) {
                    drawArc(color = track, -210f, 240f, false, style = ringStroke)
                    drawArc(
                        brush = ringBrush,
                        startAngle = -210f, sweepAngle = 240f * animProg,
                        useCenter = false, style = ringStroke
                    )
                }
                Text("${(animProg * 100).toInt()}%", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = IncomeGreen)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Overall Savings", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(FormatUtils.formatCurrency(saved, symbol, compact = true),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("of ${FormatUtils.formatCurrency(target, symbol, compact = true)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedProgressBar(progress = animProg, progressColor = IncomeGreen, height = 5.dp)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EmptySlot() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        .clip(CardShape).background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Nothing scheduled today", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
