package com.jovinyap.productchallenge

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** State transitions only: these tests do not establish rendered UI or live network behaviour. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()

    @After
    fun clearViewModels() {
        // Mirror an Activity being permanently destroyed, cancelling its ViewModel work.
        store.clear()
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
    }

    private fun create(repository: ProductRepository): ProductListViewModel =
        ProductListViewModel(repository).also { store.put("products", it) }

    private val product = Product(
        id = "catalogue-42",
        title = "Speed Leggings",
        rawPrice = 1000L,
        colour = "Navy",
        descriptionHtml = null,
        imageUrl = null,
        imageAlt = null,
        labels = emptyList()
    )

    @Test
    fun loadingTransitionsToContentWhenProductsArrive() = runTest {
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val viewModel = create(repository)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        // Start the request, but keep its result pending to prove the loading state.
        runCurrent()
        assertEquals(1, repository.callCount)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        response.complete(listOf(product))
        advanceUntilIdle()

        assertEquals(ProductListUiState.Content(listOf(product)), viewModel.uiState.value)
    }

    @Test
    fun loadingTransitionsToErrorOnNetworkFailure() = runTest {
        // Partial evidence for GS-MOB-015, following the existing parser test mapping.
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val viewModel = create(repository)
        runCurrent()
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        response.completeExceptionally(IOException("Controlled failure"))
        advanceUntilIdle()

        assertEquals(ProductListUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun loadingTransitionsToErrorOnDecodingFailure() = runTest {
        // Partial evidence for GS-MOB-015; resource text and retry rendering are checked separately in ProductListUiChecks.
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val viewModel = create(repository)
        runCurrent()
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        response.completeExceptionally(SerializationException("Controlled malformed response"))
        advanceUntilIdle()

        assertEquals(ProductListUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun retryTransitionsFromErrorThroughLoadingToContent() = runTest {
        // Partial evidence for GS-MOB-015; this test verifies state only; ProductListUiChecks verifies the retry button.
        val repository = ControlledRepository()
        val first = repository.nextResponse()
        val second = repository.nextResponse()
        val viewModel = create(repository)
        runCurrent()
        first.completeExceptionally(IOException("Controlled first attempt failure"))
        advanceUntilIdle()
        assertEquals(ProductListUiState.Error, viewModel.uiState.value)
        assertEquals(1, repository.callCount)

        viewModel.retry()
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)
        runCurrent()
        assertEquals(2, repository.callCount)
        // A repeated tap while the retry is pending must not start another request.
        viewModel.retry()
        runCurrent()
        assertEquals(2, repository.callCount)

        second.complete(listOf(product))
        advanceUntilIdle()

        assertEquals(ProductListUiState.Content(listOf(product)), viewModel.uiState.value)
    }

    @Test
    fun emptyCatalogueTransitionsToEmptyInsteadOfContent() = runTest {
        // Partial evidence for GS-MOB-015; ProductListUiChecks separately verifies the empty-screen message.
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val viewModel = create(repository)
        runCurrent()
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        response.complete(emptyList())
        advanceUntilIdle()

        assertEquals(ProductListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun retryWhileInitialRequestIsPendingDoesNotDuplicateLoading() = runTest {
        val repository = ControlledRepository()
        repository.nextResponse()
        val viewModel = create(repository)

        viewModel.retry()
        runCurrent()
        viewModel.retry()
        runCurrent()

        assertEquals(1, repository.callCount)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun retryAfterSuccessfulLoadDoesNotReloadProducts() = runTest {
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val viewModel = create(repository)
        response.complete(listOf(product))
        advanceUntilIdle()
        assertEquals(ProductListUiState.Content(listOf(product)), viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
        assertEquals(ProductListUiState.Content(listOf(product)), viewModel.uiState.value)
    }

    @Test
    fun clearingViewModelCancelsPendingRequestWithoutPublishingError() = runTest {
        val repository = ControlledRepository()
        repository.nextResponse()
        val viewModel = create(repository)
        runCurrent()
        assertFalse(repository.cancelled)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        store.clear()
        advanceUntilIdle()

        assertTrue(repository.cancelled)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun repositoryCancellationIsPropagatedWithoutBecomingError() = runTest {
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val viewModel = create(repository)
        runCurrent()
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)

        val loadingJob = requireNotNull(viewModel.viewModelScope.coroutineContext[Job]).children.single()
        response.completeExceptionally(CancellationException("Controlled cancellation"))
        advanceUntilIdle()

        assertTrue(repository.cancelled)
        assertTrue(loadingJob.isCancelled)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun selectingKnownProductUpdatesOnlySelectionState() = runTest {
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val other = product.copy(id = "second", title = "Training Shorts")
        val viewModel = create(repository)
        response.complete(listOf(product, other))
        advanceUntilIdle()
        assertEquals(ProductListUiState.Content(listOf(product, other)), viewModel.uiState.value)

        viewModel.selectProduct("second")

        assertEquals(ProductListUiState.Content(listOf(product, other), "second"), viewModel.uiState.value)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun selectingUnknownProductLeavesContentUnchanged() = runTest {
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val viewModel = create(repository)
        response.complete(listOf(product))
        advanceUntilIdle()
        val before = viewModel.uiState.value

        viewModel.selectProduct("unknown")

        assertEquals(before, viewModel.uiState.value)
        assertEquals(1, repository.callCount)
    }

    @Test
    // GS-MOB-012/013: detail navigation uses the selected product's stable identifier.
    fun openingDetailsUsesTheStableIdAndRejectsUnknownProducts() = runTest {
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val other = product.copy(id = "second", title = product.title)
        val viewModel = create(repository)
        response.complete(listOf(product, other))
        advanceUntilIdle()

        viewModel.openDetails("second")

        assertEquals("second", (viewModel.uiState.value as ProductListUiState.Content).selectedProductId)
        assertTrue((viewModel.uiState.value as ProductListUiState.Content).isDetailsVisible)

        viewModel.closeDetails()
        viewModel.openDetails("missing")

        assertFalse((viewModel.uiState.value as ProductListUiState.Content).isDetailsVisible)
        assertEquals("second", (viewModel.uiState.value as ProductListUiState.Content).selectedProductId)
    }

    @Test
    // GS-MOB-009: selecting a known variant changes detail pricing without inventing unknown variants.
    fun selectingVariantChangesDetailPriceOnlyForKnownVariant() = runTest {
        val repository = ControlledRepository()
        val response = repository.nextResponse()
        val withVariants = product.copy(
            variants = listOf(ProductVariant("small", "S", 4500L, true))
        )
        val viewModel = create(repository)
        response.complete(listOf(withVariants))
        advanceUntilIdle()

        viewModel.openDetails(withVariants.id)
        viewModel.selectVariant(withVariants.id, "small")

        val state = viewModel.uiState.value as ProductListUiState.Content
        assertEquals("small", state.selectedVariantId)
        viewModel.selectVariant(withVariants.id, "unknown")
        assertEquals("small", (viewModel.uiState.value as ProductListUiState.Content).selectedVariantId)
    }

    @Test
    fun selectingProductWhileLoadingDoesNotChangeStateOrStartRequests() = runTest {
        val repository = ControlledRepository()
        repository.nextResponse()
        val viewModel = create(repository)
        runCurrent()

        viewModel.selectProduct(product.id)

        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)
        assertEquals(1, repository.callCount)
    }

    /** Each deferred response stays pending until the test supplies products or an error. */
    private class ControlledRepository : ProductRepository {
        private val responses = ArrayDeque<CompletableDeferred<List<Product>>>()
        var callCount = 0
            private set
        var cancelled = false
            private set

        fun nextResponse(): CompletableDeferred<List<Product>> =
            CompletableDeferred<List<Product>>().also { responses.addLast(it) }

        override suspend fun fetchProducts(): List<Product> {
            callCount++
            return try {
                responses.removeFirst().await()
            } catch (failure: CancellationException) {
                cancelled = true
                throw failure
            }
        }
    }
}
