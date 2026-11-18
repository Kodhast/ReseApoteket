package se.kodhast.reseapoteket.navigation

import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.DataError
import se.kodhast.reseapoteket.ui.theme.ReseApoteketTheme
import se.kodhast.ui.common.SyncUiState
import se.kodhast.ui.components.AccountAvatar
import se.kodhast.ui.components.AppHeader

/** Navigationsramen: verktygsraden i flikarnas rot, dold på undersidor (NAV-1), plusmenyn (NAV-6), avataren (NAV-5) och synkprickarna (NFR-6). */
@RunWith(RobolectricTestRunner::class)
class AppNavHostTest {

    @get:Rule
    val rule = createComposeRule()

    private val backStack = appBackStack()

    private fun show() = rule.setContent {
        ReseApoteketTheme {
            AppNavHost(backStack, { key -> NavEntry(key) { Text("Skärm: ${key::class.simpleName}") } })
        }
    }

    @Test
    fun `verktygsraden byter flik och visar flikens skärm`() {
        show()
        rule.onNodeWithText("Skärm: TripsKey").assertIsDisplayed()
        rule.onNodeWithContentDescription("Personer").performClick()
        rule.onNodeWithText("Skärm: PersonsKey").assertIsDisplayed()
        assertEquals(PersonsKey, backStack.currentTab)
    }

    @Test
    fun `en flik behåller sitt sparade tillstånd när man byter flik och tillbaka`() {
        rule.setContent {
            ReseApoteketTheme {
                AppNavHost(backStack, { key ->
                    NavEntry(key) {
                        var taps by rememberSaveable { mutableIntStateOf(0) }
                        Text("${key::class.simpleName}: $taps", Modifier.clickable { taps++ })
                    }
                })
            }
        }
        rule.onNodeWithText("PersonsKey", substring = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Personer").performClick()
        rule.onNodeWithText("PersonsKey: 0").performClick()
        rule.onNodeWithText("PersonsKey: 1").assertIsDisplayed()
        rule.onNodeWithContentDescription("Gemensamt").performClick()
        rule.onNodeWithContentDescription("Personer").performClick()
        rule.onNodeWithText("PersonsKey: 1").assertIsDisplayed()
    }

    @Test
    fun `en stängd undersida börjar om när den öppnas igen`() {
        rule.setContent {
            ReseApoteketTheme {
                AppNavHost(backStack, { key ->
                    NavEntry(key) {
                        var taps by rememberSaveable { mutableIntStateOf(0) }
                        Text("${key::class.simpleName}: $taps", Modifier.clickable { taps++ })
                    }
                })
            }
        }
        rule.runOnIdle { backStack.push(TripEditKey()) }
        rule.onNodeWithText("TripEditKey: 0").performClick()
        rule.runOnIdle { backStack.pop() }
        rule.runOnIdle { backStack.push(TripEditKey()) }
        rule.onNodeWithText("TripEditKey: 0").assertIsDisplayed()
    }

    @Test
    fun `undersidor döljer verktygsraden`() {
        show()
        rule.runOnIdle { backStack.push(TripEditKey()) }
        rule.onNodeWithText("Skärm: TripEditKey").assertIsDisplayed()
        rule.onNodeWithContentDescription("Personer").assertDoesNotExist()
        rule.runOnIdle { backStack.pop() }
        rule.onNodeWithContentDescription("Personer").assertIsDisplayed()
    }

    /** En flikrot med appens sidhuvud, som `EntityListScreen` ger flikarna. */
    @OptIn(ExperimentalMaterial3Api::class)
    private val header: (AppKey) -> NavEntry<AppKey> = { key -> NavEntry(key) { AppHeader("Resor", TopAppBarDefaults.pinnedScrollBehavior()) } }

    @Test
    fun `prickarna i sidhuvudet säger att ändringar väntar på servern (NFR-6)`() {
        var sync by mutableStateOf(SyncUiState())
        rule.setContent {
            ReseApoteketTheme {
                AppNavHost(backStack, header, sync = sync)
            }
        }
        rule.onNodeWithContentDescription(PENDING).assertDoesNotExist()
        sync = SyncUiState(pending = true)
        rule.onNodeWithContentDescription(PENDING).assertExists()
    }

    @Test
    fun `plusknappens meny har de tre valen och ger det valda (NAV-6)`() {
        val picked = mutableListOf<AddChoice>()
        rule.setContent {
            ReseApoteketTheme {
                AppNavHost(backStack, { key -> NavEntry(key) { Text("Skärm: ${key::class.simpleName}") } }, onAdd = { picked += it })
            }
        }
        rule.onNodeWithText("Ny person").assertDoesNotExist()
        rule.onNodeWithContentDescription("Lägg till").performClick()
        rule.onNodeWithText("Ny resa").assertIsDisplayed()
        rule.onNodeWithText("Ny gemensam sak").assertIsDisplayed()
        rule.onNodeWithText("Ny person").performClick()
        assertEquals(listOf(AddChoice.Person), picked)
    }

    @Test
    fun `utan onAdd har verktygsraden ingen plusknapp`() {
        show()
        rule.onNodeWithContentDescription("Lägg till").assertDoesNotExist()
    }

    @Test
    fun `avataren står i flikens sidhuvud (NAV-5)`() {
        var opened = 0
        rule.setContent {
            ReseApoteketTheme {
                AppNavHost(
                    backStack,
                    header,
                    account = { AccountAvatar("Mats Testsson", onClick = { opened++ }) }
                )
            }
        }
        rule.onNodeWithContentDescription("Konto och inställningar").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `en nekad skrivning visas med orsaken och släpps sedan (NFR-6)`() {
        var shown = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ReseApoteketTheme {
                AppNavHost(
                    backStack,
                    { key -> NavEntry(key) { Text("Skärm: ${key::class.simpleName}") } },
                    sync = SyncUiState(writeError = AppErrors.map(DataError.PermissionDenied)),
                    onSyncErrorShown = { shown++ }
                )
            }
        }
        rule.mainClock.advanceTimeBy(SHOWN_MILLIS)
        rule.onNodeWithText("En ändring kunde inte sparas och har ångrats. Du har inte behörighet till det här hushållet.").assertIsDisplayed()
        assertEquals(0, shown)
        rule.mainClock.advanceTimeBy(LONG_SNACKBAR_MILLIS)
        rule.waitForIdle()
        assertEquals(1, shown)
    }

    @Test
    fun `en påminnelse öppnar resans packlista och släpps sedan (PÅM-7)`() {
        var trip by mutableStateOf<String?>(null)
        var opened = 0
        rule.setContent {
            ReseApoteketTheme {
                OpenTripEffect(backStack, trip) {
                    trip = null
                    opened++
                }
                AppNavHost(backStack, { key -> NavEntry(key) { Text("Skärm: $key") } })
            }
        }
        rule.runOnIdle { backStack.select(PersonsKey) }
        trip = "gc"
        rule.onNodeWithText("Skärm: PackingListKey(tripId=gc)").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, opened) }
    }

    private companion object {
        const val PENDING = "Ändringar väntar på servern"
        const val SHOWN_MILLIS = 500L
        const val LONG_SNACKBAR_MILLIS = 11_000L
    }
}
