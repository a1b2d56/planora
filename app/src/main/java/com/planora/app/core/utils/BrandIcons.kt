package com.planora.app.core.utils

import com.planora.app.R

/**
 * Utility to map bank names to their high-fidelity brand icons.
 */
object BrandIcons {
    private val iconMap = mapOf(
        "hdfc" to R.drawable.ic_brand_hdfc_bank,
        "sbi" to R.drawable.ic_brand_sbi,
        "state bank" to R.drawable.ic_brand_sbi,
        "icici" to R.drawable.ic_brand_icici_bank,
        "axis" to R.drawable.ic_brand_axis_bank,
        "kotak" to R.drawable.ic_brand_kotak_mahindra_bank,
        "idfc" to R.drawable.ic_brand_idfc_first_bank,
        "pnb" to R.drawable.ic_brand_punjab_national_bank,
        "punjab national" to R.drawable.ic_brand_punjab_national_bank,
        "bob" to R.drawable.ic_brand_bank_of_baroda,
        "bank of baroda" to R.drawable.ic_brand_bank_of_baroda,
        "canara" to R.drawable.ic_brand_canara_bank,
        "indusind" to R.drawable.ic_brand_indusind_bank,
        "yes bank" to R.drawable.ic_brand_yes_bank,
        "rbl" to R.drawable.ic_brand_rbl_bank,
        "union bank" to R.drawable.ic_brand_union_bank
    )

    fun getIconForBank(name: String): Int? {
        val lowerName = name.lowercase()
        return iconMap.entries.find { lowerName.contains(it.key) }?.value
    }
}
