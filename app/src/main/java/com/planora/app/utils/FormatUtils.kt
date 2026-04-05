package com.planora.app.utils

/** App-wide number formatting helpers. */
object FormatUtils {
    /** Format a double as a compact human-readable number: 1200 â†’ 1.2K, 2500000 â†’ 2.5M. */
    fun formatShort(value: Double): String = when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        value >= 1_000     -> "%.1fK".format(value / 1_000)
        else               -> "%.0f".format(value)
    }

    /** 
     * Formats an amount with a currency symbol. 
     * Handles negative values correctly (e.g. -$30 instead of $-30).
     */
    fun formatCurrency(
        amount: Double,
        symbol: String = "$",
        compact: Boolean = false,
        forcePlus: Boolean = false
    ): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        
        val signStr = when {
            isNegative -> "-"
            forcePlus && amount > 0 -> "+"
            else -> ""
        }
        
        val valueStr = if (compact) {
            formatShort(absAmount)
        } else {
            String.format(java.util.Locale.getDefault(), "%.2f", absAmount)
        }
        
        return "$signStr$symbol$valueStr"
    }
}
