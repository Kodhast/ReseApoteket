package se.kodhast.reseapoteket.ui.packing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.engine.PackingGroup
import se.kodhast.reseapoteket.core.engine.PackingList
import se.kodhast.reseapoteket.core.engine.PackingProgress
import se.kodhast.reseapoteket.core.model.PackingItem
import se.kodhast.reseapoteket.core.model.PackingSection
import se.kodhast.reseapoteket.ui.components.AvatarStack
import se.kodhast.reseapoteket.ui.components.LocalTravelDayMorning
import se.kodhast.reseapoteket.ui.components.PackedProgress
import se.kodhast.reseapoteket.ui.components.StackedAvatar
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.reseapoteket.ui.theme.Green
import se.kodhast.ui.common.DateFormat
import se.kodhast.ui.common.EditorUiState
import se.kodhast.ui.common.ListEvent
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.common.UiError
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppIconButton
import se.kodhast.ui.components.AppMenuItem
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.CheckRow
import se.kodhast.ui.components.Confetti
import se.kodhast.ui.components.ConfirmDialog
import se.kodhast.ui.components.EmptyAction
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.GroupAction
import se.kodhast.ui.components.GroupFold
import se.kodhast.ui.components.InfoPill
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.ListArchive
import se.kodhast.ui.components.ListGroup
import se.kodhast.ui.components.ListSubgroup
import se.kodhast.ui.components.PillRow
import se.kodhast.ui.components.SwipeToHide
import se.kodhast.ui.components.collectAsListArchive
import se.kodhast.ui.theme.Spacing
import se.kodhast.ui.theme.Tone

/** Packlistan för en resa (PAK-1–7); pennan öppnar resans formulär (RES-1). */
@Composable
fun PackingListScreen(onBack: () -> Unit, onEditTrip: () -> Unit, viewModel: PackingListViewModel) {
    val state by viewModel.shown.collectAsStateWithLifecycle()
    val collapsed by viewModel.collapsed.collectAsStateWithLifecycle()
    val hiddenCount by viewModel.hiddenCount.collectAsStateWithLifecycle()
    val header by viewModel.header.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val adding by viewModel.adding.collectAsStateWithLifecycle()
    val addState by viewModel.addEditor.state.collectAsStateWithLifecycle()
    val addList by viewModel.addList.collectAsStateWithLifecycle()
    val askRecount by viewModel.askRecount.collectAsStateWithLifecycle()
    val sheetError by viewModel.sheetError.collectAsStateWithLifecycle()
    val celebrate by viewModel.celebrate.collectAsStateWithLifecycle()
    PackingListContent(
        state = state,
        header = header,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onEditTrip = onEditTrip,
        archive = viewModel.hiding.collectAsListArchive(),
        collapsed = collapsed,
        hiddenCount = hiddenCount,
        editing = editing,
        adding = addState.takeIf { adding },
        addList = addList,
        askRecount = askRecount,
        celebrate = celebrate,
        sheetError = sheetError
    )
}

