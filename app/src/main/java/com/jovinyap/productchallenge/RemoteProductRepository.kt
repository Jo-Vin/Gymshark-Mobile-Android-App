package com.jovinyap.productchallenge

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response

/**
 * Downloads the catalogue asynchronously, then reuses the existing parser and mapper.
 * Supply a shared OkHttp client as [callFactory], or a fake factory in tests.
 * Decoding and mapping use [dispatcher] so callers can safely invoke this from the main thread.
 */
class RemoteProductRepository(
    private val callFactory: Call.Factory,
    private val parser: ProductPayloadParser = ProductPayloadParser(),
    private val mapper: ProductMapper = ProductMapper(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ProductRepository {
    override suspend fun fetchProducts(): List<Product> {
        // First retrieve the raw response; the parser and mapper have separate responsibilities.
        val json = downloadCatalogue()
        // Decoding and validation are CPU work, so move them off the Android main thread.
        // There is no error-to-empty-list conversion: genuine failures still reach the caller.
        return withContext(dispatcher) {
            mapper.mapAll(parser.parse(json))
        }
    }

    // Bridge OkHttp callbacks to a suspend function: pause here until success, failure or cancellation.
    // The continuation represents the paused caller, which we resume once an outcome is available.
    private suspend fun downloadCatalogue(): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(CATALOGUE_URL).get().build()
        val call = callFactory.newCall(request)
        // Cancelling the coroutine must also stop its underlying network request.
        continuation.invokeOnCancellation { call.cancel() }
        // enqueue schedules asynchronous work; execute would block the calling thread.
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Resume by throwing the network error at the caller's suspension point.
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    // OkHttp invokes this callback on its worker thread, including body reads.
                    // use closes the response even if a body read or status check throws.
                    val json = response.use {
                        // A late response must be closed even when the caller has cancelled.
                        if (!continuation.isActive) return
                        // Receiving a response is not enough: HTTP errors must remain failures.
                        if (!it.isSuccessful) {
                            throw IOException("Catalogue request failed (HTTP ${it.code})")
                        }
                        val body = it.body ?: throw IOException("Catalogue response has no body")
                        body.string()
                    }
                    // The body is already closed; only the JSON text is returned to the caller.
                    continuation.resume(json)
                } catch (failure: IOException) {
                    // Body reads can fail too. Catch only I/O errors, never coroutine cancellation.
                    continuation.resumeWithException(failure)
                }
            }
        })
    }

    private companion object {
        const val CATALOGUE_URL =
            "https://cdn.develop.gymshark.com/training/mock-product-responses/algolia-example-payload.json"
    }
}
