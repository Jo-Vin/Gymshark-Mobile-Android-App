package com.jovinyap.productchallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.ImageLoader

/** Displays all available product photos while retaining ProductImage's loading states. */
@Composable
fun ProductImageGallery(
    product: Product,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
    overlay: (@Composable BoxScope.() -> Unit)? = null
) {
    key(product.id) {
        val photos = remember(product.galleryImages, product.imageUrl, product.imageAlt) {
            product.galleryImages.ifEmpty {
                listOfNotNull(product.imageUrl?.let { ProductPhoto(it, product.imageAlt) })
            }
        }

        if (photos.size <= 1) {
            val photo = photos.firstOrNull()
            ProductImage(
                imageUrl = photo?.url,
                productTitle = product.title,
                imageAlt = photo?.alt,
                imageLoader = imageLoader,
                modifier = modifier,
                aspectRatio = aspectRatio,
                contentScale = ContentScale.Fit,
                overlay = overlay
            )
        } else {
            val pagerState = rememberPagerState(pageCount = { photos.size })
            val page = pagerState.currentPage.coerceIn(0, photos.lastIndex)
            val positionDescription = stringResource(
                R.string.product_gallery_position,
                page + 1,
                photos.size
            )

            HorizontalPager(
                state = pagerState,
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .testTag("product_gallery_pager")
                    .semantics { stateDescription = positionDescription },
                key = { index -> photos[index].url }
            ) { index ->
                val photo = photos[index]
                Box(Modifier.fillMaxSize()) {
                    ProductImage(
                        imageUrl = photo.url,
                        productTitle = product.title,
                        imageAlt = photo.alt ?: stringResource(
                            R.string.product_gallery_image_description,
                            product.title,
                            index + 1,
                            photos.size
                        ),
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxWidth(),
                        aspectRatio = aspectRatio,
                        contentScale = ContentScale.Fit,
                        overlay = {
                            overlay?.invoke(this)
                            GalleryPositionIndicator(
                                selectedPage = page,
                                count = photos.size
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.GalleryPositionIndicator(selectedPage: Int, count: Int) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
            .testTag("product_gallery_position"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .width(if (it == selectedPage) 28.dp else 6.dp)
                    .height(4.dp)
                    .background(
                        color = if (it == selectedPage) Color.White else Color.White.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .testTag("product_gallery_indicator_$it")
            )
        }
    }
}
