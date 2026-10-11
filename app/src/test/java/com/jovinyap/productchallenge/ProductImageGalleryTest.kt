package com.jovinyap.productchallenge

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProductImageGalleryTest {

    @get:Rule
    val compose = createComposeRule()

    private val imageLoader = ImageLoader.Builder(
        ApplicationProvider.getApplicationContext()
    ).build()

    @Test
    fun multiplePhotosCanBeSwipedAndExposeAccessiblePosition() {
        val product = Product(
            id = "gallery-product",
            title = "Gallery product",
            rawPrice = 5000L,
            colour = "Black",
            descriptionHtml = null,
            imageUrl = "https://example.com/front.jpg",
            imageAlt = "Front",
            labels = emptyList(),
            galleryImages = listOf(
                ProductPhoto("https://example.com/front.jpg", "Front"),
                ProductPhoto("https://example.com/back.jpg", "Back")
            )
        )

        compose.setContent {
            ProductCatalogueTheme {
                ProductImageGallery(product, imageLoader)
            }
        }

        val pager = compose.onNodeWithTag("product_gallery_pager")
        pager.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Image 1 of 2"
            )
        )

        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        pager.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Image 2 of 2"
            )
        )
    }
}
