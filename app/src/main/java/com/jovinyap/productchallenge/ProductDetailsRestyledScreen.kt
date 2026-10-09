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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import java.util.Locale

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
    var descriptionExpanded by remember(product.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.catalogue_spacing))
    ) {
        ProductImage(
            imageUrl = product.imageUrl,
            productTitle = product.title,
            imageAlt = product.imageAlt,
            imageLoader = imageLoader,
            aspectRatio = DETAIL_IMAGE_ASPECT_RATIO
        )
        Column(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.catalogue_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))
        ) {
            product.labels.forEach { label -> ProductBadge(label) }
            Text(product.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            product.fit?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            product.colour?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                text = displayedPrice?.let(::formatGbpPrice)
                    ?: stringResource(R.string.product_price_unavailable),
                modifier = Modifier.testTag("product_details_price"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            if (product.variants.isNotEmpty()) {
                Text(stringResource(R.string.product_size_label), style = MaterialTheme.typography.titleMedium)
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.product_label_spacing))
    ) {
        product.variants.forEach { variant ->
            val enabled = variant.inStock != false
            val selected = variant.id == selectedVariantId
            Column(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = dimensionResource(R.dimen.product_variant_min_width),
                        minHeight = dimensionResource(R.dimen.product_variant_touch_target)
                    )
                    .toggleable(
                        value = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onValueChange = { if (it) onVariantSelected(variant.id) }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        variant.size.uppercase(Locale.ROOT),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!enabled) Text(stringResource(R.string.product_variant_unavailable), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ProductDescriptionSection(html: String?) {
    var expanded by remember { mutableStateOf(false) }
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
