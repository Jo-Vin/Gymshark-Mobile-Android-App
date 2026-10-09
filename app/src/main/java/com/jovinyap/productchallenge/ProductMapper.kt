package com.jovinyap.productchallenge

/** Converts decoded response records into products that the UI can use. */
class ProductMapper {

    /** Returns null when the record lacks a usable identity, title or non-negative price. */
    fun map(dto: ProductDto): Product? {
        // Prefer the catalogue identity, with the numeric ID as a fallback.
        val id = dto.objectID.nonBlank()
            ?: dto.id?.toString()
            ?: return null

        val title = dto.title.nonBlank()
            ?: return null

        val price = dto.price
            ?: return null

        if (price < 0L) return null

        // Use the featured image first, then the first gallery entry with a non-blank URL.
        // URL validity and download failures are handled by ProductImage, not by this mapper.
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

    // Exclude only records that fail the above validation; do not invent missing product data.
    fun mapAll(dtos: List<ProductDto>): List<Product> {
        return dtos.mapNotNull { dto ->
            map(dto)
        }
    }

    private fun String?.nonBlank(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }
}