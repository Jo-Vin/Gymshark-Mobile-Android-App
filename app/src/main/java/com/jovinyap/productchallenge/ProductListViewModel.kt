package com.jovinyap.productchallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

/**
 * Owns catalogue state and retry handling, using the existing repository contract.
 * Loading starts once on creation. The screen observes state and sends user actions here.
 */
class ProductListViewModel(private val repository: ProductRepository) : ViewModel() {
    // Only this ViewModel can write state; consumers receive a read-only StateFlow.
    private val mutableUiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = mutableUiState.asStateFlow()

    init {
        loadProducts()
    }

    /** Retry is available after failure; taps during loading cannot start duplicate requests. */
    fun retry() {
        if (mutableUiState.value == ProductListUiState.Error) {
            loadProducts()
        }
    }

    /** Keep only a known product ID selected; downloading and detail navigation are separate concerns. */
    fun selectProduct(productId: String) {
        val content = mutableUiState.value as? ProductListUiState.Content ?: return
        if (content.products.any { it.id == productId }) {
            mutableUiState.value = content.copy(selectedProductId = productId)
        }
    }

    /** Opens details only for a current product, so an unknown ID cannot show another product. */
    fun openDetails(productId: String) {
        val content = mutableUiState.value as? ProductListUiState.Content ?: return
        if (content.products.any { it.id == productId }) {
            mutableUiState.value = content.copy(
                selectedProductId = productId,
                isDetailsVisible = true,
                selectedVariantId = null
            )
        }
    }

    fun closeDetails() {
        val content = mutableUiState.value as? ProductListUiState.Content ?: return
        mutableUiState.value = content.copy(isDetailsVisible = false, selectedVariantId = null)
    }

    /** Selects a variant only when it belongs to the currently selected product. */
    fun selectVariant(productId: String, variantId: String) {
        val content = mutableUiState.value as? ProductListUiState.Content ?: return
        val product = content.products.firstOrNull { it.id == productId } ?: return
        if (product.variants.any { it.id == variantId }) {
            mutableUiState.value = content.copy(selectedVariantId = variantId)
        }
    }

    private fun loadProducts() {
        // Change state immediately so a second retry tap sees Loading and is ignored.
        mutableUiState.value = ProductListUiState.Loading
        // viewModelScope cancels this work when its owner permanently clears the ViewModel.
        viewModelScope.launch {
            try {
                val products = repository.fetchProducts()
                mutableUiState.value = if (products.isEmpty()) {
                    ProductListUiState.Empty
                } else {
                    // Copy the list so subsequent changes to a repository-owned list cannot alter state.
                    ProductListUiState.Content(products.toList())
                }
            } catch (_: IOException) {
                mutableUiState.value = ProductListUiState.Error
            } catch (_: SerializationException) {
                mutableUiState.value = ProductListUiState.Error
            }
            // Cancellation is deliberately not caught: stopping work is not a loading failure.
        }
    }
}
