package com.jovinyap.productchallenge

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Raw API record: optional fields are preserved for the mapper to validate. DTO means data transfer object. */
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

// hits is required: a missing catalogue is different from a valid empty catalogue.
@Serializable
data class ProductResponse(
    val hits: List<ProductDto>
)

/** Decodes JSON into response records without applying display rules or downloading images. */
class ProductPayloadParser {

    private val decoder = Json {
        // Extra API fields are allowed, but malformed JSON and incorrect modelled types still fail.
        ignoreUnknownKeys = true
    }

    fun parse(json: String): List<ProductDto> {
        val response = decoder.decodeFromString<ProductResponse>(json)
        return response.hits
    }
}