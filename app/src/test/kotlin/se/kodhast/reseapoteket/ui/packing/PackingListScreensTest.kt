package se.kodhast.reseapoteket.ui.packing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.kodhast.reseapoteket.core.engine.PackingEngine
import se.kodhast.reseapoteket.core.engine.PackingGroup
import se.kodhast.reseapoteket.core.engine.PackingList
import se.kodhast.reseapoteket.core.engine.PackingProgress
import se.kodhast.reseapoteket.core.engine.PastedLine
import se.kodhast.reseapoteket.core.engine.PastedStatus
import se.kodhast.reseapoteket.core.model.ItemUnit
import se.kodhast.reseapoteket.core.model.PackingItem
import se.kodhast.reseapoteket.core.model.PackingSection
import se.kodhast.reseapoteket.core.model.PackingSource
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Trip
import se.kodhast.reseapoteket.core.model.TripStatus
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.DataError
import se.kodhast.reseapoteket.testing.captureLightAndDark
import se.kodhast.reseapoteket.testing.captureScreenLightAndDark
import se.kodhast.reseapoteket.ui.contracts
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.reseapoteket.ui.theme.ReseApoteketTheme
import se.kodhast.ui.common.EditorUiState
import se.kodhast.ui.common.ListEvent
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.ArchiveEvent
import se.kodhast.ui.components.ENTITY_LIST_ADD_TAG
import se.kodhast.ui.components.ListArchive
import se.kodhast.ui.testing.runListScreenContract

/** Packlistan (PAK-1–5): listkontraktet, det som är unikt för packlistan och skärmdumpar. */
@RunWith(RobolectricTestRunner::class)
class PackingListScreensTest {

    @get:Rule
    val rule = createComposeRule()

    private val anna = Person("anna", "Anna", "🦊", AppColors.PERSON_HEX[0])
    private val erik = Person("erik", "Erik", "🐻", AppColors.PERSON_HEX[1])
    private val lova = Person("lova", "Lova", "🐰", AppColors.PERSON_HEX[2])
    private val gunnar = Person("gunnar", "Morfar Gunnar", "🦉", AppColors.PERSON_HEX[3])
    private val trip = Trip("t1", "Gran Canaria", LocalDate(2026, 10, 2), 7, listOf("anna", "erik", "lova", "gunnar"))

    private fun med(id: String, person: Person, name: String, quantity: Int, unit: ItemUnit = ItemUnit.TABLETT, packages: Int? = null, checked: Boolean = false, suggested: Int = quantity, section: PackingSection = PackingSection.MEDICINE) = PackingItem(id, section, person.id, PackingSource.REGIMEN, id, name, unit, suggested, quantity, packages, "1 × 1 tablett/dag × (7 + 2) dagar", checked)

    private fun other(id: String, name: String, quantity: Int, unit: ItemUnit, checked: Boolean = false, hidden: Boolean = false, manual: Boolean = false) = PackingItem(id, PackingSection.FAMILY, null, if (manual) PackingSource.MANUAL else PackingSource.FAMILY_ITEM, id, name, unit, quantity, quantity, checked = checked, hidden = hidden)

    private val rows = listOf(
        PackingRow(med("levaxin", anna, "Levaxin 50 µg", 9, packages = 1, checked = true), anna, "1/dag"),
        PackingRow(med("zyrtec", anna, "Zyrtec 10 mg", 9, checked = true), anna, "1/dag"),
        PackingRow(med("enalapril", erik, "Enalapril 10 mg", 18, packages = 2), erik, "2/dag"),
        PackingRow(med("ventoline", erik, "Ventoline", 1, ItemUnit.ST, checked = true), erik, "vid behov"),
        PackingRow(med("alvedon", lova, "Alvedon 250 mg", 14, suggested = 10), lova, "vid behov"),
        PackingRow(med("metformin", gunnar, "Metformin 500 mg", 18), gunnar, "2/dag"),
        PackingRow(med("d", anna, "D-vitamin", 9, ItemUnit.KAPSEL, checked = true, section = PackingSection.SUPPLEMENT), anna, "1/dag"),
        PackingRow(med("omega", gunnar, "Omega-3", 9, ItemUnit.KAPSEL, section = PackingSection.SUPPLEMENT), gunnar, "1/dag"),
        PackingRow(other("solkram", "Solkräm", 2, ItemUnit.TUB, checked = true)),
        PackingRow(other("termometer", "Febertermometer", 1, ItemUnit.ST)),
        PackingRow(other("hydro", "Hydrokortison", 1, ItemUnit.TUB, checked = true)),
        PackingRow(other("laddare", "Laddare", 1, ItemUnit.ST, manual = true)),
        PackingRow(other("plaster", "Plåster", 1, ItemUnit.ST, hidden = true))
    )

