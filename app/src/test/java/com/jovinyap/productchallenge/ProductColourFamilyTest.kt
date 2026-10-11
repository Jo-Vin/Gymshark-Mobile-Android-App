package com.jovinyap.productchallenge

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductColourFamilyTest {

    @Test
    // Product-detail enhancement: conservative colour grouping uses catalogue attributes, not handles.
    fun productsWithMatchingFamilyAttributesAreRelatedByColour() {
        val navy = product("navy", "Navy")
        val black = product("black", "Black")

        assertEquals(
            listOf("navy", "black"),
            relatedColourProducts(listOf(navy, black), navy).map { it.id }
        )
    }

    @Test
    // Product-detail enhancement: identical titles with different family attributes stay separate.
    fun identicalTitlesWithDifferentTypesAreNotGrouped() {
        val leggings = product("leggings", "Navy", productType = "Womens Leggings")
        val shorts = product("shorts", "Black", productType = "Womens Shorts")

        assertEquals(listOf("leggings"), relatedColourProducts(listOf(leggings, shorts), leggings).map { it.id })
    }

    @Test
    // Product-detail enhancement: missing relationship evidence does not create guessed alternatives.
    fun productsWithoutATypeAreNotGrouped() {
        val first = product("first", "Navy", productType = null)
        val second = product("second", "Black", productType = null)

        assertEquals(emptyList<Product>(), relatedColourProducts(listOf(first, second), first))
    }

    private fun product(id: String, colour: String, productType: String? = "Womens Leggings") = Product(
        id = id,
        title = "Speed Leggings",
        rawPrice = 1000L,
        colour = colour,
        descriptionHtml = "<p>Speed</p>",
        imageUrl = "https://example.com/$id.jpg",
        imageAlt = null,
        labels = emptyList(),
        productType = productType,
        gender = "Womens"
    )
}
