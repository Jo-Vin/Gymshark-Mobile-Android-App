package com.jovinyap.productchallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSizeOptionsTest {

    @Test
    // GS-MOB-013: the baseline range is visible even when payload sizes are missing.
    fun baselineSizesArePresentAndPayloadWhitespaceMatches() {
        val options = productSizeOptions(
            listOf(ProductVariant("small", "  s ", 4500L, true))
        )

        assertEquals(listOf("XS", "S", "M", "L", "XL", "XXL"), options.map { it.label })
        assertNull(options.first().variant)
        assertEquals("small", options[1].variant?.id)
    }

    @Test
    // GS-MOB-013: additional payload sizes are retained after the baseline range.
    fun additionalSizesFollowBaselineInPayloadOrder() {
        val options = productSizeOptions(
            listOf(
                ProductVariant("three-xl", " 3xl ", 5000L, true),
                ProductVariant("two-xl", "xxl", 4500L, true),
                ProductVariant("one-size", "One Size", 3000L, true)
            )
        )

        assertEquals(listOf("XS", "S", "M", "L", "XL", "XXL", "3XL", "ONE SIZE"), options.map { it.label })
        assertEquals("three-xl", options[6].variant?.id)
        assertEquals("one-size", options[7].variant?.id)
    }

    @Test
    // GS-MOB-013: only an explicit true stock value enables selection.
    fun missingAndFalseStockValuesAreUnavailable() {
        val options = productSizeOptions(
            listOf(
                ProductVariant("small", "s", null, null),
                ProductVariant("medium", "m", 4500L, false),
                ProductVariant("large", "l", 4500L, true)
            )
        )

        assertFalse(options[1].variant?.inStock == true)
        assertFalse(options[2].variant?.inStock == true)
        assertTrue(options[3].variant?.inStock == true)
    }
}
