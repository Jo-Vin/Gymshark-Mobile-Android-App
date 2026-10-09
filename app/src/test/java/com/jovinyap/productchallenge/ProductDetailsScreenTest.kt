package com.jovinyap.productchallenge

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import org.junit.Rule
import org.junit.Test

class ProductDetailsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val imageLoader = ImageLoader.Builder(context).build()

    @Test
    // GS-MOB-012/013/014: details render the selected product, variant price, image fallback and HTML text.
    fun detailsRenderReadableDescriptionAndSelectedVariantPrice() {
        val product = Product(
            id = "same-title-2",
            title = "Speed Leggings",
            rawPrice = 1000L,
            colour = "Navy",
            descriptionHtml = "<p>Comfort &amp; support 😀</p><p>Move freely</p>",
            imageUrl = null,
            imageAlt = null,
            labels = listOf("new"),
            fit = "Compressive",
            variants = listOf(ProductVariant("small", "S", 4500L, true))
        )

        compose.setContent {
            ProductCatalogueTheme {
                ProductDetailsScreen(
                    product = product,
                    selectedVariantId = "small",
                    onBack = {},
                    onVariantSelected = {},
                    imageLoader = imageLoader
                )
            }
        }

        compose.onNodeWithText("Speed Leggings").assertIsDisplayed()
        compose.onNodeWithTag("product_description_toggle").performClick()
        compose.onNodeWithText("£45.00").assertIsDisplayed()
        compose.onNodeWithText("Comfort & support 😀\n\nMove freely").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.image_unavailable)).assertIsDisplayed()
    }

    @Test
    // GS-MOB-012: a missing selected product shows an unavailable state instead of another product.
    fun unavailableProductShowsMessageAndBackAction() {
        compose.setContent {
            ProductCatalogueTheme {
                ProductDetailsScreen(
                    product = null,
                    selectedVariantId = null,
                    onBack = {},
                    onVariantSelected = {},
                    imageLoader = imageLoader
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.product_details_unavailable)).assertIsDisplayed()
        compose.onNodeWithTag("product_details_back").assertIsDisplayed()
    }

    @Test
    // GS-MOB-009: a selected variant without a price is explicitly unavailable.
    fun selectedVariantWithoutPriceShowsUnavailableInsteadOfInventingPrice() {
        val product = Product(
            id = "no-variant-price",
            title = "Training Shorts",
            rawPrice = 1000L,
            colour = null,
            descriptionHtml = null,
            imageUrl = null,
            imageAlt = null,
            labels = emptyList(),
            variants = listOf(ProductVariant("medium", "M", null, false))
        )

        compose.setContent {
            ProductCatalogueTheme {
                ProductDetailsScreen(
                    product = product,
                    selectedVariantId = "medium",
                    onBack = {},
                    onVariantSelected = {},
                    imageLoader = imageLoader
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.product_price_unavailable)).assertIsDisplayed()
    }

    @Test
    // GS-MOB-009: the product-level price is shown before a variant is selected.
    fun detailShowsProductPriceBeforeVariantSelection() {
        val product = Product(
            id = "product-price",
            title = "Speed Leggings",
            rawPrice = 1000L,
            colour = null,
            descriptionHtml = null,
            imageUrl = null,
            imageAlt = null,
            labels = emptyList(),
            variants = listOf(ProductVariant("large", "l", 4500L, true))
        )

        compose.setContent {
            ProductCatalogueTheme {
                ProductDetailsScreen(product, null, {}, {}, imageLoader)
            }
        }

        compose.onNodeWithText("Â£10.00").assertIsDisplayed()
        compose.onNodeWithText("L").assertIsDisplayed()
    }
}
