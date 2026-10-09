package com.jovinyap.productchallenge

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Displays GBP pence as pounds, preserving pennies even for amounts larger than a Double can hold. */
internal fun formatGbpPrice(minorUnits: Long): String {
    require(minorUnits >= 0L) { "A product price must be non-negative" }
    // BigDecimal moves the decimal point exactly; it avoids floating-point rounding.
    val pounds = BigDecimal.valueOf(minorUnits, 2)
    return NumberFormat.getCurrencyInstance(Locale.UK).format(pounds)
}
