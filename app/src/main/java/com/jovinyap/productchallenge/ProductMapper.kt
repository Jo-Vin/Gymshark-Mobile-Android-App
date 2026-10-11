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

        // Preserve the supplied order, putting the featured photo first and removing
        // duplicate URLs. URL validity and download failures remain ProductImage's job.
        val galleryImages = (listOfNotNull(dto.featuredMedia) + dto.media.orEmpty())
            .mapNotNull { media ->
                val url = media.src.nonBlank() ?: return@mapNotNull null
                ProductPhoto(url = url, alt = media.alt.nonBlank())
            }
            .distinctBy(ProductPhoto::url)
        val image = galleryImages.firstOrNull()

        return Product(
            id = id,
            title = title,
            rawPrice = price,
            colour = dto.colour.nonBlank(),
            descriptionHtml = dto.description,
            imageUrl = image?.url,
            imageAlt = image?.alt,
            labels = dto.labels.orEmpty(),
            fit = dto.fit.nonBlank(),
            variants = dto.availableSizes.orEmpty().mapNotNull { variant ->
                val size = variant.size.nonBlank() ?: return@mapNotNull null
                val variantId = variant.id?.toString() ?: variant.sku.nonBlank() ?: size
                ProductVariant(variantId, size, variant.price, variant.inStock)
            },
            productType = dto.type.nonBlank(),
            gender = dto.gender.orEmpty().joinToString("|").nonBlank(),
            galleryImages = galleryImages
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