    private fun header(of: List<PackingRow>, status: TripStatus = TripStatus.PLANNING): PackingHeader {
        val items = of.map { it.item }
        val shown = items.filterNot { it.hidden }
        return PackingHeader(
            trip.copy(status = status),
            persistentListOf(anna, erik, lova, gunnar),
            PackingEngine.progress(items),
            PackingGroup.entries.associateWith { group -> PackingEngine.progress(shown.filter { PackingList.group(it) == group }) }.toImmutableMap()
        )
    }

    private val allChecked = rows.map { it.copy(item = it.item.copy(checked = true)) }

    @Test
    fun `packlistan uppfyller listkontraktet, utan lägg till-knapp nere till höger`() = rule.runListScreenContract(contracts, rows.first(), "Levaxin 50 µg", "Inget att packa än", null) { state, _, onRetry ->
        PackingListContent(state, header(rows), { if (it == PackingListEvent.List(ListEvent.Retry)) onRetry() }, {}, {})
    }

    @Test
    fun `plus i rubriken på Mediciner, Kosttillskott och Övrigt, också hopfälld, och rubriken fäller ihop (PAK-1, PAK-4)`() {
        val events = mutableListOf<PackingListEvent>()
        rule.setContent {
            ReseApoteketTheme {
                PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, collapsed = setOf(PackingGroup.MEDICINE))
            }
        }
        rule.onNodeWithText("Levaxin 50 µg").assertDoesNotExist()
        rule.onNodeWithText("3 / 6").assertIsDisplayed()
        rule.onNodeWithContentDescription("Lägg till i Mediciner").performClick()
        rule.onNodeWithContentDescription("Lägg till i Kosttillskott").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Lägg till i Övrigt"))
        rule.onNodeWithContentDescription("Lägg till i Övrigt").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Kosttillskott"))
        rule.onNodeWithText("Kosttillskott").performClick()
        assertEquals(
            listOf(
                PackingListEvent.Add(PackingSection.MEDICINE),
                PackingListEvent.Add(PackingSection.SUPPLEMENT),
                PackingListEvent.Add(PackingSection.FAMILY),
                PackingListEvent.ToggleGroup(PackingGroup.SUPPLEMENT)
            ),
            events
        )
        rule.onNodeWithTag(ENTITY_LIST_ADD_TAG).assertDoesNotExist()
    }

    @Test
    fun `⋮ har Räkna om, Dölj packade och Visa dolda bara när något är dolt (PAK-4)`() {
        val events = mutableListOf<PackingListEvent>()
        val archive = mutableListOf<ArchiveEvent>()
        var hidden by mutableIntStateOf(0)
        rule.setContent {
            ReseApoteketTheme {
                PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, archive = ListArchive { archive += it }, hiddenCount = hidden)
            }
        }
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Dölj packade").assertIsDisplayed()
        rule.onNodeWithText("Visa dolda", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Räkna om").performClick()
        assertEquals(listOf<PackingListEvent>(PackingListEvent.AskRecount), events)
        hidden = 2
        rule.onNodeWithContentDescription("Fler val").performClick()
        rule.onNodeWithText("Visa dolda (2)").performClick()
        assertEquals(listOf<ArchiveEvent>(ArchiveEvent.ToggleArchived), archive)
    }

    @Test
    fun `en tom packlista visar de tre sektionerna med plus och Räkna om (PAK-1)`() {
        val events = mutableListOf<PackingListEvent>()
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Empty, header(emptyList()), { events += it }, {}, {}) } }
        listOf("Inget att packa än", "Mediciner", "Kosttillskott").forEach { rule.onNodeWithText(it).assertIsDisplayed() }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Övrigt"))
        rule.onNodeWithContentDescription("Lägg till i Övrigt").assertIsDisplayed()
        rule.onAllNodesWithText("Inget här än").assertCountEquals(3)
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Räkna om"))
        rule.onNodeWithText("Räkna om").performClick()
        assertEquals(listOf<PackingListEvent>(PackingListEvent.AskRecount), events)
    }

    @Test
    fun `krysset prickar av, raden öppnar mängden och svep döljer (PAK-2–4)`() {
        val events = mutableListOf<PackingListEvent>()
        val hidden = mutableListOf<ArchiveEvent>()
        rule.setContent {
            ReseApoteketTheme {
                PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, archive = ListArchive(showToggle = false) { hidden += it })
            }
        }
        rule.onNodeWithContentDescription("Enalapril 10 mg, 18 tabletter · 2 kartor").performClick()
        rule.onNodeWithText("Enalapril 10 mg").performClick()
        rule.onNodeWithText("Zyrtec 10 mg").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(listOf(PackingListEvent.Check("enalapril", true), PackingListEvent.Open("enalapril")), events)
        assertEquals(listOf<ArchiveEvent>(ArchiveEvent.Archive("zyrtec", "Zyrtec 10 mg")), hidden)
    }

    @Test
    fun `allt packat visar konfetti och kvitterar firandet en gång (PAK-5)`() {
        val events = mutableListOf<PackingListEvent>()
        var celebrate by mutableStateOf(false)
        rule.setContent {
            ReseApoteketTheme {
                PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {
                    if (it == PackingListEvent.Celebrated) celebrate = false
                    events += it
                }, {}, {}, celebrate = celebrate)
            }
        }
        rule.runOnIdle { celebrate = true }
        rule.waitForIdle()
        assertEquals(listOf<PackingListEvent>(PackingListEvent.Celebrated), events)
    }

    @Test
    fun `Dolda visas sist och utfälld, och en dold rad visas igen (PAK-4)`() {
        val events = mutableListOf<PackingListEvent>()
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, archive = ListArchive(showing = true), hiddenCount = 1) } }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Visa igen"))
        rule.onNodeWithText("Plåster").assertIsDisplayed()
        rule.onNodeWithText("Visa igen").performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithText("Dolda").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(PackingListEvent.Show("plaster"), PackingListEvent.ToggleGroup(null)), events)
    }

    @Test
    fun `mängden har förslaget, Återställ och Klar (PAK-3)`() {
        val events = mutableListOf<PackingListEvent>()
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, editing = rows[4]) } }
        rule.onNodeWithText("Förslag").assertIsDisplayed()
        rule.onNodeWithText("10 tabletter").assertIsDisplayed()
        rule.onNodeWithText("Återställ till förslag (10)").performClick()
        rule.onNodeWithText("Klar").performClick()
        assertEquals(listOf<PackingListEvent>(PackingListEvent.ResetQuantity, PackingListEvent.CloseQuantity), events)
    }

    @Test
    fun `ett skrivfel visas i panelen`() {
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}, editing = rows[4], sheetError = AppErrors.map(DataError.Offline)) } }
        rule.onNodeWithText("Ingen anslutning", substring = true).assertIsDisplayed()
    }

    @Test
    fun `lägg till frågar vem för en medicin (PAK-4)`() {
        val events = mutableListOf<PackingListEvent>()
        val adding = EditorUiState(PackingItem("ny", PackingSection.MEDICINE, name = "Nässpray", quantity = 1, sourceType = PackingSource.MANUAL), isValid = false, isDirty = true)
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, adding = adding) } }
        rule.onNodeWithText("Lägg till i Mediciner").assertIsDisplayed()
        rule.onNodeWithText("Vem?", ignoreCase = true).assertIsDisplayed()
        rule.onNodeWithText("Var?", ignoreCase = true).assertDoesNotExist()
        rule.onNodeWithText("Lista").assertDoesNotExist()
        rule.onAllNodesWithText("Erik").filterToOne(hasClickAction()).performClick()
        assertEquals(listOf<PackingListEvent>(PackingListEvent.AddPerson("erik")), events)
    }

    private val newItem = EditorUiState(PackingItem("ny", PackingSection.FAMILY, sourceType = PackingSource.MANUAL, quantity = 1))

    private val pasted = AddListState(
        AddMode.LIST,
        "Solkräm 2\nLaddare\nPlåster",
        persistentListOf(PastedLine("Solkräm", 2, PastedStatus.ADD), PastedLine("Laddare", 1, PastedStatus.ADD), PastedLine("Plåster", 1, PastedStatus.EXISTS))
    )

    @Test
    @Config(qualifiers = "w390dp-h1000dp-xxhdpi")
    fun `lägg till som lista visar vad som läggs till under Övrigt och sparar (PAK-7)`() {
        val events = mutableListOf<PackingListEvent>()
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, adding = newItem, addList = pasted) } }
        rule.onNodeWithText("Lägg till i Övrigt").assertIsDisplayed()
        rule.onNodeWithText("Läggs till", ignoreCase = true).assertIsDisplayed()
        rule.onNodeWithText("2 st").assertIsDisplayed()
        rule.onNodeWithText("finns redan").assertIsDisplayed()
        rule.onNodeWithText("Vad?").assertDoesNotExist()
        rule.onNodeWithText("Lägg till 2 saker").performClick()
        rule.onNodeWithText("En sak").performClick()
        assertEquals(listOf(PackingListEvent.AddListSave, PackingListEvent.AddModeChange(AddMode.ONE)), events)
    }

    @Test
    fun `en lång lista – knappen står fast i arkets botten och förhandsvisningen går att scrolla till (#100)`() {
        val events = mutableListOf<PackingListEvent>()
        val lines = (1..72).map { PastedLine("Sak nummer $it i flyttlasset", 1, PastedStatus.ADD) }
        val long = AddListState(AddMode.LIST, lines.joinToString("\n") { it.name }, lines.toImmutableList())
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, adding = newItem, addList = long) } }
        // Fältet växer inte med alla 72 raderna – högst 8 rader, sedan scrollar det inuti.
        val field = rule.onNodeWithText("En sak per rad").getUnclippedBoundsInRoot()
        assertTrue(field.height < 320.dp, "fältet är ${field.height} högt")
        // Knappen står fast i arkets knapprad – synlig utan att rulla, hur lång listan än är.
        rule.onNodeWithText("Lägg till 72 saker").assertIsDisplayed().performClick()
        assertEquals(listOf<PackingListEvent>(PackingListEvent.AddListSave), events)
        rule.onNodeWithText("Sak nummer 72 i flyttlasset").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `en lista utan något nytt går inte att spara, och En sak byter till listan (PAK-7)`() {
        val events = mutableListOf<PackingListEvent>()
        val nothing = AddListState(AddMode.LIST, "Plåster", persistentListOf(PastedLine("Plåster", 1, PastedStatus.EXISTS)))
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, {}, adding = newItem, addList = nothing) } }
        rule.onAllNodesWithText("Lägg till").filter(isNotEnabled()).assertCountEquals(1)
        rule.onNodeWithText("En sak per rad").performTextInput("Laddare")
        assertTrue(events.single() is PackingListEvent.AddListText)
    }

    @Test
    @Config(qualifiers = "w390dp-h1000dp-xxhdpi")
    fun `skärmdump - lägg till som lista (PAK-7)`() = rule.captureScreenLightAndDark("PackingListScreen_lista") {
        PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}, adding = newItem, addList = pasted)
    }

    @Test
    fun `pennan öppnar resan och Räkna om frågar först`() {
        var edits = 0
        val events = mutableListOf<PackingListEvent>()
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), { events += it }, {}, { edits++ }, askRecount = true) } }
        rule.onNodeWithContentDescription("Redigera resa").performClick()
        rule.onNodeWithText("Räkna om packlistan?").assertIsDisplayed()
        rule.onNodeWithText("Räkna om").performClick()
        assertEquals(1, edits)
        assertEquals(listOf<PackingListEvent>(PackingListEvent.Recount), events)
    }

    @Test
    @Config(qualifiers = "w390dp-h1300dp-xxhdpi")
    fun `skärmdump - På resedagen sist i listan med tiderna (PAK-6)`() {
        val travelDay = listOf(
            rows[0],
            rows[2],
            PackingRow(med("insulin", anna, "Insulin NovoRapid", 1, ItemUnit.ST).copy(travelDay = true), anna, "3/dag"),
            PackingRow(other("tandborste", "Tandborste", 4, ItemUnit.ST).copy(travelDay = true)),
            PackingRow(other("laddare", "Laddare", 2, ItemUnit.ST, manual = true).copy(travelDay = true, travelDayTime = LocalTime(8, 30)))
        )
        captureLightAndDark("PackingListScreen_resedag") { PackingListContent(ListUiState.Content(travelDay.toImmutableList()), header(travelDay), {}, {}, {}) }
    }

    @Test
    fun `en resa som inte finns visar finns inte längre utan Försök igen och Lägg till (#88)`() {
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Error(AppErrors.notFound()), null, {}, {}, {}) } }
        rule.onNodeWithText("Det finns inte längre – det kan ha raderats på en annan enhet.").assertIsDisplayed()
        rule.onNodeWithText("Försök igen").assertDoesNotExist()
        rule.onNodeWithText("Lägg till").assertDoesNotExist()
        rule.onNodeWithContentDescription("Redigera resa").assertDoesNotExist()
        rule.onNodeWithContentDescription("Fler val").assertDoesNotExist()
    }

    @Test
    fun `skärmdump - resan finns inte (#88)`() {
        captureLightAndDark("PackingListScreen_finns_inte") { PackingListContent(ListUiState.Error(AppErrors.notFound()), null, {}, {}, {}) }
    }

    @Test
    fun `skärmdumpar - delvis, allt packat och nyskapad`() {
        captureLightAndDark("PackingListScreen_delvis") { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}) }
        captureLightAndDark("PackingListScreen_klar") {
            PackingListContent(ListUiState.Content(allChecked.toImmutableList()), header(allChecked, TripStatus.PACKED), {}, {}, {})
        }
        val other = rows.filter { it.item.section == PackingSection.FAMILY }
        captureLightAndDark("PackingListScreen_ovrigt") { PackingListContent(ListUiState.Content(other.toImmutableList()), header(other), {}, {}, {}) }
        val fresh = rows.map { it.copy(item = it.item.copy(checked = false, hidden = false, quantity = it.item.suggestedQuantity)) }
        captureLightAndDark("PackingListScreen_ny") { PackingListContent(ListUiState.Content(fresh.toImmutableList()), header(fresh), {}, {}, {}) }
    }

    @Test
    fun `skärmdumpar - en sektion hopfälld, Dolda visad och tom packlista`() {
        captureLightAndDark("PackingListScreen_hopfalld") {
            PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}, collapsed = setOf(PackingGroup.MEDICINE))
        }
        captureLightAndDark("PackingListScreen_tom") { PackingListContent(ListUiState.Empty, header(emptyList()), {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "w390dp-h1400dp-xxhdpi")
    fun `skärmdump - Dolda visad sist`() {
        val few = rows.filter { it.item.section == PackingSection.FAMILY }
        captureLightAndDark("PackingListScreen_dolda") {
            PackingListContent(ListUiState.Content(few.toImmutableList()), header(few), {}, {}, {}, archive = ListArchive(showing = true), hiddenCount = 1)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h900dp-xxhdpi")
    fun `skärmdump - 360 dp med 1 av 2 i Kosttillskott`() {
        captureLightAndDark("PackingListScreen_w360") {
            PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}, collapsed = setOf(PackingGroup.MEDICINE))
        }
    }

    @Test
    fun `skärmdump - lägg till i Övrigt, en sak`() = rule.captureScreenLightAndDark("PackingListScreen_lagg_till_ovrigt") {
        PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}, adding = newItem)
    }

    @Test
    fun `skärmdump - ändra mängd`() = rule.captureScreenLightAndDark("PackingListScreen_mangd") {
        PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}, editing = rows[4])
    }

    @Test
    fun `skärmdump - lägg till`() = rule.captureScreenLightAndDark("PackingListScreen_lagg_till") {
        PackingListContent(
            ListUiState.Content(rows.toImmutableList()),
            header(rows),
            {},
            {},
            {},
            adding = EditorUiState(PackingItem("ny", PackingSection.MEDICINE, "erik", PackingSource.MANUAL, name = "Nässpray", quantity = 1), isValid = true, isDirty = true)
        )
    }

    @Test
    fun `packat visas i rubriken`() {
        rule.setContent { ReseApoteketTheme { PackingListContent(ListUiState.Content(rows.toImmutableList()), header(rows), {}, {}, {}) } }
        assertEquals(PackingProgress(6, 12), header(rows).progress)
        rule.onNodeWithContentDescription("6 av 12 packat").assertIsDisplayed()
    }
}
