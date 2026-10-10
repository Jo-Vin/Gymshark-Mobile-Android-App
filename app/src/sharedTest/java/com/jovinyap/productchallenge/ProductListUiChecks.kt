package com.jovinyap.productchallenge

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.junit.Rule
import org.junit.Test

/**
 * Shared assertions run unchanged with Robolectric and AndroidJUnit4.
 * Catalogue and image responses are controlled locally, never fetched from the live endpoint.
 */
abstract class ProductListUiChecks {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ViewModelStore()
    private val imageOutcome = CompletableDeferred<Throwable?>()
    private val imageRequested = CompletableDeferred<Unit>()
    private val imageLoader = ImageLoader.Builder(context).components {
        add(Interceptor { chain ->
            imageRequested.complete(Unit)
            val failure = imageOutcome.await()
            if (failure == null) SuccessResult(ColorDrawable(Color.BLUE).asImage(), chain.request)
            else ErrorResult(null, chain.request, failure)
        })
    }.build()
    private val first = Product(
        "first", "Speed Leggings", 1000L, "Navy", null, null, null, listOf("new")
    )
    private val second = first.copy(id = "second", title = "Training Shorts", colour = "Black", labels = emptyList())

    @After fun cleanup() {
        compose.runOnIdle { store.clear() }
        imageLoader.shutdown()
    }

