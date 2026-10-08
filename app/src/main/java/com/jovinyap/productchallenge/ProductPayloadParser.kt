package com.jovinyap.productchallenge

data class ProductDto(
    val title: String
)

class ProductPayloadParser {

    fun parse(json: String): List<ProductDto> {
        TODO("Implement JSON decoding")
    }
}