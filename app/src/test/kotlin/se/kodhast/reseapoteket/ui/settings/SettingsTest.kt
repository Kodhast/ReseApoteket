package se.kodhast.reseapoteket.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.kodhast.reseapoteket.core.model.Household
import se.kodhast.reseapoteket.core.schema.HouseholdCodec
import se.kodhast.reseapoteket.data.FixedClock
import se.kodhast.reseapoteket.data.common.DataError
import se.kodhast.reseapoteket.data.datastore.SettingsStore
import se.kodhast.reseapoteket.data.datastore.ThemeMode
import se.kodhast.reseapoteket.data.export.ExportFiles
import se.kodhast.reseapoteket.data.export.HouseholdExport
import se.kodhast.reseapoteket.testing.FakeDataStore
import se.kodhast.reseapoteket.testing.MainDispatcherRule
import se.kodhast.reseapoteket.testing.StubHouseholds
import se.kodhast.reseapoteket.testing.captureLightAndDark
import se.kodhast.reseapoteket.ui.theme.ReseApoteketTheme

/** Inställningar (SET-1–3, SET-5, SET-7): hushåll, packlista, tema, påminnelser och data & backup. Konto och Om: [AccountViewModelTest], [AboutTest]. */
@RunWith(RobolectricTestRunner::class)
class SettingsTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val svensson = Household("h1", "Familjen Svensson", listOf("uid-anna"), marginDays = 2, schemaVersion = 2)
    private val households = StubHouseholds(MutableStateFlow(svensson))
    private val settings = SettingsStore(FakeDataStore())
    private var exported: Result<String> = Result.success("""{"documents":[]}""")
    private val saved = mutableListOf<String>()
    private var saveResult: Result<String> = Result.success("content://se.kodhast.reseapoteket.exports/exports/fil.json")
    private val export = object : HouseholdExport {
        override suspend fun export() = exported
    }
    private val files = object : ExportFiles {
        override suspend fun save(name: String, text: String): Result<String> = saveResult.also { saved += name }

        override suspend fun cleanUp(now: Instant, maxAge: Duration) = Unit
    }

    private fun viewModel() = SettingsViewModel(settings, households, export, files, FixedClock(), TimeZone.UTC)

    private val state = SettingsUiState(svensson, ThemeMode.System, moveCheckedLast = true)

    private fun show(state: SettingsUiState, onEvent: (SettingsEvent) -> Unit = {}, onEditHousehold: () -> Unit = {}) {
        rule.setContent { ReseApoteketTheme { SettingsContent(state, onEvent, "0.1.0", onEditHousehold) } }
    }

    private fun scrollTo(text: String) = rule.onNode(hasScrollAction()).performScrollToNode(hasText(text))

    @Test
    fun `tema och Flytta avprickade sist sparas på enheten (SET-1, SET-3)`() = runTest {
        val vm = viewModel()
        vm.state.test {
            assertEquals(ThemeMode.System, awaitItem().themeMode)
            vm.onEvent(SettingsEvent.SetTheme(ThemeMode.Dark))
            assertEquals(ThemeMode.Dark, expectMostRecentItem().themeMode)
            vm.onEvent(SettingsEvent.SetMoveCheckedLast(false))
            assertFalse(expectMostRecentItem().moveCheckedLast)
        }
    }

    @Test
    fun `marginaldagar skriver bara sitt fält och stannar inom 0–7 (SET-2)`() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.onEvent(SettingsEvent.SetMarginDays(3))
            assertEquals(3, expectMostRecentItem().household?.marginDays)
            vm.onEvent(SettingsEvent.SetMarginDays(12))
            assertEquals(7, expectMostRecentItem().household?.marginDays)
            vm.onEvent(SettingsEvent.SetMarginDays(7))
            assertEquals(listOf(setOf(HouseholdCodec.MARGIN_DAYS), setOf(HouseholdCodec.MARGIN_DAYS)), households.updates)
            assertEquals("Familjen Svensson", households.current.value?.name)

            households.nextUpdate = Result.failure(DataError.UpdateRequired)
            vm.onEvent(SettingsEvent.SetMarginDays(1))
            assertEquals(SettingsError(SettingsSection.Household, DataError.UpdateRequired), expectMostRecentItem().error)
        }
    }

    @Test
    fun `ett fel från en annan åtgärd under exporten står kvar när exporten blir klar (SET-7)`() = runTest {
        val done = CompletableDeferred<Result<String>>()
        val slow = object : HouseholdExport {
            override suspend fun export() = done.await()
        }
        val vm = SettingsViewModel(settings, households, slow, files, FixedClock(), TimeZone.UTC)
        vm.state.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Export)
            assertEquals(true, expectMostRecentItem().exporting)
            households.nextUpdate = Result.failure(DataError.Offline)
            vm.onEvent(SettingsEvent.SetMarginDays(1))
            done.complete(Result.success("{}"))
            val after = expectMostRecentItem()
            assertFalse(after.exporting)
            assertEquals(SettingsError(SettingsSection.Household, DataError.Offline), after.error)
        }
    }

    @Test
    fun `export sparar filen med dagens datum och öppnar delningsmenyn en gång (SET-5)`() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Export)
            assertEquals(ShareExport("content://se.kodhast.reseapoteket.exports/exports/fil.json"), expectMostRecentItem().share)
            // Delningen står kvar tills skärmen öppnat menyn, och kommer sedan inte igen.
            vm.onEvent(SettingsEvent.ShareShown)
            assertNull(expectMostRecentItem().share)
            expectNoEvents()
        }
        assertEquals(listOf("reseapoteket-2026-09-21.json"), saved)
    }

    @Test
    fun `en misslyckad export visas i datakortet och delar ingenting (SET-5)`() = runTest {
        val vm = viewModel()
        exported = Result.failure(DataError.Offline)
        vm.state.test {
            awaitItem()
            vm.onEvent(SettingsEvent.Export)
            val failed = expectMostRecentItem()
            assertNull(failed.share)
            assertEquals(SettingsError(SettingsSection.Data, DataError.Offline), failed.error)
            assertFalse(failed.exporting)
        }
        assertEquals(emptyList(), saved)
    }

    @Test
    fun `raderna skickar sina händelser och (SET-2, SET-5)`() {
        val events = mutableListOf<SettingsEvent>()
        var edits = 0
        show(state, { events += it }, onEditHousehold = { edits++ })
        rule.onNodeWithText("Familjen Svensson").performClick()
        assertEquals(1, edits)
        rule.onNodeWithContentDescription("Öka Marginaldagar").performClick()
        rule.onNodeWithText("Mörkt").performClick()
        scrollTo("Exportera som JSON")
        rule.onNodeWithText("Exportera som JSON").performClick()
        assertEquals(
            listOf(SettingsEvent.SetMarginDays(3), SettingsEvent.SetTheme(ThemeMode.Dark), SettingsEvent.Export),
            events
        )
    }

    @Test
    fun `ett fel visas i kortet där åtgärden gjordes, och data och version syns (SET-5)`() {
        show(state.copy(error = SettingsError(SettingsSection.Data, DataError.Offline)))
        scrollTo("Dataformat 2 · appversion 0.1.0")
        rule.onNodeWithText("Dataformat 2 · appversion 0.1.0").assertIsDisplayed()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Ingen anslutning", substring = true))
        rule.onNodeWithText("Ingen anslutning", substring = true).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w390dp-h1500dp-xxhdpi")
    fun `Inställningar - standard`() = captureLightAndDark("SettingsScreen_standard") {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { SettingsContent(state, {}, "0.1.0", {}) }
    }
}
