package se.kodhast.reseapoteket.ui.regimens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.kodhast.reseapoteket.core.engine.DoseScheduleFormatter
import se.kodhast.reseapoteket.core.model.Regimen
import se.kodhast.reseapoteket.core.model.displayName
import se.kodhast.reseapoteket.ui.components.kindLabel
import se.kodhast.reseapoteket.ui.theme.Blue
import se.kodhast.ui.components.PausableRow
import se.kodhast.ui.theme.Tone

/** Ett recept eller kosttillskott i en lista: namn och styrka, doseringen i klartext (REC-3) och typen. */
@Composable
fun RegimenRow(regimen: Regimen, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PausableRow(
        title = regimen.displayName,
        detail = DoseScheduleFormatter.format(regimen.schedule, regimen.unit),
        kind = regimen.schedule.kindLabel()?.let { stringResource(it) },
        active = regimen.active,
        onClick = onClick,
        pillTone = Tone.Blue,
        modifier = modifier
    )
}