    private fun show(state: ProductListUiState, onRetry: () -> Unit = {}, onSelected: (String) -> Unit = {}, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ProductCatalogueTheme { ProductListScreen(state, onRetry, onSelected, imageLoader = imageLoader) }
            }
        }
    }

    private fun viewModel(repository: ProductRepository): ProductListViewModel {
        lateinit var result: ProductListViewModel
        compose.runOnIdle {
            result = ProductListViewModel(repository)
            store.put("products", result)
        }
        return result
    }

    private fun showRoute(model: ProductListViewModel) {
        compose.setContent {
            ProductCatalogueTheme { ProductListRoute(model, imageLoader = imageLoader) }
        }
    }

    @Test fun contentShowsProductTitlesColoursLabelsAndLocalFallback() {
        // Component evidence for GS-MOB-007/024/025; full assessment coverage remains broader.
        show(ProductListUiState.Content(listOf(first, second)))

        compose.onNodeWithText("Speed Leggings", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Navy", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("new", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.image_unavailable)) and hasAnyAncestor(hasTestTag("product_card_first")), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("product_grid").performScrollToNode(hasTestTag("product_card_second"))
        compose.onNode(
            hasText("Training Shorts") and hasAnyAncestor(hasTestTag("product_card_second")),
            useUnmergedTree = true
        ).assertIsDisplayed()
        compose.onNode(hasTestTag("product_image_logo") and hasAnyAncestor(hasTestTag("product_card_second")), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("product_grid").performScrollToNode(hasTestTag("product_card_first"))
        compose.onNodeWithTag("product_card_first").assertHasClickAction().assertIsNotSelected()
        assertFalse(imageRequested.isCompleted)
    }

    @Test fun pricesAlignDespiteDifferentTitleColourAndFitLengths() {
        // Partial GS-MOB-009/024 evidence: price presentation and normal-font alignment.
        val longProduct = second.copy(
            title = "Conditioning Club Washed Training Tank",
            colour = "Haze Pink/Soft Brown/Wash", rawPrice = 65L, fit = "Oversized Fit"
        )
        show(ProductListUiState.Content(listOf(first, longProduct)))
        val firstPrice = compose.onNodeWithText(formatGbpPrice(1000L), useUnmergedTree = true)
        val secondPrice = compose.onNodeWithText(formatGbpPrice(65L), useUnmergedTree = true)
        firstPrice.performScrollTo().assertIsDisplayed()
        secondPrice.assertIsDisplayed()
        compose.onNodeWithText("Oversized Fit", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(firstPrice.fetchSemanticsNode().boundsInRoot.top,
            secondPrice.fetchSemanticsNode().boundsInRoot.top, 1f)
        val label = compose.onNodeWithText("new", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val photo = compose.onNode(hasTestTag("product_image_area") and hasAnyAncestor(hasTestTag("product_card_first")),
            useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Labels belong inside the image", label.top >= photo.top && label.bottom <= photo.bottom)
    }

    @Test
    fun largeFontsKeepProductPricesAccessible() {
        show(
            ProductListUiState.Content(
                listOf(first, second.copy(rawPrice = 45L))
            ),
            fontScale = 1.8f
        )
        compose
            .onNodeWithText(formatGbpPrice(45L), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose
            .onNodeWithTag("product_card_second")
            .assertHasClickAction()
    }

    @Test fun loadingShowsIndeterminateProgressWithoutCards() {
        show(ProductListUiState.Loading)

        compose.onNodeWithTag("catalogue_progress").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
        compose.onNodeWithTag("product_grid").assertDoesNotExist()
    }

    @Test fun emptyCatalogueShowsResourceMessageWithoutRetryOrSpinner() {
        // Partial evidence for the empty-state behaviour mapped to GS-MOB-015.
        show(ProductListUiState.Empty)

        compose.onNodeWithText(context.getString(R.string.catalogue_empty)).assertIsDisplayed()
        compose.onNodeWithTag("catalogue_retry").assertDoesNotExist()
        compose.onNodeWithTag("catalogue_progress").assertDoesNotExist()
    }

    @Test fun retryButtonLoadsAgainAndReplacesErrorWithContent() {
        // Partial UI evidence for GS-MOB-015; the test uses a fake repository.
        val initial = CompletableDeferred<List<Product>>()
        val retried = CompletableDeferred<List<Product>>()
        var calls = 0
        val model = viewModel(object : ProductRepository {
            override suspend fun fetchProducts(): List<Product> {
                calls++
                return if (calls == 1) initial.await() else retried.await()
            }
        })
        showRoute(model)
        compose.onNodeWithTag("catalogue_progress").assertIsDisplayed()
        compose.runOnIdle { initial.completeExceptionally(IOException("Controlled failure")) }
        compose.onNodeWithText(context.getString(R.string.catalogue_error)).assertIsDisplayed()

        compose.onNodeWithTag("catalogue_retry").assertIsEnabled().performClick()
        compose.onNodeWithTag("catalogue_progress").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, calls); retried.complete(listOf(first)) }

        compose.onNodeWithTag("product_card_first").assertIsDisplayed()
        compose.onNodeWithTag("catalogue_retry").assertDoesNotExist()
        compose.onNodeWithTag("catalogue_progress").assertDoesNotExist()
    }

    @Test fun selectingCardOpensTheCorrectProductAndBackReturnsToTheSelectedCard() {
        val model = viewModel(object : ProductRepository {
            override suspend fun fetchProducts() = listOf(first, second)
        })
        showRoute(model)
        compose.onNodeWithTag("product_card_first").assertIsNotSelected()
        compose.onNodeWithTag("product_grid").performScrollToNode(hasTestTag("product_card_second"))
        compose.onNodeWithTag("product_card_second").assertIsNotSelected()

        compose.onNodeWithTag("product_card_second").performClick()

        compose.onNodeWithTag("product_details_title").assertIsDisplayed()
        compose.onNodeWithTag("product_details_back").performClick()
        compose.onNodeWithTag("product_grid").performScrollToNode(hasTestTag("product_card_second"))
        compose.onNodeWithTag("product_card_second").assertIsSelected()
        compose.onNodeWithTag("product_grid").performScrollToNode(hasTestTag("product_card_first"))
        compose.onNodeWithTag("product_card_first").assertIsNotSelected().assertHasClickAction()
        compose.runOnIdle {
            assertEquals("second", (model.uiState.value as ProductListUiState.Content).selectedProductId)
        }
    }

    @Test
    // GS-MOB-034/009: selecting a size through the detail UI changes the displayed GBP variant price.
    fun selectingAvailableSizeUpdatesVisibleVariantPrice() {
        val pricedProduct = first.copy(
            rawPrice = 1000L,
            variants = listOf(ProductVariant("small-id", "S", 4500L, true))
        )
        val model = viewModel(object : ProductRepository {
            override suspend fun fetchProducts() = listOf(pricedProduct)
        })
        showRoute(model)

        compose.onNodeWithTag("product_card_first").performClick()
        compose.onNodeWithTag("product_details_price").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(formatGbpPrice(1000L), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("product_size_selector").performScrollTo()
        compose.onNodeWithTag("product_size_option_S").performClick()
        compose.onNodeWithTag("product_details_price").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(formatGbpPrice(4500L), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun gridScrollsToProductsBeyondTheInitialViewport() {
        val products = (1..20).map { first.copy(id = "item-$it", title = "Product $it") }
        show(ProductListUiState.Content(products))

        compose.onNodeWithTag("product_grid").performScrollToNode(hasTestTag("product_card_item-20"))

        compose.onNodeWithTag("product_card_item-20").assertIsDisplayed()
        compose.onNodeWithText("Product 20", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun imageLoadingAndFailureDoNotBlockCardSelection() {
        // Card integration evidence for GS-MOB-006/008; no download or decoder is exercised here.
        val pictured = first.copy(imageUrl = "https://example.com/controlled.png")
        var selected: String? = null
        show(ProductListUiState.Content(listOf(pictured)), onSelected = { selected = it })
        compose.waitUntil(5_000) { imageRequested.isCompleted }
        compose.onNodeWithTag("product_image_progress", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("product_card_first").performClick()
        compose.runOnIdle { assertEquals("first", selected); imageOutcome.complete(IOException("Controlled image failure")) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("product_image_progress", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }

        compose.onNodeWithText(context.getString(R.string.image_unavailable), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("product_card_first").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals("first", selected) }
    }

    @Test fun successfulImageExposesProductDescriptionAndRemovesSpinner() {
        // Card integration evidence for GS-MOB-006/024; the image is supplied by a fake loader.
        show(ProductListUiState.Content(listOf(first.copy(imageUrl = "https://example.com/controlled.png"))))
        compose.waitUntil(5_000) { imageRequested.isCompleted }
        compose.onNodeWithTag("product_image_progress", useUnmergedTree = true).assertIsDisplayed()

        compose.runOnIdle { imageOutcome.complete(null) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("product_image_progress", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }

        compose.onNodeWithContentDescription(context.getString(R.string.product_image_description, first.title), useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onAllNodesWithTag("product_image_logo", useUnmergedTree = true).assertCountEquals(0)
    }
}
