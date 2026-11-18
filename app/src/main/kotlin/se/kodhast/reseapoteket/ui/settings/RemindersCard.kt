package se.kodhast.reseapoteket.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.LocalDateTime
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.engine.ReminderKind
import se.kodhast.reseapoteket.core.engine.ReminderSetting
import se.kodhast.reseapoteket.core.engine.ReminderSettings
import se.kodhast.reseapoteket.ui.components.PersonChip
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.ui.common.DateFormat
import se.kodhast.ui.components.AppBottomSheet
import se.kodhast.ui.components.AppButton
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppDivider
import se.kodhast.ui.components.ChipRow
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.LabeledGroup
import se.kodhast.ui.components.NoticeBanner
import se.kodhast.ui.components.QuantityStepper
import se.kodhast.ui.components.SwitchRow
import se.kodhast.ui.components.TimeField
import se.kodhast.ui.components.ValueTile
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Spacing
import se.kodhast.ui.theme.Tone

@StringRes
private fun ReminderKind.title(): Int = when (this) {
    ReminderKind.NO_LIST -> R.string.reminder_no_list
    ReminderKind.TIME_TO_PACK -> R.string.reminder_time_to_pack
    ReminderKind.DAY_BEFORE -> R.string.reminder_day_before
    ReminderKind.TRAVEL_DAY -> R.string.reminder_travel_day
}

@StringRes
private fun ReminderKind.note(): Int = when (this) {
    ReminderKind.NO_LIST -> R.string.reminder_no_list_note
    ReminderKind.TIME_TO_PACK -> R.string.reminder_time_to_pack_note
    ReminderKind.DAY_BEFORE -> R.string.reminder_day_before_note
    ReminderKind.TRAVEL_DAY -> R.string.reminder_travel_day_note
}

/** "3 dagar före · 18:00", "18:00" eller "Av". */
@Composable
private fun summary(kind: ReminderKind, setting: ReminderSetting): String {
    if (!setting.enabled) return stringResource(R.string.reminder_off)
    val time = DateFormat.time(setting.time)
    if (!ReminderSettings.hasDaysBefore(kind)) return time
    return stringResource(R.string.reminder_at, pluralStringResource(R.plurals.reminder_days_before, setting.daysBefore, setting.daysBefore), time)
}

/**
 * Kortet Påminnelser i Inställningar (PÅM-1, PÅM-7): vilka personer telefonen påminner för och de
 * fyra påminnelserna. Tryck på en rad öppnar dess inställningar; behörigheten att visa notiser
 * frågas först när användaren slår på något. [notificationsAllowed] styr bannern "Notiser är avstängda" ovanför kortet.
 */
@Composable
fun RemindersCard(
    state: RemindersUiState,
    onEvent: (RemindersEvent) -> Unit,
    notificationsAllowed: Boolean,
    onAskPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by rememberSaveable { mutableStateOf<ReminderKind?>(null) }
    val change = { kind: ReminderKind, setting: ReminderSetting ->
        if (setting.enabled) onAskPermission()
        onEvent(RemindersEvent.Change(kind, setting))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        // Ovanför kortet; hela bannern öppnar systemets notisinställningar (PÅM-7).
        if (!notificationsAllowed) {
            NoticeBanner(
                stringResource(R.string.reminders_blocked),
                R.drawable.ic_notifications_off,
                onOpenNotificationSettings,
                onClickLabel = stringResource(R.string.reminders_enable),
                detail = stringResource(R.string.reminders_blocked_note)
            )
        }
        RemindersRows(state, onEvent, onAskPermission, change = change, onEdit = { editing = it })
    }
    editing?.let { kind -> ReminderSheet(kind, state, { change(kind, it) }, onClose = { editing = null }) }
}

@Composable
private fun RemindersRows(
    state: RemindersUiState,
    onEvent: (RemindersEvent) -> Unit,
    onAskPermission: () -> Unit,
    change: (ReminderKind, ReminderSetting) -> Unit,
    onEdit: (ReminderKind) -> Unit
) {
    AppCard {
        ItemRow(
            title = stringResource(R.string.reminders_persons),
            subtitle = stringResource(R.string.reminders_persons_note),
            below = {
                ChipRow {
                    state.persons.forEach { person ->
                        val chosen = person.id in state.settings.personIds
                        PersonChip(person.name, person.emoji, AppColors.person(person.colorHex), chosen, {
                            if (!chosen) onAskPermission()
                            onEvent(RemindersEvent.TogglePerson(person.id))
                        })
                    }
                }
            }
        )
        AppDivider()
        ReminderKind.entries.forEach { kind ->
            val setting = state.settings[kind]
            SwitchRow(
                stringResource(kind.title()),
                setting.enabled,
                { change(kind, setting.copy(enabled = it)) },
                subtitle = summary(kind, setting),
                onClick = { onEdit(kind) },
                onClickLabel = stringResource(R.string.reminder_open_settings)
            )
        }
    }
}

/** En påminnelses inställningar: på/av, dagar före (där det går), tid och när nästa kommer. */
@Composable
private fun ReminderSheet(kind: ReminderKind, state: RemindersUiState, onChange: (ReminderSetting) -> Unit, onClose: () -> Unit) {
    val setting = state.settings[kind]
    AppBottomSheet(
        stringResource(kind.title()),
        onDismiss = onClose,
        footer = { AppButton(stringResource(R.string.done), onClose, Modifier.fillMaxWidth()) },
        subtitle = stringResource(kind.note())
    ) {
        SwitchRow(stringResource(kind.title()), setting.enabled, { onChange(setting.copy(enabled = it)) })
        if (ReminderSettings.hasDaysBefore(kind)) {
            LabeledGroup(stringResource(R.string.reminder_days_before_label)) {
                QuantityStepper(
                    setting.daysBefore,
                    { onChange(setting.copy(daysBefore = it)) },
                    pluralStringResource(R.plurals.reminder_days_before, setting.daysBefore, setting.daysBefore),
                    range = ReminderSettings.DAYS_BEFORE,
                    enabled = setting.enabled
                )
            }
        }
        TimeField(stringResource(R.string.reminder_time_label), setting.time, { onChange(setting.copy(time = it)) })
        val next = state.next[kind]
        ValueTile(
            label = stringResource(R.string.reminder_next),
            value = next?.let { nextAt(it.at) } ?: stringResource(R.string.reminder_next_none),
            supporting = next?.tripName,
            tone = Tone.Blue
        )
    }
}

@Composable
private fun nextAt(at: LocalDateTime): String = stringResource(R.string.reminder_next_at, DateFormat.display(at.date), DateFormat.time(at.time))
