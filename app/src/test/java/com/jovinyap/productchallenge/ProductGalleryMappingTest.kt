package com.jovinyap.productchallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductGalleryMappingTest {

    private val mapper = ProductMapper()

    @Test
    fun featuredPhotoIsFirstAndDuplicateUrlsAreRemoved() {
        val product = requireNotNull(
            mapper.map(
                baseDto().copy(
                    featuredMedia = ProductMediaDto(" https://example.com/front.jpg ", "Front"),
                    media = listOf(
                        ProductMediaDto("https://example.com/front.jpg", "Duplicate"),
                        ProductMediaDto("https://example.com/back.jpg", "Back")
                    )
                )
            )
        )

        assertEquals(
            listOf(
                ProductPhoto("https://example.com/front.jpg", "Front"),
                ProductPhoto("https://example.com/back.jpg", "Back")
            ),
            product.galleryImages
        )
        assertEquals("https://example.com/front.jpg", product.imageUrl)
        assertEquals("Front", product.imageAlt)
    }

    @Test
    fun blankPhotosAreOmittedWithoutDroppingTheProduct() {
        val product = requireNotNull(
            mapper.map(
                baseDto().copy(
                    featuredMedia = ProductMediaDto(" "),
                    media = listOf(ProductMediaDto(null), ProductMediaDto(" "))
                )
            )
        )

        assertEquals(emptyList<ProductPhoto>(), product.galleryImages)
        assertNull(product.imageUrl)
        assertNull(product.imageAlt)
        assertEquals("Gallery test product", product.title)
    }

    @Test
    fun invalidNonBlankPhotoUrlIsPreservedForImageFallback() {
        val product = requireNotNull(
            mapper.map(
                baseDto().copy(
                    media = listOf(ProductMediaDto("not a url", "Still supplied"))
                )
            )
        )

        assertEquals(listOf(ProductPhoto("not a url", "Still supplied")), product.galleryImages)
    }

    @Test
    fun photoAltTextPreservesUnicodeAndEmoji() {
        val product = requireNotNull(
            mapper.map(
                baseDto().copy(
                    media = listOf(ProductMediaDto("https://example.com/photo.jpg", "背面 🏋️‍♀️"))
                )
            )
        )

        assertEquals("背面 🏋️‍♀️", product.galleryImages.single().alt)
    }

    private fun baseDto() = ProductDto(
        id = 1L,
        title = "Gallery test product",
        price = 50L
    )
}
