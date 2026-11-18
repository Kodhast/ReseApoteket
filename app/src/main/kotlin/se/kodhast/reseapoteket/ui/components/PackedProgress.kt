package se.kodhast.reseapoteket.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import se.kodhast.reseapoteket.R
import se.kodhast.ui.components.AppProgress

/**
 * Packprogressen (PAK-5): bibliotekets [AppProgress] med packlistans texter – "9 av 14 packat" och, när allt
 * är packat, "14 av 14 · allt packat" med temats gröna fyllnad (`ExtendedColors.progressDone`). [compact] ger
 * ett kort spår och "4 / 10" för en rad i en lista.
 */
@Composable
fun PackedProgress(packed: Int, total: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    val resources = LocalResources.current
    AppProgress(
        done = packed,
        total = total,
        label = { done, all -> resources.getString(R.string.packed_progress, done, all) },
        doneLabel = { all -> resources.getString(R.string.packed_all, all) },
        modifier = modifier,
        compact = compact
    )
}
