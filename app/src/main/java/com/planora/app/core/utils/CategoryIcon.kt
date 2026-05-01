package com.planora.app.core.utils

import androidx.annotation.DrawableRes
import com.planora.app.R

object CategoryIcon {
    @DrawableRes
    fun forCategory(category: String): Int = when (category.lowercase()) {
        "food"           -> R.drawable.ic_cat_food
        "transport"      -> R.drawable.ic_cat_transport
        "shopping"       -> R.drawable.ic_cat_shopping
        "entertainment"  -> R.drawable.ic_cat_entertainment
        "bills"          -> R.drawable.ic_cat_bills
        "health"         -> R.drawable.ic_cat_health
        "education"      -> R.drawable.ic_cat_education
        "subscription"   -> R.drawable.ic_cat_subscription
        "salary"         -> R.drawable.ic_cat_salary
        "freelance"      -> R.drawable.ic_cat_freelance
        "investment"     -> R.drawable.ic_cat_investment
        else             -> R.drawable.ic_cat_other
    }
}
