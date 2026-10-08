package com.jovinyap.productchallenge

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Repository evidence for GS-MOB-001/002; live connectivity and UI presentation are not verified. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RemoteProductRepositoryTest {
    @Test
    fun `GS-MOB-001-1 successful retrieval decodes and maps the saved catalogue`() = runTest {
        // Also provides repository-level decoding evidence for GS-MOB-002.
        // Load saved API data from test resources; this test never contacts the CDN.
        val json = requireNotNull(javaClass.getResource("/algolia-example-payload.json")).readText()
        val body = TrackingBody(json)
        val calls = FakeCalls { it.respond(body) }
        val repository: ProductRepository = RemoteProductRepository(
            calls, dispatcher = StandardTestDispatcher(testScheduler)
        )

        // Exercise retrieval, decoding and mapping together through the public contract.
        val products = repository.fetchProducts()

        assertEquals(60, products.size)
        val first = products.first()
        assertEquals("6732609257571", first.id)
        assertEquals("Speed Leggings", first.title)
        assertEquals(1000L, first.rawPrice)
        assertEquals("Navy", first.colour)
        assertEquals(emptyList<String>(), first.labels)
        assertNull(first.imageAlt)
        assertEquals(
            "https://cdn.shopify.com/s/files/1/1326/4923/products/" +
                "SpeedLEGGINGNavy-B3A3E-UBCY.A-Edit_BK.jpg?v=1649254794",
            first.imageUrl
        )
        assertTrue(first.descriptionHtml.orEmpty().contains("RUN WITH IT"))
        val call = calls.created.single()
        assertEquals(
            "https://cdn.develop.gymshark.com/training/mock-product-responses/algolia-example-payload.json",
            call.request().url.toString()
        )
        assertEquals("GET", call.request().method)
        assertTrue(call.isExecuted())
        assertFalse(call.isCanceled())
        // Confirm the response stream was released after the JSON was read.
        assertTrue(body.closed)
    }

    @Test
    fun `GS-MOB-002-7 empty catalogue returns an empty list`() = runTest {
        val calls = FakeCalls { it.respond(TrackingBody("""{"hits":[]}""")) }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))

        assertTrue(repository.fetchProducts().isEmpty())
        assertEquals(1, calls.created.size)
    }

    @Test
    fun `GS-MOB-001-2 network failure reaches the caller with its cause`() = runTest {
        val expected = IOException("Controlled connection failure")
        val calls = FakeCalls { it.fail(expected) }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))

        val actual = expectFailure<IOException> { repository.fetchProducts() }

        assertPropagated(expected, actual)
        assertEquals(1, calls.created.size)
    }

    @Test
    fun `GS-MOB-002-8 malformed response remains a decoding failure`() = runTest {
        val body = TrackingBody("{")
        val calls = FakeCalls { it.respond(body) }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))

        expectFailure<SerializationException> { repository.fetchProducts() }

        assertTrue(body.closed)
    }

    @Test
    fun `GS-MOB-001-3 cancellation cancels the pending request and propagates`() = runTest {
        val calls = FakeCalls { /* Keep the request pending until the test cancels it. */ }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))
        var observed: CancellationException? = null
        var returned = false
        val job = launch {
            try {
                repository.fetchProducts()
                returned = true
            } catch (failure: CancellationException) {
                observed = failure
                throw failure
            }
        }
        // Run queued coroutine work until the request is waiting for its callback, without a delay.
        runCurrent()
        val call = calls.created.single()
        assertTrue(job.isActive)
        assertFalse(call.isCanceled())

        job.cancel(CancellationException("Caller cancelled"))
        // Process scheduled cancellation work deterministically instead of sleeping.
        advanceUntilIdle()

        assertTrue(job.isCancelled)
        assertTrue(call.isCanceled())
        assertNotNull(observed)
        assertFalse(returned)
        // OkHttp can report an IOException after cancellation; it must not revive the coroutine.
        call.fail(IOException("Cancelled"))
        runCurrent()
        assertFalse(returned)
    }

    @Test
    fun `GS-MOB-001-4 unsuccessful HTTP response remains an observable failure`() = runTest {
        val body = TrackingBody("""{"hits":[]}""")
        val calls = FakeCalls { it.respond(body, code = 503) }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))

        val failure = expectFailure<IOException> { repository.fetchProducts() }

        assertTrue(failure.message.orEmpty().contains("503"))
        assertTrue(body.closed)
    }

    @Test
    fun `GS-MOB-001-5 body read failure reaches the caller and closes the response`() = runTest {
        val expected = IOException("Controlled body read failure")
        val body = TrackingBody("", expected)
        val calls = FakeCalls { it.respond(body) }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))

        assertPropagated(expected, expectFailure<IOException> { repository.fetchProducts() })
        assertTrue(body.closed)
    }

    @Test
    fun `GS-MOB-001-6 response arriving after cancellation is closed`() = runTest {
        val calls = FakeCalls { }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))
        val job = launch { repository.fetchProducts() }
        runCurrent()
        val call = calls.created.single()
        job.cancel()
        advanceUntilIdle()
        val body = TrackingBody("""{"hits":[]}""")

        call.respond(body)
        runCurrent()

        assertTrue(body.closed)
        assertTrue(job.isCancelled)
    }

    @Test
    fun `GS-MOB-002-9 existing mapper policy excludes unusable records`() = runTest {
        val json = """
            {"hits":[{},{"objectID":"catalogue-42","title":" Speed Leggings ","price":0,"labels":null}]}
        """.trimIndent()
        val calls = FakeCalls { it.respond(TrackingBody(json)) }
        val repository = RemoteProductRepository(calls, dispatcher = StandardTestDispatcher(testScheduler))

        val product = repository.fetchProducts().single()

        assertEquals("catalogue-42", product.id)
        assertEquals("Speed Leggings", product.title)
        assertEquals(0L, product.rawPrice)
        assertEquals(emptyList<String>(), product.labels)
    }

    private fun assertPropagated(expected: IOException, actual: IOException) {
        assertEquals(expected.message, actual.message)
        // Coroutine stack-trace recovery may copy the exception, retaining it as the cause.
        assertTrue(generateSequence<Throwable>(actual) { it.cause }.any { it === expected })
    }

    /** Fails the test if the operation succeeds or throws a different kind of error. */
    private suspend inline fun <reified T : Throwable> expectFailure(operation: () -> Unit): T {
        try {
            operation()
        } catch (failure: Throwable) {
            if (failure is T) return failure
            throw failure
        }
        throw AssertionError("Expected ${T::class.java.simpleName}")
    }

    /** Records requests and gives each fake call the response behaviour chosen by the test. */
    private class FakeCalls(private val onEnqueue: (FakeCall) -> Unit) : Call.Factory {
        val created = mutableListOf<FakeCall>()
        override fun newCall(request: Request): Call =
            FakeCall(request, onEnqueue).also { created += it }
    }

    /** Implements only the callback boundary; no socket or real HTTP client is created. */
    private class FakeCall(
        private val request: Request,
        private val onEnqueue: (FakeCall) -> Unit
    ) : Call {
        private lateinit var callback: Callback
        private var executed = false
        private var cancelled = false
        override fun request() = request
        override fun enqueue(responseCallback: Callback) {
            // Save the callback so the test can deliver an outcome now or after cancellation.
            executed = true
            callback = responseCallback
            onEnqueue(this)
        }
        override fun execute(): Response = error("Blocking execute must not be used")
        override fun cancel() { cancelled = true }
        override fun isExecuted() = executed
        override fun isCanceled() = cancelled
        override fun timeout() = Timeout()
        override fun clone(): Call = FakeCall(request, onEnqueue)

        fun fail(failure: IOException) { callback.onFailure(this, failure) }
        fun respond(body: ResponseBody, code: Int = 200) {
            // Construct an in-memory HTTP response; no connection is opened.
            callback.onResponse(
                this,
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(code).message("Controlled response").body(body).build()
            )
        }
    }

    /**
     * An in-memory response body that records whether the repository closes its stream.
     * Supplying failure makes reading throw, allowing cleanup on I/O errors to be tested.
     * This helper exists only in test code; real responses use OkHttp's response bodies.
     */
    private class TrackingBody(json: String, failure: IOException? = null) : ResponseBody() {
        var closed = false
            private set
        // Store the fixture as bytes, just as a downloaded response would be read as bytes.
        private val bytes = Buffer().writeUtf8(json)
        // ForwardingSource wraps that byte stream so we can observe reads and closure.
        private val buffered = object : ForwardingSource(bytes) {
            override fun read(sink: Buffer, byteCount: Long): Long {
                // Simulate a connection breaking while the response body is being read.
                if (failure != null) throw failure
                return super.read(sink, byteCount)
            }
            override fun close() {
                // The tests assert this flag to prove cleanup happened.
                closed = true
                super.close()
            }
        }.buffer()
        override fun contentType(): MediaType? = null
        // -1 is OkHttp's convention for an unknown body length.
        override fun contentLength() = -1L
        override fun source(): BufferedSource = buffered
    }
}
