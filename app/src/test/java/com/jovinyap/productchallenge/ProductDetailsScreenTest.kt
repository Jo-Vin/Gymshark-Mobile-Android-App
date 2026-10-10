package com.jovinyap.productchallenge

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
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

        compose.onNodeWithTag("product_details_title").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("product_description_body").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Comfort & support", substring = true), useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("product_description_body").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("product_description_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("product_description_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("product_details_price").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(formatGbpPrice(4500L), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("product_description_body").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.image_unavailable))
            .performScrollTo()
            .assertIsDisplayed()
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

        compose.onNodeWithTag("product_details_price").performScrollTo().assertIsDisplayed()
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

        compose.onNodeWithTag("product_details_price").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(formatGbpPrice(1000L), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("L").performScrollTo().assertIsDisplayed()
    }
    @Test
    // GS-MOB-013/009: detail attributes belong to the selected product and omit absent optional fields.
    fun detailShowsSelectedProductAttributesWithoutNullText() {
        val product = Product(
            id = "attributes",
            title = "Distinct Product",
            rawPrice = 1000L,
            colour = "Distinct Colour",
            descriptionHtml = null,
            imageUrl = null,
            imageAlt = null,
            labels = listOf("Limited", "New"),
            fit = "Distinct Fit"
        )

        compose.setContent {
            ProductCatalogueTheme { ProductDetailsScreen(product, null, {}, {}, imageLoader) }
        }

        compose.onNodeWithText("Distinct Colour").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Distinct Fit").assertIsDisplayed()
        compose.onNodeWithText("Limited").assertIsDisplayed()
        compose.onNodeWithText("New").assertIsDisplayed()
        compose.onAllNodesWithTag("product_description_body").assertCountEquals(1)
    }

    @Test
    // GS-MOB-032/033/024: baseline, additional and unavailable sizes expose usable semantics.
    fun sizeOptionsExposeAvailabilityAndSelectTheCorrectVariant() {
        val selectedVariantId = mutableStateOf<String?>(null)
        val product = Product(
            id = "sizes",
            title = "Size Product",
            rawPrice = 1000L,
            colour = null,
            descriptionHtml = null,
            imageUrl = null,
            imageAlt = null,
            labels = emptyList(),
            variants = listOf(
                ProductVariant("xs-id", "XS", 1100L, false),
                ProductVariant("s-id", " s ", 1200L, true),
                ProductVariant("3xl-id", "3XL", 1300L, true)
            )
        )

        compose.setContent {
            ProductCatalogueTheme {
                ProductDetailsScreen(
                    product = product,
                    selectedVariantId = selectedVariantId.value,
                    onBack = {},
                    onVariantSelected = { selectedVariantId.value = it },
                    imageLoader = imageLoader
                )
            }
        }

        compose.onNodeWithTag("product_size_selector").performScrollTo()
        compose.onNodeWithTag("product_size_option_XS").assertIsNotEnabled()
        compose.onNodeWithTag("product_size_option_M").assertIsNotEnabled()
        compose.onNodeWithTag("product_size_option_S").assertIsEnabled().performClick()
        compose.onNodeWithTag("product_size_option_3XL").assertIsEnabled()
        compose.runOnIdle { assertEquals("s-id", selectedVariantId.value) }
        compose.onNodeWithTag("product_size_option_S").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ToggleableState,
                androidx.compose.ui.state.ToggleableState.On
            )
        )
    }

    @Test
    // GS-MOB-014/024: description starts expanded and can be collapsed and reopened.
    fun descriptionIsExpandedByDefaultAndTogglesAccessibly() {
        val product = Product(
            id = "description",
            title = "Description Product",
            rawPrice = 1000L,
            colour = null,
            descriptionHtml = "<p><strong>Heading</strong></p><p>Comfort &amp; support 😀</p>",
            imageUrl = null,
            imageAlt = null,
            labels = emptyList()
        )

        compose.setContent {
            ProductCatalogueTheme { ProductDetailsScreen(product, null, {}, {}, imageLoader) }
        }

        compose.onNodeWithTag("product_description_body").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("product_description_body").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("product_description_toggle").performScrollTo().performClick()
        compose.onAllNodesWithTag("product_description_body").assertCountEquals(0)
        compose.onNodeWithTag("product_description_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("product_description_body").performScrollTo().assertIsDisplayed()
    }
    @Test
    // GS-MOB-013: valid model information is rendered inside the product-image container.
    fun modelInformationOverlayIsShownWhenDescriptionContainsModelData() {
        val product = Product(
            id = "model-overlay",
            title = "Model Product",
            rawPrice = 1000L,
            colour = null,
            descriptionHtml = "<p>Model is 5'7\" and wears a size XS</p>",
            imageUrl = null,
            imageAlt = null,
            labels = emptyList()
        )

        compose.setContent {
            ProductCatalogueTheme { ProductDetailsScreen(product, null, {}, {}, imageLoader) }
        }

        compose.onNode(hasTestTag("product_model_information") and hasAnyAncestor(hasTestTag("product_image_area")))
            .assertIsDisplayed()

    }

    @Test
    // GS-MOB-013: missing model information does not create an empty image overlay.
    fun missingModelInformationDoesNotShowOverlay() {
        val product = Product(
            id = "model-overlay-missing",
            title = "Model Product",
            rawPrice = 1000L,
            colour = null,
            descriptionHtml = "<p>Model information unavailable</p>",
            imageUrl = null,
            imageAlt = null,
            labels = emptyList()
        )

        compose.setContent {
            ProductCatalogueTheme { ProductDetailsScreen(product, null, {}, {}, imageLoader) }
        }

        compose.onAllNodesWithTag("product_model_information").assertCountEquals(0)
    }
}
