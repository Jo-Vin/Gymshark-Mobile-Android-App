package com.jovinyap.productchallenge

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser-level verification only.
 *
 * These tests do not establish that products render correctly,
 * image requests succeed, HTML is formatted, or errors reach the UI.
 */
class ProductPayloadParserTest {

    private val parser = ProductPayloadParser()

    @Test
    fun `GS-MOB-002-1 real fixture preserves catalogue and product data`() {
        // Also supports:
        // GS-MOB-003: identifiers are preserved without truncation.
        // GS-MOB-009: raw price and colour are preserved.
        // GS-MOB-013: further product information is available.
        // Mapping and UI verification are still required.

        // Arrange
        val fixture = requireNotNull(
            javaClass.getResource("/algolia-example-payload.json")
        )
        val json = fixture.readText()

        // Act
        val products = parser.parse(json)

        // Assert: these values describe the saved fixture.
        assertEquals(60, products.size)

        val first = products.first()

        assertEquals(6732609257571L, first.id)
        assertEquals("6732609257571", first.objectID)
        assertEquals("Speed Leggings", first.title)
        assertEquals("Navy", first.colour)
        assertEquals(1000L, first.price)
        assertEquals(true, first.inStock)
        assertNull(first.labels)

        assertEquals(
            "https://cdn.shopify.com/s/files/1/1326/4923/products/" +
                    "SpeedLEGGINGNavy-B3A3E-UBCY.A-Edit_BK.jpg?v=1649254794",
            first.featuredMedia?.src
        )

        assertEquals(6, first.media?.size)
        assertEquals(1000L, first.availableSizes?.first()?.price)

        assertTrue(
            first.description.orEmpty().contains("RUN WITH IT")
        )
    }

    @Test
    fun `GS-MOB-010-1 multiple labels and nested product data are preserved`() {
        // Also supports:
        // GS-MOB-025: Unicode text is preserved.
        // GS-MOB-014: original HTML reaches the rendering boundary.
        // GS-MOB-013: variant and image information is preserved.
        // This does not verify badges, HTML rendering or placeholders.

        // Arrange
        val json = """
            {
              "hits": [
                {
                  "id": 42,
                  "title": "训练紧身裤 🏋️",
                  "price": 65,
                  "description": "<p>Comfort &amp; support</p>",
                  "labels": ["new", "future-label"],
                  "featuredMedia": {
                    "src": "not-a-valid-url",
                    "alt": "Product front"
                  },
                  "media": [
                    {
                      "src": "https://example.com/back.jpg",
                      "alt": "Product back"
                    }
                  ],
                  "availableSizes": [
                    {
                      "id": 101,
                      "size": "s",
                      "price": 45,
                      "inStock": false,
                      "inventoryQuantity": 0,
                      "sku": "TEST-S"
                    }
                  ],
                  "extraField": "Ignore this unmodelled property"
                }
              ]
            }
        """.trimIndent()

        // Act
        val product = parser.parse(json).single()

        // Assert
        assertEquals(42L, product.id)
        assertEquals("训练紧身裤 🏋️", product.title)
        assertEquals(65L, product.price)

        assertEquals(
            "<p>Comfort &amp; support</p>",
            product.description
        )
        assertEquals(
            listOf("new", "future-label"),
            product.labels
        )

        // Parsing preserves the supplied string.
        // Image validation and loading are separate responsibilities.
        assertEquals(
            "not-a-valid-url",
            product.featuredMedia?.src
        )
        assertEquals("Product front", product.featuredMedia?.alt)

        val image = requireNotNull(product.media).single()

        assertEquals("https://example.com/back.jpg", image.src)
        assertEquals("Product back", image.alt)

        val variant = requireNotNull(product.availableSizes).single()

        assertEquals(101L, variant.id)
        assertEquals("s", variant.size)
        assertEquals(45L, variant.price)
        assertEquals(false, variant.inStock)
        assertEquals(0, variant.inventoryQuantity)
        assertEquals("TEST-S", variant.sku)
    }

    @Test
    fun `GS-MOB-002-2 omitted product fields do not break decoding`() {
        // Supports missing-image handling under GS-MOB-007.
        // Placeholder behaviour requires mapper and UI tests.

        // Arrange
        val json = """{"hits": [{}]}"""

        // Act
        val product = parser.parse(json).single()

        // Assert
        assertNull(product.id)
        assertNull(product.objectID)
        assertNull(product.sku)
        assertNull(product.handle)
        assertNull(product.title)
        assertNull(product.description)
        assertNull(product.type)
        assertNull(product.fit)
        assertNull(product.colour)
        assertNull(product.price)
        assertNull(product.inStock)
        assertNull(product.labels)
        assertNull(product.featuredMedia)
        assertNull(product.media)
        assertNull(product.availableSizes)
    }

    @Test
    fun `GS-MOB-011-1 null labels and optional values decode safely`() {
        // Partial evidence for GS-MOB-011.
        // The mapper must normalise null labels to an empty list.
        // The UI must show no badges for null or empty labels.

        // Arrange
        val json = """
            {
              "hits": [
                {
                  "title": "Speed Leggings",
                  "price": null,
                  "colour": null,
                  "labels": null,
                  "featuredMedia": null,
                  "media": null,
                  "availableSizes": null
                }
              ]
            }
        """.trimIndent()

        // Act
        val product = parser.parse(json).single()

        // Assert
        assertEquals("Speed Leggings", product.title)
        assertNull(product.price)
        assertNull(product.colour)
        assertNull(product.labels)
        assertNull(product.featuredMedia)
        assertNull(product.media)
        assertNull(product.availableSizes)
    }

    @Test
    fun `GS-MOB-002-3 empty hits produces an empty product collection`() {
        // Supports GS-MOB-015.
        // The ViewModel must separately expose the empty UI state.

        // Arrange
        val json = """{"hits": []}"""

        // Act
        val products = parser.parse(json)

        // Assert
        assertTrue(products.isEmpty())
    }

    @Test
    fun `GS-MOB-002-4 malformed JSON is rejected`() {
        // Negative decoding case supporting GS-MOB-015.
        // Error-state presentation is tested in the ViewModel.

        // Arrange
        val json = "{"

        // Act and assert
        assertThrows(SerializationException::class.java) {
            parser.parse(json)
        }
    }

    @Test
    fun `GS-MOB-002-5 response without hits is rejected`() {
        // Missing hits must not masquerade as a valid empty catalogue.

        // Arrange
        val json = "{}"

        // Act and assert
        assertThrows(SerializationException::class.java) {
            parser.parse(json)
        }
    }

    @Test
    fun `GS-MOB-002-6 incorrect price structure is rejected`() {
        // Supports data integrity for GS-MOB-009.
        // Invalid data must not become an invented display price.

        // Arrange
        val json = """
            {
              "hits": [
                {
                  "title": "Speed Leggings",
                  "price": {"unexpected": 65}
                }
              ]
            }
        """.trimIndent()

        // Act and assert
        assertThrows(SerializationException::class.java) {
            parser.parse(json)
        }
    }
}