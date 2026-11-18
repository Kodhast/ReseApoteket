package se.kodhast.reseapoteket.ui.packing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.engine.PackingList
import se.kodhast.reseapoteket.core.engine.PastedLine
import se.kodhast.reseapoteket.core.engine.PastedStatus
import se.kodhast.reseapoteket.core.engine.UnitNames
import se.kodhast.reseapoteket.core.model.ItemUnit
import se.kodhast.reseapoteket.core.model.PackingItem
import se.kodhast.reseapoteket.core.model.PackingSection
import se.kodhast.reseapoteket.core.model.PackingSource
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.ui.components.PackingPreview
import se.kodhast.reseapoteket.ui.components.PersonChip
import se.kodhast.reseapoteket.ui.components.TravelDayField
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.ui.common.EditorUiState
import se.kodhast.ui.common.UiError
import se.kodhast.ui.components.AppBottomSheet
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.AppTextField
import se.kodhast.ui.components.ButtonVariant
import se.kodhast.ui.components.ChipRow
import se.kodhast.ui.components.InfoPill
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.LabeledGroup
import se.kodhast.ui.components.QuantityStepper
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Tone

/** Ändra mängd (PAK-3): stegare, förslaget med förklaringen och "Återställ till förslag". */
@Composable
internal fun QuantitySheet(row: PackingRow, onEvent: (PackingListEvent) -> Unit, error: UiError? = null) {
    val item = row.item
    AppBottomSheet(
        item.name,
        onDismiss = { onEvent(PackingListEvent.CloseQuantity) },
        error = error,
        footer = { AppButton(stringResource(R.string.packing_done), { onEvent(PackingListEvent.CloseQuantity) }, Modifier.fillMaxWidth()) }
    ) {
        listOfNotNull(row.person?.name, row.schedule).takeIf { it.isNotEmpty() }?.let {
            Text(it.joinToString(" · "), style = AppTheme.typography.itemSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        QuantityStepper(item.quantity, { onEvent(PackingListEvent.Quantity(it)) }, UnitNames.name(item.quantity, item.unit))
        if (item.sourceType != PackingSource.MANUAL) {
            val suggested = item.copy(quantity = item.suggestedQuantity)
            PackingPreview(stringResource(R.string.packing_suggestion), PackingList.amount(suggested), reason = item.reason.ifBlank { null })
        }
        if (PackingList.isChanged(item)) {
            AppButton(stringResource(R.string.packing_reset_format, item.suggestedQuantity), { onEvent(PackingListEvent.ResetQuantity) }, variant = ButtonVariant.Text)
        }
    }
}

/**
 * Lägg till i en sektion (PAK-4, PAK-7), öppnat från plus i sektionens rubrik: en egen rad – vad, vem
 * (för mediciner och kosttillskott) och hur många. Sektionen står i raden och frågas aldrig. Bara i
 * Övrigt kan man välja en inklistrad lista i stället ([list]).
 */
@Composable
internal fun AddItemSheet(
    state: EditorUiState<PackingItem>,
    travellers: List<Person>,
    onEvent: (PackingListEvent) -> Unit,
    error: UiError? = null,
    list: AddListState = AddListState()
) {
    val section = state.value.section
    val mode = if (section == PackingSection.FAMILY) list.mode else AddMode.ONE
    AppBottomSheet(
        stringResource(R.string.packing_add_to_format, sectionName(section)),
        onDismiss = { onEvent(PackingListEvent.CloseAdd) },
        error = error,
        footer = {
            when (mode) {
                AddMode.ONE -> AppButton(stringResource(R.string.add), { onEvent(PackingListEvent.AddSave) }, Modifier.fillMaxWidth(), enabled = state.canSave, loading = state.saving)
                AddMode.LIST -> AddListButton(list, onEvent)
            }
        }
    ) {
        if (section == PackingSection.FAMILY) {
            AppSegmentedChoice(
                listOf(stringResource(R.string.packing_add_one), stringResource(R.string.packing_add_list)),
                selectedIndex = mode.ordinal,
                onSelect = { onEvent(PackingListEvent.AddModeChange(AddMode.entries[it])) }
            )
        }
        when (mode) {
            AddMode.ONE -> AddOneContent(state, travellers, onEvent)
            AddMode.LIST -> AddListContent(list, onEvent)
        }
    }
}

/** En egen rad (PAK-4). */
@Composable
private fun AddOneContent(state: EditorUiState<PackingItem>, travellers: List<Person>, onEvent: (PackingListEvent) -> Unit) {
    val item = state.value
    AppTextField(
        item.name,
        { onEvent(PackingListEvent.AddName(it)) },
        stringResource(R.string.packing_what),
        error = state.errorFor(ManualItemValidator.NAME)?.let { stringResource(it) }
    )
    if (item.section != PackingSection.FAMILY) {
        LabeledGroup(stringResource(R.string.packing_who), error = state.errorFor(ManualItemValidator.PERSON)?.let { stringResource(it) }) {
            ChipRow {
                travellers.forEach { person ->
                    PersonChip(person.name, person.emoji, AppColors.person(person.colorHex), person.id == item.personId, { onEvent(PackingListEvent.AddPerson(person.id)) })
                }
            }
        }
    }
    LabeledGroup(stringResource(R.string.packing_how_many)) {
        QuantityStepper(item.quantity, { onEvent(PackingListEvent.AddQuantity(it)) }, UnitNames.name(item.quantity, item.unit), range = ManualItemValidator.QUANTITY_RANGE)
    }
    TravelDayField(item.travelDay, item.travelDayTime, { onEvent(PackingListEvent.AddTravelDay(it)) })
}

/** En inklistrad lista (PAK-7): en sak per rad och vad som läggs till under Övrigt; knappen står fast i arkets botten. */
@Composable
private fun AddListContent(list: AddListState, onEvent: (PackingListEvent) -> Unit) {
    AppTextField(
        list.text,
        { onEvent(PackingListEvent.AddListText(it)) },
        stringResource(R.string.packing_list_field),
        helper = stringResource(R.string.packing_list_helper),
        singleLine = false,
        maxLines = LIST_MAX_LINES
    )
    if (list.lines.isNotEmpty()) {
        LabeledGroup(stringResource(R.string.packing_list_preview)) {
            AppCard { list.lines.forEach { PastedRow(it) } }
        }
    }
}

/** "Lägg till 3 saker" – i arkets fasta knapprad, så att den syns även när listan är lång (#100). */
@Composable
private fun AddListButton(list: AddListState, onEvent: (PackingListEvent) -> Unit) {
    val count = list.toAdd.size
    AppButton(
        if (count == 0) stringResource(R.string.add) else pluralStringResource(R.plurals.packing_list_add, count, count),
        { onEvent(PackingListEvent.AddListSave) },
        Modifier.fillMaxWidth(),
        enabled = list.canSave,
        loading = list.saving
    )
}

/** Sektionens namn, som i packlistans rubriker. */
@Composable
internal fun sectionName(section: PackingSection): String = stringResource(
    when (section) {
        PackingSection.MEDICINE -> R.string.packing_medicines
        PackingSection.SUPPLEMENT -> R.string.packing_supplements
        PackingSection.FAMILY -> R.string.packing_other
    }
)

/** Så många rader visar listans fält; en längre lista scrollar i fältet (#100). */
private const val LIST_MAX_LINES = 8

/** En rad i förhandsvisningen: antalet i Övrigts ton, eller nedtonad med varför den inte läggs till. */
@Composable
private fun PastedRow(line: PastedLine) {
    val added = line.status == PastedStatus.ADD
    val label = when (line.status) {
        PastedStatus.ADD -> UnitNames.quantity(line.quantity, ItemUnit.ST)
        PastedStatus.EXISTS -> stringResource(R.string.packing_list_exists)
        PastedStatus.TOO_LONG -> stringResource(R.string.packing_list_too_long)
    }
    ItemRow(line.name, inactive = !added, tinted = true, trailing = { InfoPill(label, tone = Tone.Blue) })
}
