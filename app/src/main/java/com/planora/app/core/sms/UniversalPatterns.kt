package com.planora.app.core.sms


/**
 * Universal patterns for global banking SMS — currency detection, amount parsing, merchant extraction.
 */
object UniversalPatterns {

    // Each entry: Regex to match → currency symbol for the app
    private val CURRENCY_AMOUNT_PATTERNS = listOf(
        // India
        Regex("""(?i)Rs\.?\s*([0-9,]+(?:\.\d{1,2})?)""")   to "₹",
        Regex("""(?i)INR\s*([0-9,]+(?:\.\d{1,2})?)""")     to "₹",
        Regex("""₹\s*([0-9,]+(?:\.\d{1,2})?)""")            to "₹",
        // US / Generic Dollar
        Regex("""(?i)USD\s*([0-9,]+(?:\.\d{1,2})?)""")     to "$",
        Regex("""\$\s*([0-9,]+(?:\.\d{1,2})?)""")          to "$",
        // Europe
        Regex("""(?i)EUR\s*([0-9,]+(?:\.\d{1,2})?)""")     to "€",
        Regex("""€\s*([0-9,]+(?:\.\d{1,2})?)""")            to "€",
        // UK
        Regex("""(?i)GBP\s*([0-9,]+(?:\.\d{1,2})?)""")     to "£",
        Regex("""£\s*([0-9,]+(?:\.\d{1,2})?)""")            to "£",
        // UAE
        Regex("""(?i)AED\s*([0-9,]+(?:\.\d{1,2})?)""")     to "د.إ",
        Regex("""(?i)Dhs\.?\s*([0-9,]+(?:\.\d{1,2})?)""")  to "د.إ",
        // Saudi Arabia
        Regex("""(?i)SAR\s*([0-9,]+(?:\.\d{1,2})?)""")     to "﷼",
        // Singapore
        Regex("""(?i)SGD\s*([0-9,]+(?:\.\d{1,2})?)""")     to "S$",
        Regex("""S\$\s*([0-9,]+(?:\.\d{1,2})?)""")         to "S$",
        // Malaysia
        Regex("""(?i)MYR\s*([0-9,]+(?:\.\d{1,2})?)""")     to "RM",
        Regex("""(?i)RM\s*([0-9,]+(?:\.\d{1,2})?)""")      to "RM",
        // Philippines
        Regex("""(?i)PHP\s*([0-9,]+(?:\.\d{1,2})?)""")     to "₱",
        Regex("""₱\s*([0-9,]+(?:\.\d{1,2})?)""")            to "₱",
        // Bangladesh
        Regex("""(?i)BDT\s*([0-9,]+(?:\.\d{1,2})?)""")     to "৳",
        Regex("""(?i)Tk\.?\s*([0-9,]+(?:\.\d{1,2})?)""")   to "৳",
        // Pakistan
        Regex("""(?i)PKR\s*([0-9,]+(?:\.\d{1,2})?)""")     to "Rs",
        // Japan
        Regex("""(?i)JPY\s*([0-9,]+)""")                    to "¥",
        Regex("""¥\s*([0-9,]+)""")                           to "¥",
        // South Korea
        Regex("""(?i)KRW\s*([0-9,]+)""")                    to "₩",
        Regex("""₩\s*([0-9,]+)""")                           to "₩",
        // Thailand
        Regex("""(?i)THB\s*([0-9,]+(?:\.\d{1,2})?)""")     to "฿",
        Regex("""฿\s*([0-9,]+(?:\.\d{1,2})?)""")            to "฿",
        // Indonesia
        Regex("""(?i)IDR\s*([0-9,]+)""")                    to "Rp",
        Regex("""(?i)Rp\.?\s*([0-9,.]+)""")                 to "Rp",
        // Nigeria
        Regex("""(?i)NGN\s*([0-9,]+(?:\.\d{1,2})?)""")     to "₦",
        Regex("""₦\s*([0-9,]+(?:\.\d{1,2})?)""")            to "₦",
        // Kenya
        Regex("""(?i)KES\s*([0-9,]+(?:\.\d{1,2})?)""")     to "KSh",
        Regex("""(?i)KSh\.?\s*([0-9,]+(?:\.\d{1,2})?)""")  to "KSh",
        // South Africa
        Regex("""(?i)ZAR\s*([0-9,]+(?:\.\d{1,2})?)""")     to "R",
        // Brazil
        Regex("""(?i)BRL\s*([0-9,]+(?:\.\d{1,2})?)""")     to "R$",
        Regex("""R\$\s*([0-9,]+(?:\.\d{1,2})?)""")         to "R$",
        // Canada
        Regex("""(?i)CAD\s*([0-9,]+(?:\.\d{1,2})?)""")     to "C$",
        Regex("""C\$\s*([0-9,]+(?:\.\d{1,2})?)""")         to "C$",
        // Australia
        Regex("""(?i)AUD\s*([0-9,]+(?:\.\d{1,2})?)""")     to "A$",
        Regex("""A\$\s*([0-9,]+(?:\.\d{1,2})?)""")         to "A$",
        // Turkey
        Regex("""(?i)TRY\s*([0-9,]+(?:\.\d{1,2})?)""")     to "₺",
        Regex("""₺\s*([0-9,]+(?:\.\d{1,2})?)""")            to "₺",
        // China
        Regex("""(?i)CNY\s*([0-9,]+(?:\.\d{1,2})?)""")     to "¥",
        // Russia
        Regex("""(?i)RUB\s*([0-9,]+(?:\.\d{1,2})?)""")     to "₽",
        Regex("""₽\s*([0-9,]+(?:\.\d{1,2})?)""")            to "₽"
    )

