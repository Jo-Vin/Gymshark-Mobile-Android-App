package com.jovinyap.productchallenge

/**
 * Data ready for presentation after mapper validation.
 * rawPrice preserves the supplied number; currency and price units are not inferred here.
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
    val labels: List<String>
)