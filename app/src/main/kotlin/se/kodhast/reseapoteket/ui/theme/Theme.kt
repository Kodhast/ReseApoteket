package se.kodhast.reseapoteket.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import se.kodhast.ui.components.AppBrandStyle
import se.kodhast.ui.components.LocalAppBrandStyle
import se.kodhast.ui.theme.AppShapes
import se.kodhast.ui.theme.AppTheme

/** Appens former: kort 28 dp; övriga (rader 18 dp, pills helt rundade …) är android-ui:s standard. */
private val ReseApoteketShapes = AppShapes(cardRadius = 28.dp)

/** Kodhäst-igenkänningen (NFR-22): prickar före sektionsrubriker utan ikonruta, prickar som laddning och märket i tomma lägen. */
private val ReseApoteketBrandStyle = AppBrandStyle(sectionDots = true, dotLoading = true, emptyStateMark = true)

/**
 * Appens tema: android-ui:s `AppTheme` (Material 3 Expressive med fjädrande rörelse) med
 * [AppColors], [AppTypeScale] och appens former (skill `ui-style`). Ingen dynamic color.
 * Ljust, mörkt eller system väljs i Inställningar; `MainActivity` räknar ut [darkTheme]. Kodhäst-delarna
 * slås på här, en gång för hela appen ([ReseApoteketBrandStyle]), och djupet ([AppColors.lightDepth], NFR-23).
 */
@Composable
fun ReseApoteketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppTheme(
        colorScheme = if (darkTheme) AppColors.dark else AppColors.light,
        extendedColors = if (darkTheme) AppColors.darkExtended else AppColors.lightExtended,
        shapes = ReseApoteketShapes,
        typeScale = AppTypeScale,
        depth = if (darkTheme) AppColors.darkDepth else AppColors.lightDepth
    ) {
        CompositionLocalProvider(LocalAppBrandStyle provides ReseApoteketBrandStyle, content = content)
    }
}
