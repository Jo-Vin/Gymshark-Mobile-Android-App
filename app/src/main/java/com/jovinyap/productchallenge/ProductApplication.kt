package com.jovinyap.productchallenge

import android.app.Application
import okhttp3.OkHttpClient

/**
 * Owns the application-wide repository and shared HTTP client.
 * Tests override this property in a test-only Application, so Activity tests never call the CDN.
 */
open class ProductApplication : Application() {
    open val productRepository: ProductRepository by lazy {
        RemoteProductRepository(OkHttpClient())
    }
}
