package com.jovinyap.productchallenge

import org.junit.Test
import org.junit.Assert.assertEquals

class ProductPayloadParserTest {

    @Test
    fun `parsing preserves the first product title`() {
        // Arrange: get the json data from a local copy of the file
        val fixture = requireNotNull(
            javaClass.getResource("/algolia-example-payload.json")
        )
        val json = fixture.readText()
        val parser = ProductPayloadParser()

        // Act: Parse the json
        val products = parser.parse(json)

        // Assert: The Title is what we are expecting
        assertEquals("Speed Leggings", products.first().title)
    }
}