package com.jovinyap.productchallenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader

private const val DETAIL_IMAGE_ASPECT_RATIO = 0.82f

@Composable
fun ProductDetailsScreen(
    product: Product?,
    selectedVariantId: String?,
    onBack: () -> Unit,
    onVariantSelected: (String) -> Unit,
    imageLoader: ImageLoader
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            ProductDetailsHeader(product?.title, onBack)
            if (product == null) ProductUnavailableContent()
            else ProductDetailsContent(product, selectedVariantId, onVariantSelected, imageLoader)
        }
    }
}

@Composable
private fun ProductDetailsHeader(title: String?, onBack: () -> Unit) {
    val backDescription = stringResource(R.string.product_details_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = dimensionResource(R.dimen.catalogue_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u2190",
            modifier = Modifier
                .width(48.dp)
                .defaultMinSize(minHeight = 48.dp)
                .clickable(onClick = onBack)
                .semantics {
                    contentDescription = backDescription
                    role = Role.Button
                }
                .testTag("product_details_back"),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            textAlign = TextAlign.Start
        )
        Text(
            text = title.orEmpty(),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun ProductUnavailableContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(dimensionResource(R.dimen.catalogue_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.catalogue_spacing))
    ) {
        Text(stringResource(R.string.product_details_unavailable))
    }
}

@Composable
private fun ProductDetailsContent(
    product: Product,
    selectedVariantId: String?,
    onVariantSelected: (String) -> Unit,
    imageLoader: ImageLoader
) {
    val selectedVariant = product.variants.firstOrNull { it.id == selectedVariantId }
    val displayedPrice = selectedVariant?.rawPrice ?: if (selectedVariant == null) product.rawPrice else null
    val modelInformation = remember(product.descriptionHtml) { extractModelInformation(product.descriptionHtml) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .testTag("product_details_content"),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.catalogue_spacing))
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            productTitle = product.title,
            imageAlt = product.imageAlt,
            imageLoader = imageLoader,
            aspectRatio = DETAIL_IMAGE_ASPECT_RATIO,
            overlay = {
                modelInformation?.let { information ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .testTag("product_model_information"),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = stringResource(
                                R.string.product_model_information,
                                information.height,
                                information.size
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        )
        Column(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.catalogue_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))
        ) {
            product.labels.forEach { label -> ProductBadge(label) }
            Text(
                product.title,
                modifier = Modifier.testTag("product_details_title"),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            product.fit?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            product.colour?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                text = displayedPrice?.let(::formatGbpPrice)
                    ?: stringResource(R.string.product_price_unavailable),
                modifier = Modifier.testTag("product_details_price"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            if (product.variants.isNotEmpty()) {
                VariantSelector(product, selectedVariantId, onVariantSelected)
            }
            ProductDescriptionSection(product.descriptionHtml)
        }
    }
}

@Composable
private fun ProductBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.small
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun VariantSelector(product: Product, selectedVariantId: String?, onVariantSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.product_size_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_size_selector"),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            FlowRow(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.product_card_padding), vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))
            ) {
                productSizeOptions(product.variants).forEach { option ->
                    val variant = option.variant
                    val enabled = variant?.inStock == true
                    val selected = variant?.id == selectedVariantId
                    val availability = if (variant == null) {
                        R.string.product_size_unavailable_missing
                    } else if (enabled) {
                        R.string.product_size_available
                    } else {
                        R.string.product_size_unavailable
                    }
                    val availabilityDescription = stringResource(availability)
                    val optionModifier = Modifier
                        .defaultMinSize(
                            minWidth = dimensionResource(R.dimen.product_variant_min_width),
                            minHeight = dimensionResource(R.dimen.product_variant_touch_target)
                        )
                        .then(
                            if (variant == null) {
                                Modifier.semantics {
                                    disabled()
                                    stateDescription = availabilityDescription
                                }
                            } else {
                                Modifier.toggleable(
                                    value = selected,
                                    enabled = enabled,
                                    role = Role.RadioButton,
                                    onValueChange = { if (it) onVariantSelected(variant.id) }
                                ).semantics {
                                    stateDescription = availabilityDescription
                                }
                            }
                        )
                    Surface(
                        modifier = optionModifier.testTag("product_size_option_${option.label}"),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Text(
                            option.label,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = when {
                                selected -> MaterialTheme.colorScheme.surface
                                enabled -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDescriptionSection(html: String?) {
    var expanded by remember { mutableStateOf(true) }
    val stateDescription = stringResource(
        if (expanded) R.string.product_description_expanded else R.string.product_description_collapsed
    )
    Column(Modifier.fillMaxWidth().testTag("product_description_section")) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = expanded, role = Role.Button, onValueChange = { expanded = it })
                .semantics { this.stateDescription = stateDescription }
                .padding(vertical = 16.dp)
                .testTag("product_description_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.product_description_heading), style = MaterialTheme.typography.titleMedium)
            Text(if (expanded) "\u2303" else "\u2304", style = MaterialTheme.typography.titleMedium)
        }
        if (expanded) {
            Text(
                text = annotatedProductDescription(html)
                    ?: androidx.compose.ui.text.AnnotatedString(stringResource(R.string.product_description_unavailable)),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("product_description_body")
            )
        }
    }
}
