package se.kodhast.reseapoteket.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalTime
import se.kodhast.reseapoteket.core.model.DoseSchedule
import se.kodhast.reseapoteket.core.model.QuantityRule
import se.kodhast.reseapoteket.ui.theme.AppColors
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppDivider
import se.kodhast.ui.components.ChipRow
import se.kodhast.ui.components.ColorSwatchPicker
import se.kodhast.ui.components.EmojiPicker
import se.kodhast.ui.components.GroupLabel
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.navigation.gallery.ComponentGallery as LibraryGallery
import se.kodhast.ui.navigation.gallery.GallerySection

/**
 * Komponentgalleriet (android-ui `ComponentGallery`): bibliotekets sektioner med varje delad komponent,
 * sedan ReseApotekets egna – personerna, doseringen och packningen – med påhittad data. Nås från
 * Inställningar → "Komponenter" i debug-bygget. `UiConsistencyTest` kontrollerar att varje publik
 * komponent i appens `ui.components` finns här.
 */
@Composable
fun ComponentGallery(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LibraryGallery(
        onBack = onBack,
        modifier = modifier,
        appSections = listOf(
            GallerySection("Personer") { Persons() },
            GallerySection("Dosering och packning") { Packing() }
        )
    )
}

@Composable
private fun Persons() {
    val selected = remember { mutableStateOf(setOf("Anna", "Erik")) }
    var emoji by remember { mutableStateOf("🦉") }
    var color by remember { mutableStateOf(AppColors.PERSON_HEX[3]) }
    AppCard {
        ItemRow("Anna", subtitle = "3 recept · 1 kosttillskott", leading = { PersonAvatar("🦊", AppColors.person(0)) }, navigates = true, onClick = {})
        AppDivider()
        ItemRow(
            "Gran Canaria",
            subtitle = "fre 2 okt 2026 · 7 dagar",
            below = { AvatarStack(listOf(StackedAvatar("🦊", AppColors.person(0), "Anna"), StackedAvatar("🐻", AppColors.person(1), "Erik"))) }
        )
    }
    ChipRow {
        listOf("🦊", "🐻", "🐰", "🦉", "🐯", "🐢", "🦄", "🐧").forEachIndexed { i, avatar -> PersonAvatar(avatar, AppColors.person(i)) }
    }
    PersonAvatar("🦉", AppColors.person(3), size = AvatarSize.Large)
    ChipRow(wrap = false) {
        listOf("Anna" to "🦊", "Erik" to "🐻", "Lova" to "🐰").forEachIndexed { i, (person, avatar) ->
            PersonChip(person, avatar, AppColors.person(i), person in selected.value, {
                selected.value = if (person in selected.value) selected.value - person else selected.value + person
            })
        }
    }
    GroupLabel("Personens emoji")
    EmojiPicker(emoji, { emoji = it }, PERSON_EMOJIS)
    GroupLabel("Personens färg")
    ColorSwatchPicker(AppColors.PERSON_HEX, color, { color = it }, AppColors::person, { personColorLabel(it) }, PersonColorMarker)
}

@Composable
private fun Packing() {
    var schedule by remember { mutableStateOf<DoseSchedule>(DoseSchedule.Daily(dosesPerDay = 2)) }
    var rule by remember { mutableStateOf<QuantityRule>(QuantityRule.PerDays(1, 7)) }
    var travelDay by remember { mutableStateOf(true) }
    var travelTime by remember { mutableStateOf<LocalTime?>(null) }
    DoseScheduleRow(
        schedule,
        { schedule = it },
        unitName = { if (it == 1) "tablett" else "tabletter" },
        error = "Välj minst en veckodag".takeIf { (schedule as? DoseSchedule.Weekdays)?.days?.isEmpty() == true }
    )
    PackingPreview("För en 7-dagarsresa", "18 tabletter · 2 kartor", reason = "2 × 1 tablett/dag × (7 + 2) dagar")
    QuantityRuleRow(rule, { rule = it }, unitName = { if (it == 1) "tub" else "tuber" })
    TravelDayField(travelDay, travelTime, {
        travelDay = it.on
        travelTime = it.ownTime
    })
    AppCard { PackedProgress(packed = 9, total = 14) }
    AppCard { PackedProgress(packed = 14, total = 14) }
    PackedProgress(packed = 4, total = 10, compact = true)
}
