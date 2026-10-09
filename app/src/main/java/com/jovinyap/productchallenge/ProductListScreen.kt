package com.jovinyap.productchallenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.SingletonImageLoader

/** Connects lifecycle-aware ViewModel state to a screen that only renders state and emits actions. */
@Composable
fun ProductListRoute(
    viewModel: ProductListViewModel,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalContext.current)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductListScreen(
        uiState = state,
        onRetry = viewModel::retry,
        onProductSelected = viewModel::selectProduct,
        imageLoader = imageLoader
    )
}

/** Displays the catalogue without knowing how products are downloaded or decoded. */
@Composable
fun ProductListScreen(
    uiState: ProductListUiState,
    onRetry: () -> Unit,
    onProductSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalContext.current)
) {
    val padding = dimensionResource(R.dimen.catalogue_padding)
    val spacing = dimensionResource(R.dimen.catalogue_spacing)
    val fontScale = LocalDensity.current.fontScale
    Surface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(padding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.gymshark_logo),
                    contentDescription = null,
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.catalogue_logo_width),
                        dimensionResource(R.dimen.catalogue_logo_height)
                    )
                )
                Text(stringResource(R.string.catalogue_title), style = MaterialTheme.typography.headlineSmall)
            }
            HorizontalDivider()
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (uiState) {
                    ProductListUiState.Loading -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        CircularProgressIndicator(Modifier.testTag("catalogue_progress"))
                        Text(stringResource(R.string.catalogue_loading))
                    }
                    ProductListUiState.Empty -> Text(
                        stringResource(R.string.catalogue_empty),
                        modifier = Modifier.padding(padding)
                    )
                    ProductListUiState.Error -> Column(
                        modifier = Modifier.padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        Text(stringResource(R.string.catalogue_error))
                        Button(onClick = onRetry, modifier = Modifier.testTag("catalogue_retry")) {
                            Text(stringResource(R.string.catalogue_retry))
                        }
                    }
                    is ProductListUiState.Content -> {
                        // Keep adaptive columns at normal scale, but give large text a single readable column.
                        if (fontScale >= LARGE_FONT_SCALE_THRESHOLD) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .testTag("product_grid"),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(spacing)
                                ) {
                                    uiState.products.forEach { product ->
                                        ProductCard(
                                            product,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = padding),
                                            selected = product.id == uiState.selectedProductId,
                                            onProductSelected = onProductSelected,
                                            imageLoader = imageLoader
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(dimensionResource(R.dimen.product_card_min_width)),
                                modifier = Modifier.fillMaxSize().testTag("product_grid"),
                                contentPadding = PaddingValues(padding),
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                                verticalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                // Stable product IDs keep each image and card attached to the correct record.
                                items(uiState.products, key = { it.id }) { product ->
                                    ProductCard(
                                        product,
                                        modifier = Modifier.fillMaxWidth(),
                                        selected = product.id == uiState.selectedProductId,
                                        onProductSelected = onProductSelected,
                                        imageLoader = imageLoader
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    selected: Boolean,
    onProductSelected: (String) -> Unit,
    imageLoader: ImageLoader
) {
    Card(
        modifier = modifier.testTag("product_card_${product.id}")
            // Selection is conveyed through semantics, not encoded in the test tag.
            .selectable(selected = selected, role = Role.Button, onClick = { onProductSelected(product.id) }),
        border = if (selected) BorderStroke(
            dimensionResource(R.dimen.product_card_selected_border), MaterialTheme.colorScheme.primary
        ) else null
    ) {
        // Image loading is independent of selection: fallback cards remain selectable.
        ProductImage(
            imageUrl = product.imageUrl,
            productTitle = product.title,
            imageAlt = product.imageAlt,
            imageLoader = imageLoader,
            overlay = {
                FlowRow(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(dimensionResource(R.dimen.product_card_padding)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))
                ) {
                    product.labels.forEach { label ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(R.dimen.product_label_padding_horizontal),
                                    vertical = dimensionResource(R.dimen.product_label_padding_vertical)
                                )
                            )
                        }
                    }
                }
            }
        )
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.product_card_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))
        ) {
            Text(product.title, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = product.colour.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.fit ?: " ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatGbpPrice(product.rawPrice),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private const val LARGE_FONT_SCALE_THRESHOLD = 1.5f

@Preview(showBackground = true)
@Composable
private fun EmptyCataloguePreview() {
    MaterialTheme { ProductListScreen(ProductListUiState.Empty, onRetry = {}, onProductSelected = {}) }
}
