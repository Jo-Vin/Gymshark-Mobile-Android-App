package com.jovinyap.productchallenge

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

/** Uses the reference catalogue's white background and neutral text, independent of device dark mode. */
@Composable
internal fun ProductCatalogueTheme(content: @Composable () -> Unit) {
    val paper = colorResource(R.color.catalogue_background)
    val ink = colorResource(R.color.catalogue_ink)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ink, onPrimary = paper,
            background = paper, onBackground = ink,
            surface = paper, onSurface = ink,
            onSurfaceVariant = colorResource(R.color.catalogue_secondary_text),
            outlineVariant = colorResource(R.color.catalogue_divider),
            secondaryContainer = paper, onSecondaryContainer = ink
        ),
        content = content
    )
}
