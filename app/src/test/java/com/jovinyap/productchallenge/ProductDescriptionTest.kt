package com.jovinyap.productchallenge

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProductDescriptionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    // GS-MOB-014: readable product description content is produced without executable markup.
    fun readableDescriptionPreservesParagraphsEntitiesAndUnicodeWithoutScripts() {
        val result = requireNotNull(readableProductDescription(
            "<p>Comfort &amp; support 😀</p><p><strong>Move freely</strong><br>every day</p>" +
                "<script>alert('not product copy')</script><style>.x{display:none}</style>"
        ))

        assertEquals("Comfort & support 😀\n\nMove freely\nevery day", result)
        assertFalse(result.contains("alert"))
        assertFalse(result.contains("display:none"))
    }

    @Test
    // GS-MOB-014: model height and size are extracted only when both are present.
    fun modelInformationIsExtractedForImageOverlay() {
        assertEquals(
            ModelInformation("5'7\"", "XS"),
            extractModelInformation("<p>- Model is 5'7\" and wears a size XS</p>")
        )
        assertNull(extractModelInformation("<p>- Model is not supplied</p>"))
    }

    @Test
    // GS-MOB-014: absent or irrelevant description content has an explicit empty result.
    fun missingOrTranslatorOnlyDescriptionIsUnavailable() {
        assertTrue(readableProductDescription(null).isNullOrBlank())
        assertTrue(
            readableProductDescription("<div id=\"gtx-trans\"><div>translated noise</div></div>")
                .isNullOrBlank()
        )
    }

    @Test
    fun suppliedYoutubeAnchorRetainsUrlTextRangeAndUnderline() {
        val url = "https://www.youtube.com/watch?v=Jkwz6my9PV4"
        val anchorText = "Apex Women's Product Review"
        val result = requireNotNull(annotatedProductDescription(
            "<p><a href=\"$url\">$anchorText</a></p>"
        ))

        val link = result.getLinkAnnotations(0, result.length).single()
        assertEquals(url, (link.item as LinkAnnotation.Url).url)
        assertEquals(anchorText, result.text.substring(link.start, link.end))
        assertEquals(
            TextDecoration.Underline,
            (link.item as LinkAnnotation.Url).styles?.style?.textDecoration
        )
    }

    @Test
    fun multipleLinksRetainIndependentDestinations() {
        val result = requireNotNull(annotatedProductDescription(
            "<p><a href=\"https://example.com/one\">One</a> and " +
                "<a href=\"https://example.com/two\">Two</a></p>"
        ))

        val links = result.getLinkAnnotations(0, result.length)
        assertEquals(2, links.size)
        assertEquals("https://example.com/one", (links[0].item as LinkAnnotation.Url).url)
        assertEquals("One", result.text.substring(links[0].start, links[0].end))
        assertEquals("https://example.com/two", (links[1].item as LinkAnnotation.Url).url)
        assertEquals("Two", result.text.substring(links[1].start, links[1].end))
    }

    @Test
    fun whitespaceCleanupAndUnicodePreserveLinkRange() {
        val anchorText = "日本語 🏋️"
        val result = requireNotNull(annotatedProductDescription(
            "<p>😀  <a href=\"https://example.com/unicode\">$anchorText</a> \u00a0 text</p>"
        ))

        val link = result.getLinkAnnotations(0, result.length).single()
        assertEquals(anchorText, result.text.substring(link.start, link.end))
        assertEquals(result.text.indexOf(anchorText), link.start)
        assertEquals(link.end, link.start + anchorText.length)
    }

    @Test
    fun unsupportedDestinationsRemainReadableWithoutLinks() {
        val result = requireNotNull(annotatedProductDescription(
            "<p><a href=\"http://example.com\">HTTP</a> " +
                "<a href=\"javascript:alert(1)\">Script</a> " +
                "<a href=\"https://\">Malformed</a></p>"
        ))

        assertTrue(result.text.contains("HTTP"))
        assertTrue(result.text.contains("Script"))
        assertTrue(result.text.contains("Malformed"))
        assertTrue(result.getLinkAnnotations(0, result.length).isEmpty())
    }

    @Test
    fun clickingLinkInvokesInjectedUrlHandler() {
        var openedUrl: String? = null
        compose.setContent {
            ProductCatalogueTheme {
                ProductDescriptionSection(
                    "<p><a href=\"https://example.com/review\">Open review</a></p>",
                    externalUrlHandler = ExternalUrlHandler { url ->
                        openedUrl = url
                        true
                    }
                )
            }
        }

        compose.onNodeWithText("Open review").performClick()

        compose.runOnIdle {
            assertEquals("https://example.com/review", openedUrl)
        }
    }

    @Test
    fun unavailableUrlHandlerShowsResourceBackedFeedback() {
        compose.setContent {
            ProductCatalogueTheme {
                ProductDescriptionSection(
                    "<p><a href=\"https://example.com/review\">Open review</a></p>",
                    externalUrlHandler = ExternalUrlHandler { false }
                )
            }
        }

        compose.onNodeWithText("Open review").performClick()

        compose.onNodeWithText(
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .getString(R.string.product_description_link_unavailable)
        ).assertIsDisplayed()
    }
}