@Composable
fun PackingListContent(
    state: ListUiState<PackingRow>,
    header: PackingHeader?,
    onEvent: (PackingListEvent) -> Unit,
    onBack: () -> Unit,
    onEditTrip: () -> Unit,
    modifier: Modifier = Modifier,
    archive: ListArchive = ListArchive(showToggle = false),
    collapsed: Set<PackingGroup?> = emptySet(),
    hiddenCount: Int = 0,
    editing: PackingRow? = null,
    adding: EditorUiState<PackingItem>? = null,
    addList: AddListState = AddListState(),
    askRecount: Boolean = false,
    celebrate: Boolean = false,
    sheetError: UiError? = null
) {
    val groups = packingGroups(header?.groups.orEmpty(), collapsed, onEvent)
    val sections = remember(groups) { listOf(PackingGroup.MEDICINE, PackingGroup.SUPPLEMENT, PackingGroup.FAMILY).map(groups::getValue) }
    val progress = header?.progress ?: PackingProgress.NONE
    var confetti by remember { mutableStateOf(false) }
    val event by rememberUpdatedState(onEvent)
    LaunchedEffect(celebrate) {
        if (celebrate) {
            confetti = true
            event(PackingListEvent.Celebrated)
        }
    }
    Box(modifier.fillMaxSize()) {
        EntityListScreen(
            title = header?.trip?.name.orEmpty(),
            state = state,
            empty = EmptyContent(
                icon = R.drawable.ic_luggage,
                title = stringResource(R.string.packing_empty_title),
                message = stringResource(R.string.packing_empty_message),
                actions = listOf(EmptyAction(stringResource(R.string.packing_recount), { onEvent(PackingListEvent.AskRecount) }, R.drawable.ic_refresh))
            ),
            add = null,
            key = { it.item.id },
            onRetry = { onEvent(PackingListEvent.List(ListEvent.Retry)) },
            group = { row -> groups.getValue(if (row.item.hidden) null else PackingList.group(row.item)) },
            // Visa dolda (N)/Göm dolda står i ⋮ bara när något är dolt eller visas (PAK-4).
            archive = archive.copy(
                showToggle = hiddenCount > 0 || archive.showing,
                showLabel = stringResource(R.string.packing_show_hidden_format, hiddenCount),
                hideLabel = stringResource(R.string.packing_hide_hidden),
                toggleIcon = R.drawable.ic_visibility_off
            ),
            groups = sections,
            // Utan resa (inte läst än, eller finns inte) finns inget att räkna om eller dölja.
            menu = if (header == null) {
                emptyList()
            } else {
                listOf(
                    AppMenuItem(stringResource(R.string.packing_recount), { onEvent(PackingListEvent.AskRecount) }, R.drawable.ic_refresh),
                    AppMenuItem(stringResource(R.string.packing_hide_checked), { onEvent(PackingListEvent.HideChecked) }, R.drawable.ic_visibility_off)
                )
            },
            subtitle = header?.let { subtitle(it) },
            onBack = onBack,
            actions = {
                // Utan resa (inte läst än, eller finns inte) finns inget att ändra.
                header?.let { h ->
                    AvatarStack(h.travellers.map { StackedAvatar(it.emoji, AppColors.person(it.colorHex), it.name) })
                    AppIconButton(R.drawable.ic_edit, stringResource(R.string.trip_edit), onEditTrip)
                }
            },
            header = header?.let {
                {
                    AppCard { PackedProgress(progress.packed, progress.total) }
                }
            },
            subgroup = { row ->
                row.person?.takeIf { !row.item.hidden && row.item.section != PackingSection.FAMILY }?.let { ListSubgroup(it.name, AppColors.person(it.colorHex)) }
            }
        ) { row -> PackingRowView(row, onEvent, archive) }
        if (confetti) Confetti(Modifier.fillMaxSize(), onFinished = { confetti = false })
    }
    editing?.let { QuantitySheet(it, onEvent, sheetError) }
    adding?.let { AddItemSheet(it, header?.travellers.orEmpty(), onEvent, sheetError, addList) }
    if (askRecount) {
        ConfirmDialog(
            title = stringResource(R.string.trip_recount_title),
            message = stringResource(R.string.packing_recount_message),
            confirmLabel = stringResource(R.string.trip_recount_confirm),
            onConfirm = { onEvent(PackingListEvent.Recount) },
            onDismiss = { onEvent(PackingListEvent.CancelRecount) }
        )
    }
}

/** "fre 2 okt 2026 · 7 dagar · 4 resenärer" (PAK-1). */
@Composable
private fun subtitle(header: PackingHeader): String {
    val trip = header.trip
    val days = pluralStringResource(R.plurals.trip_days, trip.days, trip.days)
    val travellers = pluralStringResource(R.plurals.packing_travellers, header.travellers.size, header.travellers.size)
    return listOfNotNull(trip.startDate?.let(DateFormat::display), days, travellers).joinToString(" · ")
}

/**
 * Rubrik per grupp med packat/totalt, hopfällbar med läget från appsessionen ([collapsed]); plus i
 * rubriken på Mediciner, Kosttillskott och Övrigt. "På resedagen" efter Övrigt och "Dolda" (`null`)
 * sist (PAK-1, PAK-4, PAK-6).
 */
