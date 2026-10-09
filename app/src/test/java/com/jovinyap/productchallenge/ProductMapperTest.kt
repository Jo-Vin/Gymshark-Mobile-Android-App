package com.jovinyap.productchallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductMapperTest {

    private val mapper = ProductMapper()

    @Test
    fun fitIsNormalisedWithoutInventingMissingValues() {
        assertEquals("Oversized Fit", requireNotNull(mapper.map(validDto().copy(fit = " Oversized Fit "))).fit)
        assertNull(requireNotNull(mapper.map(validDto().copy(fit = " "))).fit)
        assertNull(requireNotNull(mapper.map(validDto())).fit)
    }

    private fun validDto() = ProductDto(
        id = 6732609257571L,
        title = "Speed Leggings",
        price = 65L,
        colour = "Navy"
    )

    @Test
    fun `GS-MOB-003-1 stable identity is preserved without truncation`() {
        val numericIdentity = requireNotNull(
            mapper.map(validDto())
        )

        val objectIdentity = requireNotNull(
            mapper.map(validDto().copy(objectID = "catalogue-42"))
        )

        assertEquals("6732609257571", numericIdentity.id)
        assertEquals("catalogue-42", objectIdentity.id)
    }

    @Test
    fun `GS-MOB-009-1 raw price and product attributes are preserved`() {
        // Also supports GS-MOB-010, GS-MOB-013 and GS-MOB-025.
        // UI presentation remains to be verified.
        val dto = validDto().copy(
            title = " 训练紧身裤 🏋️ ",
            colour = " Navy ",
            description = "<p>Comfort &amp; support</p>",
            labels = listOf("new", "future-label"),
            featuredMedia = ProductMediaDto(
                src = "https://example.com/front.jpg",
                alt = "Product front"
            ),
            availableSizes = listOf(
                ProductVariantDto(size = "s", price = 45L)
            )
        )

        val product = requireNotNull(mapper.map(dto))

        assertEquals("训练紧身裤 🏋️", product.title)
        assertEquals(65L, product.rawPrice)
        assertEquals("Navy", product.colour)
        assertEquals(
            "<p>Comfort &amp; support</p>",
            product.descriptionHtml
        )
        assertEquals(
            listOf("new", "future-label"),
            product.labels
        )
        assertEquals(
            "https://example.com/front.jpg",
            product.imageUrl
        )
        assertEquals("Product front", product.imageAlt)
        assertEquals(
            listOf(ProductVariant("s", "s", 45L, null)),
            product.variants
        )
    }

    @Test
    // GS-MOB-013: variant identity and nullable variant pricing survive mapping.
    fun variantsKeepStableIdentifiersAndNullablePrices() {
        val product = requireNotNull(
            mapper.map(
                validDto().copy(
                    availableSizes = listOf(
                        ProductVariantDto(id = 10L, size = "S", price = 4500L, inStock = true),
                        ProductVariantDto(id = 11L, size = "M", price = null, inStock = false)
                    )
                )
            )
        )

        assertEquals("10", product.variants[0].id)
        assertEquals("S", product.variants[0].size)
        assertEquals(4500L, product.variants[0].rawPrice)
        assertEquals("11", product.variants[1].id)
        assertNull(product.variants[1].rawPrice)
    }

    @Test
    fun `GS-MOB-007-1 missing images do not exclude a usable product`() {
        val product = requireNotNull(
            mapper.map(
                validDto().copy(featuredMedia = null, media = null)
            )
        )

        assertEquals("Speed Leggings", product.title)
        assertNull(product.imageUrl)
        assertNull(product.imageAlt)
    }

    @Test
    fun `GS-MOB-007-2 missing featured URL falls back to gallery image`() {
        val dto = validDto().copy(
            featuredMedia = ProductMediaDto(src = " "),
            media = listOf(
                ProductMediaDto(src = null),
                ProductMediaDto(
                    src = "https://example.com/gallery.jpg",
                    alt = "Gallery image"
                )
            )
        )

        val product = requireNotNull(mapper.map(dto))

        assertEquals(
            "https://example.com/gallery.jpg",
            product.imageUrl
        )
        assertEquals("Gallery image", product.imageAlt)
    }

    @Test
    fun `GS-MOB-011-2 null labels become an empty list`() {
        val product = requireNotNull(
            mapper.map(validDto().copy(labels = null))
        )

        assertEquals(emptyList<String>(), product.labels)
    }

    @Test
    fun `GS-MOB-011-3 empty labels remain an empty list`() {
        val product = requireNotNull(
            mapper.map(validDto().copy(labels = emptyList()))
        )

        assertEquals(emptyList<String>(), product.labels)
    }

    @Test
    fun `GS-MOB-009-2 unusable records are excluded while zero price is preserved`() {
        // Documents our chosen validation policy.
        // Also supports GS-MOB-003 and GS-MOB-005.
        val dtos = listOf(
            validDto().copy(id = null, objectID = null),
            validDto().copy(title = " "),
            validDto().copy(price = null),
            validDto().copy(price = -1L),
            validDto().copy(price = 0L)
        )

        val products = mapper.mapAll(dtos)

        assertEquals(1, products.size)
        assertEquals(0L, products.single().rawPrice)
    }
}
