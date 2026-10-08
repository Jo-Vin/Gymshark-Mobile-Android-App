package com.jovinyap.productchallenge

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.asImage
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

/** Component evidence only; card integration and device accessibility require further checks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProductImageTest {
    @get:Rule val compose = createComposeRule()
    private val outcome = CompletableDeferred<Throwable?>()
    private val requested = CompletableDeferred<Unit>()
    // A suspended interceptor controls loading and never reaches network or decoders.
    private val loader = ImageLoader.Builder(RuntimeEnvironment.getApplication())
        .components {
            add(Interceptor { chain ->
                requested.complete(Unit)
                val failure = outcome.await()
                if (failure == null) SuccessResult(ColorDrawable(Color.BLUE).asImage(), chain.request)
                else ErrorResult(null, chain.request, failure)
            })
        }.build()
    private var clicks = 0
    private val unavailable get() = RuntimeEnvironment.getApplication().getString(R.string.image_unavailable)

    @After fun closeLoader() { loader.shutdown() }

    private fun show(url: String?, alt: String? = null) {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(200.dp).testTag("test_card").clickable { clicks++ }) {
                    ProductImage(url, "Speed Leggings", imageAlt = alt, imageLoader = loader)
                }
            }
        }
    }

    private fun assertFallback() {
        compose.onNodeWithText(unavailable, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("product_image_logo", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("product_image_progress", useUnmergedTree = true).assertDoesNotExist()
    }

    private fun assertLoading() {
        compose.waitUntil(5_000) { requested.isCompleted }
        compose.onNodeWithTag("product_image_progress", useUnmergedTree = true)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate
            ))
        compose.onNodeWithText(unavailable, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun complete(failure: Throwable? = null) {
        compose.runOnIdle { outcome.complete(failure) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("product_image_progress", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun bounds(tag: String) =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test fun `GS-MOB-007-3 null URL shows local fallback without a request`() {
        show(null)
        assertFallback()
        assertFalse(requested.isCompleted)
        compose.onNodeWithTag("test_card").performClick()
        assertEquals(1, clicks)
    }

    @Test fun `GS-MOB-007-4 blank URL shows local fallback without a request`() {
        show("  ")
        assertFallback()
        assertFalse(requested.isCompleted)
    }

    @Test fun `GS-MOB-008-1 invalid URL shows fallback without a request`() {
        show("not-a-valid-url")
        assertFallback()
        assertFalse(requested.isCompleted)
        listOf("https://", "file:///photo.png", "https://example.com/a b", "https://example.com:99999/a")
            .forEach { assertNull(supportedImageUrl(it)) }
    }

    @Test fun `GS-MOB-008-4 HTTP URL shows fallback without starting a request`() {
        show("http://example.com/photo.png")
        assertFallback()
        assertFalse(requested.isCompleted)
        compose.onNodeWithTag("test_card").performClick()
        assertEquals(1, clicks)
    }

    @Test fun `GS-MOB-006-1 loading shows centred indeterminate progress`() {
        show("https://example.com/photo.png")
        assertLoading()
        assertEquals(bounds("product_image_area").center.x, bounds("product_image_progress").center.x, 1f)
        assertEquals(bounds("product_image_area").center.y, bounds("product_image_progress").center.y, 1f)
        compose.onNodeWithTag("test_card").performClick()
        assertEquals(1, clicks)
    }

    @Test fun `GS-MOB-006-2 success displays photo and preserves dimensions`() {
        show("https://example.com/photo.png")
        assertLoading()
        val before = bounds("product_image_area")
        complete()
        compose.onNodeWithContentDescription("Photo of Speed Leggings", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(unavailable, useUnmergedTree = true).assertDoesNotExist()
        assertEquals(before, bounds("product_image_area"))
        compose.onNodeWithTag("test_card").performClick()
        assertEquals(1, clicks)
    }

    @Test fun `GS-MOB-008-2 download failure removes spinner and preserves dimensions`() {
        show("https://example.com/photo.png")
        assertLoading()
        val before = bounds("product_image_area")
        complete(IOException("Controlled download failure"))
        assertFallback()
        assertEquals(before, bounds("product_image_area"))
        compose.onNodeWithTag("test_card").performClick()
        assertEquals(1, clicks)
    }

    @Test fun `GS-MOB-008-3 decoding failure uses the same fallback`() {
        show("https://example.com/photo.png")
        assertLoading()
        complete(IllegalArgumentException("Controlled decoding failure"))
        assertFallback()
    }

    @Test fun `GS-MOB-024-1 supplied image description is exposed after success`() {
        show("https://example.com/photo.png", "Navy leggings viewed from the front")
        assertLoading()
        complete()
        compose.onNodeWithContentDescription("Navy leggings viewed from the front", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test fun `GS-MOB-024-2 fallback logo is decorative beside explanatory text`() {
        show(null)
        assertFallback()
        compose.onNodeWithTag("product_image_logo", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test fun `GS-MOB-025-1 fallback text uses the existing string resource`() {
        show("")
        compose.onNodeWithText(unavailable, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun `GS-MOB-006-3 changing URL clears previous success`() {
        val url = mutableStateOf<String?>("https://example.com/photo.png")
        compose.setContent {
            MaterialTheme {
                ProductImage(url.value, "Speed Leggings", Modifier.width(200.dp), imageLoader = loader)
            }
        }
        assertLoading()
        complete()
        compose.onNodeWithContentDescription("Photo of Speed Leggings").assertIsDisplayed()
        compose.runOnIdle { url.value = null }
        assertFallback()
        compose.onNodeWithContentDescription("Photo of Speed Leggings").assertDoesNotExist()
    }
}
