package se.kodhast.reseapoteket.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.ui.components.ColorSwatchPicker
import se.kodhast.ui.components.SwatchMarker

/** Personfärgernas markering i [ColorSwatchPicker]: ring och vit bock på den valda färgen. */
internal val PersonColorMarker: SwatchMarker = SwatchMarker.RingAndCheck(Color.White)

/** Personfärgens namn för TalkBack ("Färg korall") – det som [ColorSwatchPicker] läser för en `AppColors.PERSON_HEX`. */
@Composable
internal fun personColorLabel(hex: String): String {
    val names = stringArrayResource(R.array.person_color_names)
    return stringResource(R.string.person_color_format, names.getOrElse(AppColors.PERSON_HEX.indexOf(hex)) { "" })
}
