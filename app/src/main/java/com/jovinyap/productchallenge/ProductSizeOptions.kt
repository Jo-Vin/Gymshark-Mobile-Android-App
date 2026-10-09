package com.jovinyap.productchallenge

import java.util.Locale

private val baselineProductSizes = listOf("XS", "S", "M", "L", "XL", "XXL")

/** A visible size, optionally backed by one payload variant. */
internal data class ProductSizeOption(
    val label: String,
    val variant: ProductVariant?
)

/**
 * Builds the assessment size range without discarding payload sizes.
 * Baseline sizes are shown first; additional sizes retain their payload order.
 */
internal fun productSizeOptions(variants: List<ProductVariant>): List<ProductSizeOption> {
    val byNormalisedSize = variants
        .filter { it.size.trim().isNotEmpty() }
        .groupBy { normaliseProductSize(it.size) }

    val baseline = baselineProductSizes.map { size ->
        ProductSizeOption(size, byNormalisedSize[size]?.firstOrNull())
    }
    val additional = variants
        .asSequence()
        .filter { it.size.trim().isNotEmpty() }
        .map { variant -> normaliseProductSize(variant.size) to variant }
        .filter { (size, _) -> size !in baselineProductSizes }
        .distinctBy { (size, _) -> size }
        .map { (size, variant) -> ProductSizeOption(size, variant) }
        .toList()

    return baseline + additional
}

internal fun normaliseProductSize(value: String): String = value.trim().uppercase(Locale.ROOT)
