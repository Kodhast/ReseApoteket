package se.kodhast.reseapoteket.ui.familyitems

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.engine.QuantityRuleFormatter
import se.kodhast.reseapoteket.core.model.Archivable
import se.kodhast.reseapoteket.core.model.FamilyItem
import se.kodhast.reseapoteket.core.model.FamilyItemCategory
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.repository.FamilyItemRepository
import se.kodhast.reseapoteket.ui.components.kindLabel
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.ui.common.ListEvent
import se.kodhast.ui.common.ListLoader
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.ArchiveActions
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EmptyExample
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.ListArchive
import se.kodhast.ui.components.ListGroup
import se.kodhast.ui.components.PausableRow
import se.kodhast.ui.components.SwipeToHide
import se.kodhast.ui.components.collectAsListArchive
import se.kodhast.ui.theme.Tone

sealed interface FamilyItemsEvent {
    data class List(val event: ListEvent) : FamilyItemsEvent
}

@HiltViewModel
class FamilyItemsViewModel @Inject constructor(items: FamilyItemRepository) : ViewModel() {
    /** Svep-arkivera, Ångra och "Visa arkiverade" (FAM-2) – samma som i alla listor. */
    val archive = ArchiveActions(viewModelScope, AppErrors, items::setArchived)

    // Per kategori i fast ordning (FAM-1); inom kategorin aktiva först, sedan i sparad ordning.
    private val loader = ListLoader(archive.visible(items.observe(), Archivable::archived).map { list -> list.sortedBy { it.category.ordinal } }, viewModelScope, AppErrors)
    val state = loader.state

    fun onEvent(event: FamilyItemsEvent) {
        when (event) {
            is FamilyItemsEvent.List -> loader.onEvent(event.event)
        }
    }
}

/** Fliken Gemensamt (FAM-1–4). En ny sak läggs till med plusknappen i flikraden (NAV-6) eller från ett exempel i tomt läge (FAM-4). */
@Composable
fun FamilyItemsScreen(
    onAddExample: (FamilyItemExample) -> Unit,
    onOpen: (String) -> Unit,
    viewModel: FamilyItemsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FamilyItemsContent(state, viewModel::onEvent, onOpen, archive = viewModel.archive.collectAsListArchive(), onAddExample = onAddExample)
}

@Composable
fun FamilyItemsContent(
    state: ListUiState<FamilyItem>,
    onEvent: (FamilyItemsEvent) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    archive: ListArchive = ListArchive(),
    onAddExample: (FamilyItemExample) -> Unit = {}
) {
    val examples = FamilyItemExample.entries.map { example -> EmptyExample(stringResource(example.label)) { onAddExample(example) } }
    val groups = categoryGroups()
    val group = remember(groups) { { item: FamilyItem -> groups.getValue(item.category) } }
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
        archive = archive
    ) { item ->
        SwipeToHide(onHide = { archive.archive(item.id, item.name) }, enabled = !item.archived) { swipe ->
            FamilyItemRow(item, swipe, onOpen = { onOpen(item.id) })
        }
    }
}

/** En rubrik per kategori, med ikon, i Övrigt-tonen (FAM-1). */
@Composable
private fun categoryGroups(): Map<FamilyItemCategory, ListGroup> = FamilyItemCategory.entries.associateWith { ListGroup(stringResource(it.label()), it.icon(), Tone.Blue) }

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
