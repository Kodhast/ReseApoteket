package se.kodhast.reseapoteket.ui.theme

import androidx.navigation3.runtime.NavEntry
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.reseapoteket.navigation.AppBackStack
import se.kodhast.reseapoteket.navigation.AppNavHost
import se.kodhast.reseapoteket.testing.captureLightAndDark
import se.kodhast.reseapoteket.ui.AppRootContent
import se.kodhast.reseapoteket.ui.auth.AuthGate
import se.kodhast.reseapoteket.ui.auth.AuthUiState
import se.kodhast.reseapoteket.ui.trips.TripsContent
import se.kodhast.ui.common.ListUiState

@RunWith(RobolectricTestRunner::class)
class ThemeScreenshotTest {

    /** Hela startskärmen efter inloggning: Resor utan resor, med verktygsraden (NAV-2). */
    @Test
    fun `startskärmen i ljust och mörkt tema`() {
        captureLightAndDark("AppRoot_start") {
            AppRootContent(AuthUiState(AuthGate.Ready), {}) {
                AppNavHost(AppBackStack(), { key -> NavEntry(key) { TripsContent(ListUiState.Empty, {}, {}, {}) } })
            }
        }
    }
}
