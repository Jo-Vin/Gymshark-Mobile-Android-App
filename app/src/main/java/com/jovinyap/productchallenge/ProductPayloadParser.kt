package com.jovinyap.productchallenge

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class ProductDto(
    val id: Long? = null,
    val objectID: String? = null,
    val sku: String? = null,
    val handle: String? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val fit: String? = null,
    val colour: String? = null,
    val price: Long? = null,
    val inStock: Boolean? = null,
    val labels: List<String>? = null,
    val featuredMedia: ProductMediaDto? = null,
    val media: List<ProductMediaDto>? = null,
    val availableSizes: List<ProductVariantDto>? = null
)

@Serializable
data class ProductMediaDto(
    val src: String? = null,
    val alt: String? = null
)

@Serializable
data class ProductVariantDto(
    val id: Long? = null,
    val size: String? = null,
    val price: Long? = null,
    val inStock: Boolean? = null,
    val inventoryQuantity: Int? = null,
    val sku: String? = null
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