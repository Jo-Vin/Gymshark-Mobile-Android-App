package com.jovinyap.productchallenge

/** Exactly one catalogue state is active, so loading, content and errors cannot overlap. */
sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Content(val products: List<Product>) : ProductListUiState
    data object Empty : ProductListUiState
    // The future screen supplies a resource-based message; raw exception text is not UI text.
    data object Error : ProductListUiState
}
