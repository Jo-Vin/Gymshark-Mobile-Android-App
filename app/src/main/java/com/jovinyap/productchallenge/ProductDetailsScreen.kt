package com.jovinyap.productchallenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader

@Composable
private fun ProductDetailsScreenLegacy(
    product: Product?,
    selectedVariantId: String?,
    onBack: () -> Unit,
    onVariantSelected: (String) -> Unit,
    imageLoader: ImageLoader
) {
    if (product == null) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(dimensionResource(R.dimen.catalogue_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.catalogue_spacing))
            ) {
                Button(onClick = onBack, modifier = Modifier.testTag("product_details_back")) {
                    Text(stringResource(R.string.product_details_back))
                }
                Text(stringResource(R.string.product_details_unavailable))
            }
        }
        return
    }

    val selectedVariant = product.variants.firstOrNull { it.id == selectedVariantId }
    val displayedPrice = selectedVariant?.rawPrice ?: if (selectedVariant == null) product.rawPrice else null

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.catalogue_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.catalogue_spacing))
        ) {
            Button(onClick = onBack, modifier = Modifier.testTag("product_details_back")) {
                Text(stringResource(R.string.product_details_back))
            }
            ProductImage(
                imageUrl = product.imageUrl,
                productTitle = product.title,
                imageAlt = product.imageAlt,
                imageLoader = imageLoader
            )
            Text(product.title, style = MaterialTheme.typography.headlineSmall)
            product.colour?.let { Text(it) }
            product.fit?.let { Text(it) }
            Text(
                text = displayedPrice?.let(::formatGbpPrice)
                    ?: stringResource(R.string.product_price_unavailable),
                modifier = Modifier.testTag("product_details_price")
            )
            product.labels.forEach { label -> Text(label) }
            if (product.variants.isNotEmpty()) {
                Text(stringResource(R.string.product_size_label), style = MaterialTheme.typography.titleMedium)
                product.variants.forEach { variant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = variant.id == selectedVariantId,
                                role = Role.RadioButton,
                                enabled = variant.inStock != false,
                                onClick = { onVariantSelected(variant.id) }
                            )
                            .padding(dimensionResource(R.dimen.product_card_padding)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(variant.size)
                        if (variant.inStock == false) {
                            Text(stringResource(R.string.product_variant_unavailable))
                        }
                    }
                }
            }
            Text(stringResource(R.string.product_description_heading), style = MaterialTheme.typography.titleMedium)
            val description = annotatedProductDescription(product.descriptionHtml)
            if (description == null) {
                Text(stringResource(R.string.product_description_unavailable))
            } else {
                Text(description)
            }
        }
    }
}
