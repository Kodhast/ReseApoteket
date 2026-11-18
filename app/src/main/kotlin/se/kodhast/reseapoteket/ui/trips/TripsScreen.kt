package se.kodhast.reseapoteket.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.engine.PackingEngine
import se.kodhast.reseapoteket.core.engine.PackingProgress
import se.kodhast.reseapoteket.core.engine.SwedishOrder
import se.kodhast.reseapoteket.core.model.Archivable
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Trip
import se.kodhast.reseapoteket.core.model.TripStatus
import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.combineByKey
import se.kodhast.reseapoteket.data.common.withFallback
import se.kodhast.reseapoteket.data.repository.PackingRepository
import se.kodhast.reseapoteket.data.repository.PersonRepository
import se.kodhast.reseapoteket.data.repository.TripRepository
import se.kodhast.reseapoteket.ui.components.AvatarStack
import se.kodhast.reseapoteket.ui.components.StackedAvatar
import se.kodhast.reseapoteket.ui.components.SuitcaseProgress
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.reseapoteket.ui.theme.Green
import se.kodhast.ui.common.DateFormat
import se.kodhast.ui.common.ListEvent
import se.kodhast.ui.common.ListLoader
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.components.ArchiveActions
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.InfoPill
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.ListArchive
import se.kodhast.ui.components.SwipeToHide
import se.kodhast.ui.components.collectAsListArchive
import se.kodhast.ui.theme.Tone

/** En rad i reselistan: resan, resenärerna och hur mycket som är packat (RES-1). */
data class TripListItem(val trip: Trip, val travellers: List<Person> = emptyList(), val progress: PackingProgress = PackingProgress.NONE)

sealed interface TripsEvent {
    data class List(val event: ListEvent) : TripsEvent
}

@HiltViewModel
class TripsViewModel @Inject constructor(
    trips: TripRepository,
    persons: PersonRepository,
    packing: PackingRepository
) : ViewModel() {
    /** Svep-arkivera, Ångra och "Visa arkiverade" (RES-1) – samma som i alla listor. */
    val archive = ArchiveActions(viewModelScope, AppErrors, trips::setArchived)

    // Senaste start först; resor utan startdatum sist (RES-1). Arkiverade sist och bara när de
    // visas (ArchiveActions behåller ordningen i övrigt). Packlistorna följs per uppsättning resor –
    // inte om när en resa ändras eller flyttas. Resenärer och packat är tillägg: går de inte att
    // läsa visas resorna ändå, utan avatarer eller packat.
    private val items: Flow<List<TripListItem>> =
        archive.visible(trips.observe().map { it.sortedWith(NEWEST_FIRST) }, Archivable::archived)
            .combineByKey({ list -> list.map { it.id }.toSet() }, { ids -> progressFor(packing, ids) }, ::Pair)
            .combine(persons.observe().withFallback(emptyList())) { (list, progress), people ->
                val byId = people.associateBy { it.id }
                list.map { trip -> TripListItem(trip, trip.travellerIds.mapNotNull(byId::get), progress[trip.id] ?: PackingProgress.NONE) }
            }

    private val loader = ListLoader(items, viewModelScope, AppErrors)
    val state = loader.state

    fun onEvent(event: TripsEvent) {
        when (event) {
            is TripsEvent.List -> loader.onEvent(event.event)
        }
    }

    private companion object {
        val NEWEST_FIRST: Comparator<Trip> = compareByDescending<Trip> { it.startDate }.thenBy(SwedishOrder.names) { it.name }

        fun progressFor(packing: PackingRepository, ids: Set<String>): Flow<Map<String, PackingProgress>> = packing.packingFor(ids.toList())
            .map { byTrip -> byTrip.mapValues { PackingEngine.progress(it.value) } }
            .withFallback(emptyMap())
    }
}

/** Fliken Resor – appens startskärm (NAV-2, RES-1). En ny resa läggs till med plusknappen i flikraden (NAV-6). */
@Composable
fun TripsScreen(onOpen: (String) -> Unit, viewModel: TripsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TripsContent(state, viewModel::onEvent, onOpen, archive = viewModel.archive.collectAsListArchive())
}

@Composable
fun TripsContent(
    state: ListUiState<TripListItem>,
    onEvent: (TripsEvent) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    archive: ListArchive = ListArchive()
) {
    EntityListScreen(
        title = stringResource(R.string.tab_trips),
        state = state,
        empty = EmptyContent(
            icon = R.drawable.ic_luggage,
            title = stringResource(R.string.trips_empty_title),
            message = stringResource(R.string.trips_empty_message)
        ),
        // Ny läggs till med plusknappen i flikraden (NAV-6).
        add = null,
        key = { it.trip.id },
        modifier = modifier,
        onRetry = { onEvent(TripsEvent.List(ListEvent.Retry)) },
        archive = archive
    ) { item ->
        val trip = item.trip
        SwipeToHide(onHide = { archive.archive(trip.id, trip.name) }, enabled = !trip.archived) { swipe ->
            TripRow(item, swipe, onOpen = { onOpen(trip.id) })
        }
    }
}

/** Namn, startdatum och dagar, status, resenärerna och packat (RES-1). En klar resa är nedtonad. */
@Composable
private fun TripRow(item: TripListItem, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val trip = item.trip
    val days = pluralStringResource(R.plurals.trip_days, trip.days, trip.days)
    val subtitle = when {
        trip.archived -> stringResource(R.string.archived_state)
        trip.startDate != null -> stringResource(R.string.trip_date_days_format, DateFormat.display(trip.startDate!!), days)
        else -> days
    }
    val (status, tone) = trip.status.label()
    ItemRow(
        title = trip.name,
        modifier = modifier,
        subtitle = subtitle,
        trailing = { InfoPill(stringResource(status), tone = tone) },
        onClick = onOpen,
        navigates = true,
        inactive = trip.status == TripStatus.DONE,
        below = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AvatarStack(item.travellers.map { StackedAvatar(it.emoji, AppColors.person(it.colorHex), it.name) })
                if (item.progress.total > 0) SuitcaseProgress(item.progress.packed, item.progress.total, compact = true)
            }
        }
    )
}

/** Statusens namn och ton i listan (RES-1). */
private fun TripStatus.label(): Pair<Int, Tone> = when (this) {
    TripStatus.PLANNING -> R.string.trip_status_planning to Tone.Blue
    TripStatus.PACKED -> R.string.trip_status_packed to Tone.Green
    TripStatus.DONE -> R.string.trip_status_done to Tone.Primary
}
