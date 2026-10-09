package com.jovinyap.productchallenge

/**
 * Data ready for presentation after mapper validation.
 * rawPrice preserves the supplied integer GBP pence, as confirmed for this assessment.
 * descriptionHtml preserves the original HTML for a future rendering component.
 */
data class Product(
    val id: String,
    val title: String,
    val rawPrice: Long,
    val colour: String?,
    val descriptionHtml: String?,
    val imageUrl: String?,
    val imageAlt: String?,
    val labels: List<String>,
    val fit: String? = null
)