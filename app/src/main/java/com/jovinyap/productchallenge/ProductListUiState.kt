package com.jovinyap.productchallenge

/** Exactly one catalogue state is active, so loading, content and errors cannot overlap. */
sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Content(
        val products: List<Product>,
        val selectedProductId: String? = null,
        val isDetailsVisible: Boolean = false,
        val selectedVariantId: String? = null
    ) : ProductListUiState
    data object Empty : ProductListUiState
    // The screen supplies a resource-based message; raw exception text is not UI text.
    data object Error : ProductListUiState
}
