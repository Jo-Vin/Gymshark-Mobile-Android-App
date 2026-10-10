package com.jovinyap.productchallenge

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
}
