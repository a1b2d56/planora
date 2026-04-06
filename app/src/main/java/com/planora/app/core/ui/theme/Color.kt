package com.planora.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// Semantic colors for income/expense indicators
val PlanoraGreen = Color(0xFF4ADE80)
val IncomeGreen  = Color(0xFF22C55E)
val ExpenseRed   = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

// Priority badge colors  --  aliases of semantic colors, no duplication
val PriorityHigh   = ExpenseRed
val PriorityMedium = WarningAmber
val PriorityLow    = IncomeGreen

// Chart palette  --  muted tones that look great on AMOLED dark backgrounds
val chartColors = listOf(
    Color(0xFFBDA67A),  // tan
    Color(0xFF7BA7BC),  // steel blue
    Color(0xFF9B8EC4),  // soft purple
    Color(0xFF7DB87D),  // sage green
    Color(0xFFBC7B8B),  // dusty rose
    Color(0xFFBC9B6A),  // warm amber
    Color(0xFF7BC4B8),  // teal
    Color(0xFFB87BAA),  // mauve
)
