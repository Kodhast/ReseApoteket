package se.kodhast.reseapoteket.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import se.kodhast.reseapoteket.R
import se.kodhast.ui.components.ActionColor
import se.kodhast.ui.components.ActionMenuStyle
import se.kodhast.ui.components.AppBrandStyle
import se.kodhast.ui.components.AppIdentity
import se.kodhast.ui.components.LocalAppBrandStyle
import se.kodhast.ui.components.LocalAppIdentity
import se.kodhast.ui.components.ToolbarStyle
import se.kodhast.ui.theme.AppTheme

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
 * [AppColors], och [AppTypeScale] (skill `ui-style`); formerna är bibliotekets (22 dp-kort, 3.0.0). Ingen dynamic color.
 * Ljust, mörkt eller system väljs i Inställningar; `MainActivity` räknar ut [darkTheme]. Kodhäst-delarna
 * slås på här, en gång för hela appen ([ReseApoteketBrandStyle]), och djupet ([AppColors.lightDepth], NFR-23). Appens
 * identitet (`LocalAppIdentity`: ikonen, namnet och [appVersion]) ger flikarna Kodhäst-sidhuvudet (NAV-4). [appVersion] sätter
 * bara `MainActivity`, så att skärmdumparna inte bär något riktigt versionsnummer.
 */
@Composable
fun ReseApoteketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appVersion: String? = null,
    content: @Composable () -> Unit
) {
    AppTheme(
        colorScheme = if (darkTheme) AppColors.dark else AppColors.light,
        extendedColors = if (darkTheme) AppColors.darkExtended else AppColors.lightExtended,
        typeScale = AppTypeScale,
        depth = if (darkTheme) AppColors.darkDepth else AppColors.lightDepth
    ) {
        CompositionLocalProvider(
            LocalAppBrandStyle provides ReseApoteketBrandStyle,
            LocalAppIdentity provides AppIdentity(R.drawable.ic_app_header, stringResource(R.string.app_name), appVersion),
            content = content
        )
    }
}
