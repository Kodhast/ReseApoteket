package se.kodhast.reseapoteket.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import se.kodhast.reseapoteket.core.engine.PackingProgress
import se.kodhast.reseapoteket.core.engine.Reminder
import se.kodhast.reseapoteket.core.engine.ReminderKind
import se.kodhast.reseapoteket.core.engine.ReminderSetting
import se.kodhast.reseapoteket.core.engine.ReminderSettings
import se.kodhast.reseapoteket.core.model.PackingItem
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Trip
import se.kodhast.reseapoteket.data.FakeCollectionFactory
import se.kodhast.reseapoteket.data.FixedClock
import se.kodhast.reseapoteket.data.datastore.SettingsStore
import se.kodhast.reseapoteket.data.repository.DefaultPackingRepository
import se.kodhast.reseapoteket.data.repository.DefaultPersonRepository
import se.kodhast.reseapoteket.data.repository.DefaultTripRepository
import se.kodhast.reseapoteket.reminders.ReminderNotifier
import se.kodhast.reseapoteket.reminders.ReminderRunner
import se.kodhast.reseapoteket.reminders.ReminderSource
import se.kodhast.reseapoteket.testing.FakeDataStore
import se.kodhast.reseapoteket.testing.MainDispatcherRule
import se.kodhast.reseapoteket.testing.captureLightAndDark
import se.kodhast.reseapoteket.testing.captureScreenLightAndDark
import se.kodhast.reseapoteket.testing.clickWithoutRipple
import se.kodhast.reseapoteket.ui.theme.ReseApoteketTheme