@Composable
private fun packingGroups(progress: Map<PackingGroup, PackingProgress>, collapsed: Set<PackingGroup?>, onEvent: (PackingListEvent) -> Unit): Map<PackingGroup?, ListGroup> {
    val event by rememberUpdatedState(onEvent)
    val emptyText = stringResource(R.string.empty_generic)
    fun count(group: PackingGroup) = (progress[group] ?: PackingProgress.NONE).let { "${it.packed} / ${it.total}" }
    fun fold(group: PackingGroup?) = GroupFold(group !in collapsed) { event(PackingListEvent.ToggleGroup(group)) }

    @Composable
    fun add(section: PackingSection) = GroupAction(R.drawable.ic_add, stringResource(R.string.add_to_format, sectionName(section))) { event(PackingListEvent.Add(section)) }
    val medicines = ListGroup(stringResource(R.string.packing_medicines), R.drawable.ic_medication, Tone.Primary, count(PackingGroup.MEDICINE), action = add(PackingSection.MEDICINE), fold = fold(PackingGroup.MEDICINE), emptyText = emptyText)
    val supplements = ListGroup(stringResource(R.string.packing_supplements), R.drawable.ic_eco, Tone.Green, count(PackingGroup.SUPPLEMENT), action = add(PackingSection.SUPPLEMENT), fold = fold(PackingGroup.SUPPLEMENT), emptyText = emptyText)
    val other = ListGroup(stringResource(R.string.packing_other), R.drawable.ic_box, Tone.Blue, count(PackingGroup.FAMILY), columns = 2, action = add(PackingSection.FAMILY), fold = fold(PackingGroup.FAMILY), emptyText = emptyText)
    val travelDay = ListGroup(stringResource(R.string.packing_travel_day), R.drawable.ic_sun, Tone.Primary, count(PackingGroup.TRAVEL_DAY), fold = fold(PackingGroup.TRAVEL_DAY))
    val hidden = ListGroup(stringResource(R.string.packing_hidden), R.drawable.ic_visibility_off, Tone.Primary, fold = fold(null))
    return remember(medicines, supplements, other, travelDay, hidden) {
        mapOf(
            PackingGroup.MEDICINE to medicines,
            PackingGroup.SUPPLEMENT to supplements,
            PackingGroup.FAMILY to other,
            PackingGroup.TRAVEL_DAY to travelDay,
            null to hidden
        )
    }
}

/** En rad: kryss och mängd; tryck öppnar mängden, svep döljer. En dold rad har "Visa igen". */
@Composable
private fun PackingRowView(row: PackingRow, onEvent: (PackingListEvent) -> Unit, archive: ListArchive) {
    val item = row.item
    val amount = PackingList.amount(item)
    if (item.hidden) {
        ItemRow(
            item.name,
            subtitle = amount,
            trailing = { AppButton(stringResource(R.string.packing_show_again), { onEvent(PackingListEvent.Show(item.id)) }, variant = ButtonVariant.Text, compact = true) }
        )
        return
    }
    SwipeToHide(onHide = { archive.archive(item.id, item.name) }, label = stringResource(R.string.packing_hide)) { swipe ->
        CheckRow(
            item.name,
            item.checked,
            { onEvent(PackingListEvent.Check(item.id, it)) },
            swipe,
            subtitle = amount,
            trailing = labels(row),
            onClick = { onEvent(PackingListEvent.Open(item.id)) },
            onClickLabel = stringResource(R.string.packing_edit_quantity),
            accent = row.person?.takeIf { item.section != PackingSection.FAMILY }?.let { AppColors.person(it.colorHex) }
        )
    }
}

/**
 * Etiketterna på en rad: ändrad mängd eller dosering (PAK-2) och, för det som packas på
 * resedagen, när (egen tid eller resedagens morgon, PAK-6). Utan etikett ingen yta alls.
 */
private fun labels(row: PackingRow): (@Composable () -> Unit)? {
    val item = row.item
    val changed = PackingList.isChanged(item)
    if (!changed && row.schedule == null && !item.travelDay) return null
    return {
        PillRow {
            when {
                changed -> InfoPill(stringResource(R.string.packing_changed))
                row.schedule != null -> InfoPill(row.schedule, tone = Tone.Blue)
            }
            if (item.travelDay) InfoPill(DateFormat.time(item.travelDayTime ?: LocalTravelDayMorning.current))
        }
    }
}
