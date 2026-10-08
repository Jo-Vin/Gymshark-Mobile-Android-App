package com.jovinyap.productchallenge

class ProductMapper {

    fun map(dto: ProductDto): Product? {
        val id = dto.objectID.nonBlank()
            ?: dto.id?.toString()
            ?: return null

        val title = dto.title.nonBlank()
            ?: return null

        val price = dto.price
            ?: return null

        if (price < 0L) return null

        val featuredImage = dto.featuredMedia?.takeIf {
            !it.src.isNullOrBlank()
        }

        val image = featuredImage
            ?: dto.media.orEmpty().firstOrNull {
                !it.src.isNullOrBlank()
            }

        return Product(
            id = id,
            title = title,
            rawPrice = price,
            colour = dto.colour.nonBlank(),
            descriptionHtml = dto.description,
            imageUrl = image?.src.nonBlank(),
            imageAlt = image?.alt.nonBlank(),
            labels = dto.labels.orEmpty()
        )
    }

    fun mapAll(dtos: List<ProductDto>): List<Product> {
        return dtos.mapNotNull { dto ->
            map(dto)
        }
    }

    private fun String?.nonBlank(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }
}