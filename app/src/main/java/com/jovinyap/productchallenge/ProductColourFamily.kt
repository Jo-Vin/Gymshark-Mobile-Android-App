package com.jovinyap.productchallenge

import java.util.Locale

/**
 * Returns colour alternatives only when the catalogue provides a conservative family key.
 * There is no explicit family ID in the supplied payload, so title alone is intentionally
 * insufficient and products without a type are not grouped.
 */
internal fun relatedColourProducts(products: List<Product>, selectedProduct: Product): List<Product> {
    val selectedKey = selectedProduct.colourFamilyKey() ?: return emptyList()
    return products.filter { product ->
        product.colour != null && product.colourFamilyKey() == selectedKey
    }
}

private fun Product.colourFamilyKey(): String? {
    val title = title.normalisedOrNull() ?: return null
    val type = productType.normalisedOrNull() ?: return null
    return listOf(title, type, gender.normalisedOrEmpty(), fit.normalisedOrEmpty())
        .joinToString("|")
}

private fun String?.normalisedOrNull(): String? = this
    ?.trim()
    ?.replace(Regex("\\s+"), " ")
    ?.takeIf { it.isNotEmpty() }
    ?.uppercase(Locale.ROOT)

private fun String?.normalisedOrEmpty(): String = normalisedOrNull().orEmpty()
