package se.kodhast.reseapoteket.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.kodhast.reseapoteket.R
import se.kodhast.ui.components.ActionColor
import se.kodhast.ui.components.ActionMenuStyle
import se.kodhast.ui.components.AppBrandStyle
import se.kodhast.ui.components.AppIdentity
import se.kodhast.ui.components.LocalAppBrandStyle
import se.kodhast.ui.components.LocalAppIdentity
import se.kodhast.ui.components.ToolbarStyle
import se.kodhast.ui.theme.AppShapes
import se.kodhast.ui.theme.AppTheme

/** Appens former: kort 28 dp; övriga (rader 18 dp, pills helt rundade …) är android-ui:s standard. */
private val ReseApoteketShapes = AppShapes(cardRadius = 28.dp)

/**
 * Kodhäst-igenkänningen (NFR-22): prickar före sektionsrubriker utan ikonruta, prickar som laddning, märket i tomma lägen
 * och prickarna i sidhuvudet som hoppar vid synk (NAV-4, NFR-6); flikraden i appens ljusa ton med plusknappens val som
 * FAB-meny (NAV-1, NAV-6).
 */
private val ReseApoteketBrandStyle = AppBrandStyle(
    sectionDots = true,
    dotLoading = true,
    emptyStateMark = true,
    headerDots = true,
    toolbarStyle = ToolbarStyle.Tonal,
    actionMenu = ActionMenuStyle.Fab,
    // secondary är en neutral gråbrun i ReseApoteket; den blåa accenten skiljer plusknappen från den valda fliken (NAV-6).
    actionColor = ActionColor.Tertiary
)

/**
 * Appens tema: android-ui:s `AppTheme` (Material 3 Expressive med fjädrande rörelse) med
 * [AppColors], [AppTypeScale] och appens former (skill `ui-style`). Ingen dynamic color.
 * Ljust, mörkt eller system väljs i Inställningar; `MainActivity` räknar ut [darkTheme]. Kodhäst-delarna
 * slås på här, en gång för hela appen ([ReseApoteketBrandStyle]), och djupet ([AppColors.lightDepth], NFR-23). Appens
 * identitet (`LocalAppIdentity`: ikonen och namnet) ger flikarna Kodhäst-sidhuvudet (NAV-4).
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
        CompositionLocalProvider(
            LocalAppBrandStyle provides ReseApoteketBrandStyle,
            LocalAppIdentity provides AppIdentity(R.drawable.ic_app_header, stringResource(R.string.app_name)),
            content = content
        )
    }
}
