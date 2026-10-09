package com.jovinyap.productchallenge

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import java.net.URI

/**
 * Displays a product photo in a stable square, without consuming card clicks.
 * The caller supplies mapped product data; request state belongs to this component.
 */
@Composable
fun ProductImage(
    imageUrl: String?,
    productTitle: String,
    modifier: Modifier = Modifier,
    imageAlt: String? = null,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalContext.current),
    aspectRatio: Float = 1f,
    contentScale: ContentScale = ContentScale.Fit,
    overlay: @Composable (BoxScope.() -> Unit)? = null
) {
    val url = remember(imageUrl) { supportedImageUrl(imageUrl) }
    // Recreate request state when a recycled card receives a different image.
    key(url, imageLoader) {
        var state by remember {
            mutableStateOf(if (url == null) ImageState.Unavailable else ImageState.Loading)
        }
        val description = imageAlt?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.product_image_description, productTitle)
        // Every state uses this same square, preventing card layout jumps as requests complete.
        Box(
            modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio).testTag("product_image_area"),
            contentAlignment = Alignment.Center
        ) {
            // Coil owns loading, sizing and caching; callbacks only update this component's UI state.
            if (url != null) {
                AsyncImage(
                    model = url,
                    imageLoader = imageLoader,
                    contentDescription = if (state == ImageState.Success) description else null,
                    modifier = Modifier.fillMaxSize().testTag("product_photo"),
                    contentScale = contentScale,
                    onLoading = { state = ImageState.Loading },
                    onSuccess = { state = ImageState.Success },
                    onError = { state = ImageState.Unavailable }
                )
            }
            when (state) {
                ImageState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.testTag("product_image_progress")
                )
                ImageState.Unavailable -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.gymshark_logo),
                        // The nearby unavailable text explains the state, so the logo is decorative.
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).testTag("product_image_logo")
                    )
                    Text(stringResource(R.string.image_unavailable))
                }
                ImageState.Success -> Unit
            }
            overlay?.invoke(this)
        }
    }
}

private enum class ImageState { Loading, Success, Unavailable }

/** Accepts absolute HTTPS image references; Coil handles download and decoding errors. */
internal fun supportedImageUrl(value: String?): String? {
    val url = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uri = try {
        URI(url)
    } catch (_: java.net.URISyntaxException) {
        return null
    }
    // Only HTTPS is supported; do not start requests that rely on cleartext HTTP traffic.
    return url.takeIf {
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port in 1..65535)
    }
}
