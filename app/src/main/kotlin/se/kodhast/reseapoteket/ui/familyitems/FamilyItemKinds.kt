package se.kodhast.reseapoteket.ui.familyitems

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.model.FamilyItem
import se.kodhast.reseapoteket.core.model.FamilyItemCategory
import se.kodhast.reseapoteket.core.model.ItemUnit
import se.kodhast.reseapoteket.core.model.QuantityRule

/** Kategorins namn i listan och formuläret (FAM-1). */
@StringRes
fun FamilyItemCategory.label(): Int = when (this) {
    FamilyItemCategory.CREAM -> R.string.category_cream
    FamilyItemCategory.HYGIENE -> R.string.category_hygiene
    FamilyItemCategory.OTHER -> R.string.category_other
}

/** Kategorins ikon i listans rubrik (FAM-1). */
@DrawableRes
fun FamilyItemCategory.icon(): Int = when (this) {
    FamilyItemCategory.CREAM -> R.drawable.ic_cream
    FamilyItemCategory.HYGIENE -> R.drawable.ic_hygiene
    FamilyItemCategory.OTHER -> R.drawable.ic_box
}

/**
 * Exemplen i den tomma listan (FAM-4): öppnar formuläret ifyllt med namn, kategori, enhet och
 * en vanlig mängd – användaren sparar själv. Namnet kommer från [label].
 */
enum class FamilyItemExample(
    @param:StringRes val label: Int,
    val category: FamilyItemCategory,
    val unit: ItemUnit,
    val rule: QuantityRule
) {
    SUNSCREEN(R.string.example_sunscreen, FamilyItemCategory.CREAM, ItemUnit.TUB, QuantityRule.PerDays(quantity = 1, perDays = 7)),
    TOOTHPASTE(R.string.example_toothpaste, FamilyItemCategory.HYGIENE, ItemUnit.TUB, QuantityRule.Fixed(quantity = 1)),
    THERMOMETER(R.string.example_thermometer, FamilyItemCategory.OTHER, ItemUnit.ST, QuantityRule.Fixed(quantity = 1));

    fun item(id: String, name: String): FamilyItem = FamilyItem(id, name, category, unit, rule)
}
