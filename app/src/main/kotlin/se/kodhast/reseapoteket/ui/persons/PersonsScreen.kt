package se.kodhast.reseapoteket.ui.persons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.model.Archivable
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.RegimenKind
import se.kodhast.reseapoteket.core.model.listed
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.combineByKey
import se.kodhast.reseapoteket.data.repository.PersonRepository
import se.kodhast.reseapoteket.data.repository.RegimenRepository
import se.kodhast.reseapoteket.ui.components.PersonAvatar
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.ui.common.ListEvent
import se.kodhast.ui.common.ListLoader
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.ArchiveActions
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.ListArchive
import se.kodhast.ui.components.SwipeToHide
import se.kodhast.ui.components.collectAsListArchive

/** En rad i personlistan: personen och hur många recept och kosttillskott den har (PER-1). */
data class PersonListItem(val person: Person, val medicines: Int = 0, val supplements: Int = 0)

sealed interface PersonsEvent {
    data class List(val event: ListEvent) : PersonsEvent
}

@HiltViewModel
class PersonsViewModel @Inject constructor(
    persons: PersonRepository,
    regimens: RegimenRepository
) : ViewModel() {
    /** Svep-arkivera, Ångra och "Visa arkiverade" (PER-3) – samma som i alla listor. */
    val archive = ArchiveActions(viewModelScope, AppErrors, persons::setArchived)

    // Aktiva först; arkiverade sist och bara när de visas (PER-1, PER-3).
    private val items: Flow<List<PersonListItem>> =
        archive.visible(persons.observe(), Archivable::archived).combineByKey({ list -> list.map { it.id }.toSet() }, { ids -> regimens.regimensFor(ids.toList()) }) { list, byPerson ->
            list.map { person ->
                val own = byPerson[person.id].orEmpty()
                PersonListItem(person, own.listed(RegimenKind.MEDICINE).size, own.listed(RegimenKind.SUPPLEMENT).size)
            }
        }

    private val loader = ListLoader(items, viewModelScope, AppErrors)
    val state = loader.state

    fun onEvent(event: PersonsEvent) {
        when (event) {
            is PersonsEvent.List -> loader.onEvent(event.event)
        }
    }
}

/** Fliken Personer (PER-1, PER-3). En ny person läggs till med plusknappen i flikraden (NAV-6). */
@Composable
fun PersonsScreen(onOpen: (String) -> Unit, viewModel: PersonsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PersonsContent(state, viewModel::onEvent, onOpen, archive = viewModel.archive.collectAsListArchive())
}

@Composable
fun PersonsContent(
    state: ListUiState<PersonListItem>,
    onEvent: (PersonsEvent) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    archive: ListArchive = ListArchive()
) {
    EntityListScreen(
        title = stringResource(R.string.tab_persons),
        state = state,
        empty = EmptyContent(
            icon = R.drawable.ic_person,
            title = stringResource(R.string.persons_empty_title),
            message = stringResource(R.string.persons_empty_message)
        ),
        // Ny läggs till med plusknappen i flikraden (NAV-6).
        add = null,
        key = { it.person.id },
        modifier = modifier,
        onRetry = { onEvent(PersonsEvent.List(ListEvent.Retry)) },
        archive = archive
    ) { item ->
        val person = item.person
        SwipeToHide(onHide = { archive.archive(person.id, person.name) }, enabled = !person.archived) { swipe ->
            PersonRow(item, swipe, onOpen = { onOpen(person.id) })
        }
    }
}

@Composable
private fun PersonRow(item: PersonListItem, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val person = item.person
    ItemRow(
        title = person.name,
        modifier = modifier,
        subtitle = if (person.archived) stringResource(R.string.archived_state) else personSummary(item.medicines, item.supplements),
        leading = { PersonAvatar(person.emoji, AppColors.person(person.colorHex)) },
        onClick = onOpen,
        navigates = true
    )
}

/** "3 recept · 1 kosttillskott", eller att personen inte har något än – samma text i lista och detalj. */
@Composable
internal fun personSummary(medicines: Int, supplements: Int): String {
    val parts = listOfNotNull(
        medicines.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.person_medicines, it, it) },
        supplements.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.person_supplements, it, it) }
    )
    return if (parts.isEmpty()) stringResource(R.string.person_nothing_yet) else parts.joinToString(" · ")
}
