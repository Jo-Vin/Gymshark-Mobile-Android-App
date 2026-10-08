package com.jovinyap.productchallenge

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class ProductDto(
    val title: String
)

@Serializable
data class ProductResponse(
    val hits: List<ProductDto>
)

class ProductPayloadParser {

    private val decoder = Json {
        ignoreUnknownKeys = true
    }

    fun parse(json: String): List<ProductDto> {
        val response = decoder.decodeFromString<ProductResponse>(json)
        return response.hits
    }
}