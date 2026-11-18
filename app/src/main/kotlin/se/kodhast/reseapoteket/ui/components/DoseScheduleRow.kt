package se.kodhast.reseapoteket.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.DayOfWeek
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.model.DoseSchedule
import se.kodhast.ui.common.DateFormat
import se.kodhast.ui.components.ChoiceChips
import se.kodhast.ui.components.FieldError
import se.kodhast.ui.components.QuantityStepper
import se.kodhast.ui.components.UnknownValueNotice
import se.kodhast.ui.theme.Spacing

/**
 * Dosering: typ (Dagligen · Veckodagar · Intervall · Vid behov) och dess antal. [unitName]
 * ger enhetens namn för ett antal ("tablett"/"tabletter") från `UnitNames` i `:core`.
 * En okänd typ från en nyare app visas som meddelande och kan inte ändras (BCK-5).
 * [error] (från formulärets validering) visas under fälten, som i `AppTextField`.
 */
@Composable
fun DoseScheduleRow(
    schedule: DoseSchedule,
    onChange: (DoseSchedule) -> Unit,
    unitName: (Int) -> String,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (schedule is DoseSchedule.Unknown) {
            UnknownValueNotice()
            return@Column
        }
        ChoiceChips(
            options = DOSE_KINDS,
            selected = DOSE_KINDS.first { it.matches(schedule) },
            onSelect = { onChange(it.switch(schedule)) },
            label = { stringResource(it.label) }
        )
        when (schedule) {
            is DoseSchedule.Daily -> {
                QuantityStepper(schedule.dosesPerDay, { onChange(schedule.copy(dosesPerDay = it)) }, stringResource(R.string.dose_doses_per_day), range = 1..24, showLabel = true)
                UnitsPerDose(schedule.unitsPerDose, unitName) { onChange(schedule.copy(unitsPerDose = it)) }
            }

            is DoseSchedule.Weekdays -> {
                ChoiceChips(
                    options = DayOfWeek.entries,
                    selected = schedule.days,
                    onToggle = { day -> onChange(schedule.copy(days = if (day in schedule.days) schedule.days - day else schedule.days + day)) },
                    label = { DateFormat.weekdayShort(it) }
                )
                UnitsPerDose(schedule.unitsPerDose, unitName) { onChange(schedule.copy(unitsPerDose = it)) }
            }

            is DoseSchedule.IntervalDays -> {
                QuantityStepper(schedule.intervalDays, { onChange(schedule.copy(intervalDays = it)) }, stringResource(R.string.dose_interval_days), range = 2..365, showLabel = true)
                UnitsPerDose(schedule.unitsPerDose, unitName) { onChange(schedule.copy(unitsPerDose = it)) }
            }

            is DoseSchedule.AsNeeded ->
                QuantityStepper(schedule.quantity, { onChange(schedule.copy(quantity = it)) }, stringResource(R.string.dose_as_needed_quantity, unitName(schedule.quantity)), range = 1..999, showLabel = true)

            is DoseSchedule.Unknown -> Unit
        }
        error?.let { FieldError(it) }
    }
}

@Composable
private fun UnitsPerDose(value: Int, unitName: (Int) -> String, onChange: (Int) -> Unit) = QuantityStepper(value, onChange, stringResource(R.string.dose_units_per_dose, unitName(value)), range = 1..99, showLabel = true)

/** Doseringstypens namn ("Dagligen", "Vid behov") – samma etikett som i väljaren; `null` för en okänd typ. */
@StringRes
fun DoseSchedule.kindLabel(): Int? = DOSE_KINDS.firstOrNull { it.matches(this) }?.label

/**
 * En valbar typ: hur den känns igen och hur man byter till den (antal per dos följer med).
 * Stegarna börjar där en dosering blir giltig (REC-2): intervall från 2 dagar, vid behov från 1.
 */
private enum class DoseKind(@param:StringRes val label: Int) {
    DAILY(R.string.dose_daily),
    WEEKDAYS(R.string.dose_weekdays),
    INTERVAL(R.string.dose_interval),
    AS_NEEDED(R.string.dose_as_needed);

    fun matches(schedule: DoseSchedule) = when (this) {
        DAILY -> schedule is DoseSchedule.Daily
        WEEKDAYS -> schedule is DoseSchedule.Weekdays
        INTERVAL -> schedule is DoseSchedule.IntervalDays
        AS_NEEDED -> schedule is DoseSchedule.AsNeeded
    }

    fun switch(from: DoseSchedule): DoseSchedule {
        if (matches(from)) return from
        val units = when (from) {
            is DoseSchedule.Daily -> from.unitsPerDose
            is DoseSchedule.Weekdays -> from.unitsPerDose
            is DoseSchedule.IntervalDays -> from.unitsPerDose
            else -> 1
        }
        return when (this) {
            DAILY -> DoseSchedule.Daily(unitsPerDose = units)
            WEEKDAYS -> DoseSchedule.Weekdays(unitsPerDose = units)
            INTERVAL -> DoseSchedule.IntervalDays(intervalDays = 2, unitsPerDose = units)
            AS_NEEDED -> DoseSchedule.AsNeeded(quantity = 1)
        }
    }
}

private val DOSE_KINDS = DoseKind.entries
