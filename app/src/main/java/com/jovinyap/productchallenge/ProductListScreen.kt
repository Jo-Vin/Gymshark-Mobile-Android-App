package com.jovinyap.productchallenge

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val largeFontScrollState = rememberScrollState()
    val content = state as? ProductListUiState.Content
    if (content?.isDetailsVisible == true) {
        BackHandler { viewModel.closeDetails() }
        ProductDetailsScreen(
            product = content.products.firstOrNull { it.id == content.selectedProductId },
            selectedVariantId = content.selectedVariantId,
            onBack = viewModel::closeDetails,
            onVariantSelected = { viewModel.selectVariant(content.selectedProductId.orEmpty(), it) },
            imageLoader = imageLoader,
            catalogueProducts = content.products,
            onColourSelected = viewModel::selectColourProduct
        )
    } else {
        ProductListScreen(
            uiState = state,
            onRetry = viewModel::retry,
            onProductSelected = viewModel::openDetails,
            imageLoader = imageLoader,
            gridState = gridState,
            largeFontScrollState = largeFontScrollState
        )
    }
}

/** Displays the catalogue without knowing how products are downloaded or decoded. */
@Composable
fun ProductListScreen(
    uiState: ProductListUiState,
    onRetry: () -> Unit,
    onProductSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalContext.current),
    gridState: LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
    largeFontScrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {
    val padding = dimensionResource(R.dimen.catalogue_padding)
    val spacing = dimensionResource(R.dimen.catalogue_spacing)
    val fontScale = LocalDensity.current.fontScale
    Surface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = padding, vertical = dimensionResource(R.dimen.catalogue_grid_padding_vertical)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.gymshark_logo),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterStart).size(
                        dimensionResource(R.dimen.catalogue_logo_width),
                        dimensionResource(R.dimen.catalogue_logo_height)
                    )
                )
                Text(
                    stringResource(R.string.catalogue_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
                        // Keep two catalogue columns at normal scale, but give large text a single readable column.
                        if (fontScale >= LARGE_FONT_SCALE_THRESHOLD) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(largeFontScrollState)
                                    .testTag("product_grid"),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.catalogue_row_spacing))
                                ) {
                                    uiState.products.forEach { product ->
                                        key(product.id) {
                                            ProductCard(
                                                product = product,
                                                modifier = Modifier.fillMaxWidth(),
                                                selected = product.id == uiState.selectedProductId,
                                                onProductSelected = onProductSelected,
                                                imageLoader = imageLoader,
                                                largeText = true
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize().testTag("product_grid"),
                                contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.catalogue_grid_padding_vertical)),
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.catalogue_row_spacing))
                            ) {
                                // Stable product IDs keep each image and card attached to the correct record.
                                items(uiState.products, key = { it.id }) { product ->
                                    ProductCard(
                                        product,
                                        modifier = Modifier.fillMaxWidth(),
                                        selected = product.id == uiState.selectedProductId,
                                        onProductSelected = onProductSelected,
                                        imageLoader = imageLoader,
                                        largeText = false
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
    imageLoader: ImageLoader,
    largeText: Boolean
) {
    Column(
        modifier = modifier.testTag("product_card_${product.id}")
            // Selection is conveyed through semantics, not encoded in the test tag.
            .selectable(selected = selected, role = Role.Button, onClick = { onProductSelected(product.id) }),
    ) {
        // Image loading is independent of selection: fallback cards remain selectable.
        ProductImage(
            imageUrl = product.imageUrl,
            productTitle = product.title,
            imageAlt = product.imageAlt,
            imageLoader = imageLoader,
            aspectRatio = 1692f / 2018f, // Width divided by height.
            contentScale = ContentScale.Fit,
            overlay = {
                FlowRow(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(dimensionResource(R.dimen.product_card_padding)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))
                ) {
                    product.labels.forEach { label ->
                        Surface(color = MaterialTheme.colorScheme.surface) {
                            Text(
                                text = label.replace('-', ' ').trim(),
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
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.product_card_padding),
                vertical = 4.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleSmall,
                minLines = if (largeText) 1 else 2,
                maxLines = if (largeText) Int.MAX_VALUE else 2,
                overflow = if (largeText) TextOverflow.Clip else TextOverflow.Ellipsis
            )

            // Show fit, then colour, without reserving empty attribute rows.
            listOfNotNull(product.fit, product.colour)
                .filter { it.isNotBlank() }
                .forEach { attribute ->
                    Text(
                        text = attribute,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (largeText) Int.MAX_VALUE else 1,
                        overflow = if (largeText) {
                            TextOverflow.Clip
                        } else {
                            TextOverflow.Ellipsis
                        }
                    )
                }

            Text(
                text = formatGbpPrice(product.rawPrice),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
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
