package com.jovinyap.productchallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProductPriceFormatterTest {
    // These cases extend the existing GS-MOB-009 price tests; they prove formatting only.
    @Test fun `GS-MOB-009-3 one thousand pence displays ten pounds`() {
        assertEquals("£10.00", formatGbpPrice(1000L))
    }

    @Test fun `GS-MOB-009-4 sixty five pence remains a fractional pound`() {
        assertEquals("£0.65", formatGbpPrice(65L))
    }

    @Test fun `GS-MOB-009-5 forty five pence remains a fractional pound`() {
        assertEquals("£0.45", formatGbpPrice(45L))
    }

    @Test fun `GS-MOB-009-6 zero retains two decimal places`() {
        assertEquals("£0.00", formatGbpPrice(0L))
    }

    @Test fun `GS-MOB-009-7 large amounts retain every penny`() {
        assertEquals("£92,233,720,368,547,758.07", formatGbpPrice(Long.MAX_VALUE))
    }

    @Test fun `GS-MOB-009-8 negative amounts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { formatGbpPrice(-1L) }
    }
}
