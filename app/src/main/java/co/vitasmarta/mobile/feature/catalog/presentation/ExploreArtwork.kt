package co.vitasmarta.mobile.feature.catalog.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.feature.catalog.domain.Category

private val LandscapeInk = Color(0xFF0D1723)

@Composable
internal fun ExploreHero(onSearch: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge) {
        Box {
            Image(
                painterResource(R.drawable.explore_tayrona),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.matchParentSize()
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(LandscapeInk.copy(alpha = .62f), LandscapeInk.copy(alpha = .76f))
                    )
                )
            )
            Column(
                Modifier.fillMaxWidth().heightIn(min = 264.dp).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.native_explore_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    stringResource(R.string.native_explore_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Button(
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF4E1DE),
                        contentColor = Color(0xFF1D2B5C)
                    )
                ) {
                    Icon(Icons.Outlined.Search, null, modifier = Modifier.padding(end = 10.dp))
                    Text(stringResource(R.string.catalog_search_attractions))
                }
            }
        }
    }
}

@Composable
internal fun ExploreCategoryTile(
    category: Category,
    locale: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("catalog-category-${category.slug}")
            .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow
    ) {
        Column {
            CategoryArtwork(category.slug, Modifier.fillMaxWidth().height(108.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.displayName(locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryArtwork(slug: String, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val photograph = when (slug) {
        "playa" -> R.drawable.explore_bay
        "naturaleza" -> R.drawable.explore_tayrona
        else -> null
    }
    if (photograph != null) {
        Image(
            painterResource(photograph),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterStart,
            modifier = modifier
        )
    } else {
        val heritage = slug == "cultura" || slug == "gastronomico"
        val background = if (heritage) colors.primaryContainer else colors.secondaryContainer
        val foreground = if (heritage) colors.onPrimaryContainer else colors.onSecondaryContainer
        val symbol = when (slug) {
            "aventura" -> Icons.Outlined.Terrain
            "cultura" -> Icons.Outlined.AccountBalance
            "ecoturismo" -> Icons.Outlined.Park
            "gastronomico" -> Icons.Outlined.Restaurant
            else -> Icons.Outlined.Sell
        }
        Box(
            modifier.background(background).clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    foreground.copy(alpha = .06f),
                    size.width * .42f,
                    Offset(0f, size.height)
                )
                drawCircle(foreground.copy(alpha = .06f), size.width * .28f, Offset(size.width, 0f))
            }
            Icon(
                symbol,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
internal fun ExploreImageCredits() {
    val uriHandler = LocalUriHandler.current
    var unavailable by remember { mutableStateOf(false) }
    fun open(url: String) {
        unavailable = !runCatching { uriHandler.openUri(url) }.isSuccess
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(R.string.explore_image_credits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                open(
                    "https://commons.wikimedia.org/wiki/File:2019_Santa_Marta_-_" +
                        "Ensenadas_en_el_Parque_nacional_natural_Tayrona.jpg"
                )
            },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) { Text(stringResource(R.string.explore_tayrona_source)) }
        TextButton(
            onClick = { open("https://creativecommons.org/licenses/by-sa/4.0/") },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) { Text(stringResource(R.string.explore_photo_license)) }
        TextButton(
            onClick = { open("https://commons.wikimedia.org/wiki/File:Santa_Marta_Bahia.jpg") },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) { Text(stringResource(R.string.explore_bay_source)) }
        if (unavailable) Text(stringResource(R.string.catalog_open_link_error))
    }
}
