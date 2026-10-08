package com.jovinyap.productchallenge

/**
 * Retrieves displayable products, using the established mapper validation policy.
 * Network and decoding failures are thrown to the caller; cancellation is preserved.
 */
interface ProductRepository {
    /** Waits for one catalogue request without blocking the calling thread. */
    suspend fun fetchProducts(): List<Product>
}
