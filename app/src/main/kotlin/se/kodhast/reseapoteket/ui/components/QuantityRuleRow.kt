package se.kodhast.reseapoteket.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.core.model.QuantityRule
import se.kodhast.ui.components.AppSegmentedChoice
import se.kodhast.ui.components.FieldError
import se.kodhast.ui.components.QuantityStepper
import se.kodhast.ui.components.SegmentWidth
import se.kodhast.ui.components.UnknownValueNotice
import se.kodhast.ui.theme.Spacing

/**
 * Mängdregel för en familjeartikel: Fast antal · Per resenär · Per N dagar (segmentval) och dess antal.
 * [unitName] som i [DoseScheduleRow]; en okänd regel visas som meddelande (BCK-5).
 * [error] (från formulärets validering) visas under fälten.
 */
@Composable
fun QuantityRuleRow(
    rule: QuantityRule,
    onChange: (QuantityRule) -> Unit,
    unitName: (Int) -> String,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (rule is QuantityRule.Unknown) {
            UnknownValueNotice()
            return@Column
        }
        // Segment efter textens bredd: lika breda klipper "Per N dagar" på telefonen (android-ui #98).
        AppSegmentedChoice(
            options = RuleKind.entries.map { stringResource(it.label) },
            selectedIndex = RuleKind.entries.indexOfFirst { it.matches(rule) },
            onSelect = { onChange(RuleKind.entries[it].switch(rule)) },
            segmentWidth = SegmentWidth.FitText
        )
        when (rule) {
            is QuantityRule.Fixed ->
                QuantityStepper(rule.quantity, { onChange(rule.copy(quantity = it)) }, unitName(rule.quantity), range = 1..999, showLabel = true)

            is QuantityRule.PerTraveller ->
                QuantityStepper(rule.quantity, { onChange(rule.copy(quantity = it)) }, stringResource(R.string.rule_per_traveller_label, unitName(rule.quantity)), range = 1..999, showLabel = true)

            is QuantityRule.PerDays -> {
                QuantityStepper(rule.quantity, { onChange(rule.copy(quantity = it)) }, unitName(rule.quantity), range = 1..999, showLabel = true)
                QuantityStepper(rule.perDays, { onChange(rule.copy(perDays = it)) }, stringResource(R.string.rule_per_days_label, unitName(1)), range = 1..365, showLabel = true)
            }

            is QuantityRule.Unknown -> Unit
        }
        error?.let { FieldError(it) }
    }
}

/** Regeltypens namn ("Fast antal", "Per resenär") – samma etikett som i väljaren; `null` för en okänd typ. */
@StringRes
fun QuantityRule.kindLabel(): Int? = RuleKind.entries.firstOrNull { it.matches(this) }?.label

private enum class RuleKind(@param:StringRes val label: Int) {
    FIXED(R.string.rule_fixed),
    PER_TRAVELLER(R.string.rule_per_traveller),
    PER_DAYS(R.string.rule_per_days);

    fun matches(rule: QuantityRule) = when (this) {
        FIXED -> rule is QuantityRule.Fixed
        PER_TRAVELLER -> rule is QuantityRule.PerTraveller
        PER_DAYS -> rule is QuantityRule.PerDays
    }

    /** Byter typ och behåller antalet. */
    fun switch(from: QuantityRule): QuantityRule {
        if (matches(from)) return from
        val quantity = when (from) {
            is QuantityRule.Fixed -> from.quantity
            is QuantityRule.PerTraveller -> from.quantity
            is QuantityRule.PerDays -> from.quantity
            is QuantityRule.Unknown -> 1
        }
        return when (this) {
            FIXED -> QuantityRule.Fixed(quantity)
            PER_TRAVELLER -> QuantityRule.PerTraveller(quantity)
            PER_DAYS -> QuantityRule.PerDays(quantity)
        }
    }
}