/** Kortet Påminnelser i Inställningar (PÅM-1, PÅM-7). */
@RunWith(RobolectricTestRunner::class)
class RemindersTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val collections = FakeCollectionFactory()
    private val persons = DefaultPersonRepository(collections)
    private val trips = DefaultTripRepository(collections)
    private val packing = DefaultPackingRepository(collections)
    private val settings = SettingsStore(FakeDataStore())

    // FixedClock = mån 21 sep 2026.
    private val source = ReminderSource(trips, packing, persons, settings)
    private val runner = ReminderRunner(source, settings, ReminderNotifier(RuntimeEnvironment.getApplication()), FixedClock()) { TimeZone.UTC }

    private fun viewModel() = RemindersViewModel(persons, source, settings, runner, FixedClock(), TimeZone.UTC)

    private val people = listOf(
        Person("anna", "Anna", "🦊", sortOrder = 0),
        Person("erik", "Erik", "🐻", sortOrder = 1),
        Person("lova", "Lova", "🐰", sortOrder = 2),
        Person("gunnar", "Morfar Gunnar", "🦉", sortOrder = 3)
    )
    private val granCanaria = Reminder(
        ReminderKind.TIME_TO_PACK,
        "gc",
        "Gran Canaria",
        LocalDate(2026, 10, 3),
        LocalDateTime(2026, 9, 30, 18, 0),
        PackingProgress(9, 16)
    )
    private val state = RemindersUiState(people.toImmutableList(), ReminderSettings(personIds = setOf("anna", "lova")), persistentMapOf(ReminderKind.TIME_TO_PACK to granCanaria))

    @Test
    fun `personerna utan arkiverade, inställningarna och nästa påminnelse av varje slag`() = runTest {
        persons.upsert(Person("erik", "Erik", sortOrder = 1))
        persons.upsert(Person("anna", "Anna", sortOrder = 0))
        persons.upsert(Person("lova", "Lova", archived = true))
        trips.upsert(Trip("gc", "Gran Canaria", LocalDate(2026, 10, 3), 7, listOf("anna")))
        packing.forTrip("gc").upsert(PackingItem("r1"))
        settings.setReminderPersons(setOf("anna"))
        val vm = viewModel()
        vm.state.test {
            var current = awaitItem()
            while (current.next.isEmpty() || current.persons.isEmpty()) current = awaitItem()
            assertEquals(listOf("Anna", "Erik"), current.persons.map { it.name })
            assertEquals(setOf(ReminderKind.TIME_TO_PACK, ReminderKind.DAY_BEFORE, ReminderKind.TRAVEL_DAY), current.next.keys)
            assertEquals(LocalDateTime(2026, 9, 30, 18, 0), current.next.getValue(ReminderKind.TIME_TO_PACK).at)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `val av personer och ändrade påminnelser sparas på telefonen`() = runTest {
        val vm = viewModel()
        vm.onEvent(RemindersEvent.TogglePerson("anna"))
        vm.onEvent(RemindersEvent.TogglePerson("lova"))
        vm.onEvent(RemindersEvent.TogglePerson("anna"))
        val changed = ReminderSetting(true, LocalTime(19, 30), 5)
        vm.onEvent(RemindersEvent.Change(ReminderKind.TIME_TO_PACK, changed))
        val saved = settings.reminderSettings.first()
        assertEquals(setOf("lova"), saved.personIds)
        assertEquals(changed, saved[ReminderKind.TIME_TO_PACK])
        // En ändring visar inte påminnelser som redan passerat (PÅM-6).
        assertEquals(FixedClock().instant, settings.remindersCheckedUntil.first())
    }

    private fun show(state: RemindersUiState, allowed: Boolean = true, onEvent: (RemindersEvent) -> Unit = {}, onAsk: () -> Unit = {}, onSettings: () -> Unit = {}) = rule.setContent { ReseApoteketTheme { RemindersCard(state, onEvent, allowed, onAsk, onSettings) } }

    @Test
    fun `raderna visar dagar och tid, växeln slår av och på och frågar om notiser`() {
        val events = mutableListOf<RemindersEvent>()
        var asked = 0
        show(state, onEvent = { events += it }, onAsk = { asked++ })
        rule.onNodeWithText("3 dagar före · 18:00").assertIsDisplayed()
        rule.onNodeWithText("7 dagar före · 18:00").assertIsDisplayed()
        rule.onNodeWithText("07:00").assertIsDisplayed()
        rule.onNodeWithContentDescription("Dagen före avresan").performClick()
        assertEquals(RemindersEvent.Change(ReminderKind.DAY_BEFORE, ReminderSettings()[ReminderKind.DAY_BEFORE].copy(enabled = false)), events.single())
        assertEquals(0, asked)
        rule.onNodeWithText("Erik").performClick()
        assertEquals(RemindersEvent.TogglePerson("erik"), events.last())
        assertEquals(1, asked)
    }

    @Test
    fun `en rad öppnar påminnelsens inställningar med dagar, tid och nästa påminnelse`() {
        var current by mutableStateOf(state)
        show(current, onEvent = { event ->
            if (event is RemindersEvent.Change) current = current.copy(settings = current.settings.with(event.kind, event.setting))
        })
        rule.onNodeWithText("Dags att packa").performClick()
        rule.onNodeWithText("En påminnelse om packlistan inte är klar.").assertIsDisplayed()
        rule.onNodeWithText("ons 30 sep 2026 kl. 18:00").assertIsDisplayed()
        rule.onNodeWithContentDescription("Tid, 18:00").assertIsDisplayed()
        rule.onNodeWithText("Klar").performClick()
        rule.onNodeWithText("En påminnelse om packlistan inte är klar.").assertDoesNotExist()
    }

    @Test
    fun `avstängda notiser visas som banner som öppnar inställningarna`() {
        var opened = 0
        show(state, allowed = false, onSettings = { opened++ })
        rule.onNodeWithText("Påminnelserna kan inte visas. Tryck för att slå på notiser för ReseApoteket.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Notiser är avstängda")
            .assertIsDisplayed()
            .assert(SemanticsMatcher("TalkBack läser Slå på") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Slå på" })
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    @Config(qualifiers = "w390dp-h1100dp-xxhdpi")
    fun `Påminnelser - kortet`() = captureLightAndDark("RemindersCard_standard") {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { RemindersCard(state, {}, true, {}, {}) }
    }

    @Test
    fun `Påminnelser - notiser avstängda`() = captureLightAndDark("RemindersCard_avstangda") {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { RemindersCard(state, {}, false, {}, {}) }
    }

    @Test
    fun `Påminnelser - ändra en påminnelse`() = rule.captureScreenLightAndDark("RemindersCard_andra", open = {
        onNode(hasScrollAction()).performScrollToNode(hasText("Dags att packa"))
        onNodeWithText("Dags att packa").clickWithoutRipple()
    }) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SettingsContent(SettingsUiState(), {}, "0.4.0", {}, onOpenGallery = null) { RemindersCard(state, {}, true, {}, {}) }
        }
    }
}