    val BALANCE_PATTERNS = listOf(
        Regex("""(?i)(?:Bal|Balance|Avl Bal|Available Balance)[:\s]+(?:[A-Z₹$€£¥₩₦₱₺₽৳฿]{0,4}\.?\s*)?([0-9,]+(?:\.\d{1,2})?)"""),
        Regex("""(?i)(?:Updated Balance|Remaining Balance)[:\s]+(?:[A-Z₹$€£¥₩₦₱₺₽৳฿]{0,4}\.?\s*)?([0-9,]+(?:\.\d{1,2})?)""")
    )

    val MERCHANT_PATTERNS = listOf(
        Regex("""(?i)to\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|\s+UPI|$)"""),
        Regex("""(?i)from\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|\s+UPI|$)"""),
        Regex("""(?i)at\s+([^.\n]+?)(?:\s+on|\s+Ref|$)"""),
        Regex("""(?i)for\s+([^.\n]+?)(?:\s+on|\s+at|\s+Ref|$)""")
    )
    
    val CLEANING_PATTERNS = listOf(
        Regex("""\s*\(.*?\)\s*$"""),
        Regex("""(?i)\s+Ref\s+No.*"""),
        Regex("""\s+on\s+\d{2}.*"""),
        Regex("""(?i)\s+UPI.*"""),
        Regex("""\s+at\s+\d{2}:\d{2}.*"""),
        Regex("""\s*-\s*$"""),
        Regex("""(?i)(\s+PVT\.?\s*LTD\.?|\s+PRIVATE\s+LIMITED)$"""),
        Regex("""(?i)(\s+LTD\.?|\s+LIMITED)$""")
    )
    
    val EXPENSE_KEYWORDS = listOf("debited", "spent", "paid", "withdrawn", "transfer out", "payment of", "deducted", "purchase", "charged")
    val INCOME_KEYWORDS  = listOf("credited", "received", "deposited", "transfer in", "refunded", "reversal", "cashback")

    fun isExpense(body: String): Boolean {
        val lower = body.lowercase()
        return EXPENSE_KEYWORDS.any { lower.contains(it) }
    }

    fun isIncome(body: String): Boolean {
        val lower = body.lowercase()
        return INCOME_KEYWORDS.any { lower.contains(it) }
    }

    /** Extracts the transaction amount from the SMS body. */
    fun extractAmount(body: String): Double? {
        for ((pattern, _) in CURRENCY_AMOUNT_PATTERNS) {
            pattern.find(body)?.let { match ->
                return match.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }
        val genericFallback = Regex("""([0-9,]+\.\d{2})""")
        return genericFallback.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    /** Detects the currency symbol from the SMS body (e.g. "₹", "$", "€"). Returns null if unknown. */
    fun detectCurrency(body: String): String? {
        for ((pattern, symbol) in CURRENCY_AMOUNT_PATTERNS) {
            if (pattern.containsMatchIn(body)) return symbol
        }
        return null
    }

    fun extractBalance(body: String): Double? {
        for (pattern in BALANCE_PATTERNS) {
            pattern.find(body)?.let { match ->
                return match.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }
        return null
    }

    fun extractMerchant(body: String): String {
        var rawMerchant = "Unknown"
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                rawMerchant = match.groupValues[1].trim()
                break
            }
        }
        
        var cleanedMerchant = rawMerchant
        for (pattern in CLEANING_PATTERNS) {
            cleanedMerchant = cleanedMerchant.replace(pattern, "")
        }
        
        return cleanedMerchant.trim()
    }
}

@Suppress("SpellCheckingInspection")
object BankDetector {
    private val bankMap = mapOf(
        // India
        "HDFC" to "HDFC Bank",
        "SBI"  to "State Bank of India",
        "ICICI" to "ICICI Bank",
        "AXIS"  to "Axis Bank",
        "KOTAK" to "Kotak Mahindra Bank",
        "PNB"   to "Punjab National Bank",
        "BOB"   to "Bank of Baroda",
        "CANARA" to "Canara Bank",
        "IDFC"  to "IDFC First Bank",
        "YES"   to "Yes Bank",
        "INDUS" to "IndusInd Bank",
        "UNION" to "Union Bank of India",
        "RBL"   to "RBL Bank",
        "PAYTM" to "Paytm Payments Bank",
        "JIOPY" to "Jio Payments Bank",
        "AIRTEL" to "Airtel Payments Bank",
        // Global
        "HSBC"  to "HSBC",
        "DBS"   to "DBS Bank",
        "CITI"  to "Citibank",
        "CHASE" to "Chase",
        "AMEX"  to "American Express",
        "BARCLAY" to "Barclays",
        "LLOYDS" to "Lloyds",
        "NATWEST" to "NatWest",
        "STANDARD" to "Standard Chartered",
        "MAYBANK" to "Maybank",
        "OCBC"  to "OCBC Bank",
        "UOB"   to "UOB",
        "BDO"   to "BDO",
        "BPI"   to "BPI",
        "BRAC"  to "BRAC Bank",
        "HABIB" to "HBL",
        "MCB"   to "MCB Bank",
        "ALRAJHI" to "Al Rajhi Bank",
        "ENBD"  to "Emirates NBD",
        "FAB"   to "First Abu Dhabi Bank",
        "ANZ"   to "ANZ Bank",
        "COMBANK" to "Commonwealth Bank",
        "WESTPAC" to "Westpac",
        "NAB"   to "NAB",
        "SCOTIABANK" to "Scotiabank",
        "RBC"   to "RBC",
        "TD"    to "TD Bank"
    )

    fun detectBank(sender: String): String {
        val upper = sender.uppercase()
        for ((key, name) in bankMap) {
            if (upper.contains(key)) return name
        }
        return "Bank"
    }
}
