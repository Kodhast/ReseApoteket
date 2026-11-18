package se.kodhast.reseapoteket.ui.familyitems

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.engine.QuantityRuleFormatter
import se.kodhast.reseapoteket.core.model.Archivable
import se.kodhast.reseapoteket.core.model.FamilyItem
import se.kodhast.reseapoteket.core.model.FamilyItemCategory
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.repository.FamilyItemRepository
import se.kodhast.reseapoteket.ui.common.ListSession
import se.kodhast.reseapoteket.ui.components.kindLabel
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.ui.common.ListEvent
import se.kodhast.ui.common.ListLoader
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.ArchiveActions
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EmptyExample
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.GroupAction
import se.kodhast.ui.components.GroupFold
import se.kodhast.ui.components.ListArchive
import se.kodhast.ui.components.ListGroup
import se.kodhast.ui.components.PausableRow
import se.kodhast.ui.components.SwipeToHide
import se.kodhast.ui.components.collectAsListArchive
import se.kodhast.ui.theme.Tone

sealed interface FamilyItemsEvent {
    data class List(val event: ListEvent) : FamilyItemsEvent

    /** Fäll ihop eller ut en kategori (FAM-1). */
    data class ToggleCategory(val category: FamilyItemCategory) : FamilyItemsEvent
}

@HiltViewModel
class FamilyItemsViewModel @Inject constructor(items: FamilyItemRepository, private val session: ListSession) : ViewModel() {
    /** Svep-arkivera, Ångra och "Visa arkiverade" (FAM-2) – samma som i alla listor. */
    val archive = ArchiveActions(viewModelScope, AppErrors, items::setArchived)

    // Per kategori i fast ordning (FAM-1); inom kategorin aktiva först, sedan i sparad ordning.
    private val loader = ListLoader(archive.visible(items.observe(), Archivable::archived).map { list -> list.sortedBy { it.category.ordinal } }, viewModelScope, AppErrors)
    val state = loader.state

    /** Hopfällda kategorier under appsessionen (FAM-1). */
    val collapsed: StateFlow<Set<FamilyItemCategory>> = session.familyCollapsed.asStateFlow()

    fun onEvent(event: FamilyItemsEvent) {
        when (event) {
            is FamilyItemsEvent.List -> loader.onEvent(event.event)
            is FamilyItemsEvent.ToggleCategory -> session.familyCollapsed.update { if (event.category in it) it - event.category else it + event.category }
        }
    }
}

/**
 * Fliken Gemensamt (FAM-1–4). En ny sak läggs till med plus i en kategori ([onAdd]), med plusknappen i
 * flikraden (NAV-6) eller från ett exempel i tomt läge (FAM-4).
 */
@Composable
fun FamilyItemsScreen(
    onAddExample: (FamilyItemExample) -> Unit,
    onAdd: (FamilyItemCategory) -> Unit,
    onOpen: (String) -> Unit,
    viewModel: FamilyItemsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val collapsed by viewModel.collapsed.collectAsStateWithLifecycle()
    FamilyItemsContent(state, viewModel::onEvent, onOpen, archive = viewModel.archive.collectAsListArchive(), onAddExample = onAddExample, onAdd = onAdd, collapsed = collapsed)
}

@Composable
fun FamilyItemsContent(
    state: ListUiState<FamilyItem>,
    onEvent: (FamilyItemsEvent) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    archive: ListArchive = ListArchive(),
    onAddExample: (FamilyItemExample) -> Unit = {},
    onAdd: (FamilyItemCategory) -> Unit = {},
    collapsed: Set<FamilyItemCategory> = emptySet()
) {
    val examples = FamilyItemExample.entries.map { example -> EmptyExample(stringResource(example.label)) { onAddExample(example) } }
    val groups = categoryGroups(collapsed, onEvent, onAdd)
    val group = remember(groups) { { item: FamilyItem -> groups.getValue(item.category) } }
    val all = remember(groups) { FamilyItemCategory.entries.map(groups::getValue) }
    EntityListScreen(
        title = stringResource(R.string.tab_family_items),
        state = state,
        empty = EmptyContent(
            icon = R.drawable.ic_family_items,
            title = stringResource(R.string.family_items_empty_title),
            message = stringResource(R.string.family_items_empty_message),
            examples = examples
        ),
        // Ny läggs till med plusknappen i flikraden (NAV-6).
        add = null,
        key = FamilyItem::id,
        modifier = modifier,
        onRetry = { onEvent(FamilyItemsEvent.List(ListEvent.Retry)) },
        group = group,
        archive = archive,
        // Alla tre kategorierna visas när listan har innehåll, en tom med sitt plus (FAM-1); helt tom
        // lista visar exemplen (FAM-4).
        groups = if (state is ListUiState.Content) all else emptyList()
    ) { item ->
        SwipeToHide(onHide = { archive.archive(item.id, item.name) }, enabled = !item.archived) { swipe ->
            FamilyItemRow(item, swipe, onOpen = { onOpen(item.id) })
        }
    }
}

/**
 * Ett hopfällbart kort per kategori, med ikon och plus ("Lägg till i Hygien"), i Övrigt-tonen; läget
 * från appsessionen ([collapsed]) (FAM-1).
 */
@Composable
private fun categoryGroups(collapsed: Set<FamilyItemCategory>, onEvent: (FamilyItemsEvent) -> Unit, onAdd: (FamilyItemCategory) -> Unit): Map<FamilyItemCategory, ListGroup> {
    val event by rememberUpdatedState(onEvent)
    val add by rememberUpdatedState(onAdd)
    val emptyText = stringResource(R.string.empty_generic)
    return FamilyItemCategory.entries.associateWith { category ->
        val title = stringResource(category.label())
        ListGroup(
            title,
            category.icon(),
            Tone.Blue,
            action = GroupAction(R.drawable.ic_add, stringResource(R.string.add_to_format, title)) { add(category) },
            fold = GroupFold(category !in collapsed) { event(FamilyItemsEvent.ToggleCategory(category)) },
            emptyText = emptyText
        )
    }
}

/** Namn, mängdregeln i klartext och regeltypen (FAM-3); pausad nedtonad som recepten. */
@Composable
private fun FamilyItemRow(item: FamilyItem, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    PausableRow(
        title = item.name,
        detail = if (item.archived) stringResource(R.string.archived_state) else QuantityRuleFormatter.format(item.rule, item.unit),
        kind = item.rule.kindLabel()?.let { stringResource(it) },
        // En arkiverad visas som arkiverad även om den är pausad – den återställs från formuläret.
        active = item.active || item.archived,
        onClick = onOpen,
        pillTone = Tone.Blue,
        modifier = modifier
    )
}
