package se.kodhast.reseapoteket.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.kodhast.reseapoteket.R
import se.kodhast.ui.components.AppLoading
import se.kodhast.ui.components.EmptyState
import se.kodhast.ui.components.SectionHeader

/** Kodhäst-igenkänningen (NFR-22): temat slår på bibliotekets delar, och startskärmens färger är appens. */
@RunWith(RobolectricTestRunner::class)
class BrandStyleTest {

    @get:Rule
    val rule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `fullstort tomt läge visar hästen, felvyn sin ikon (NFR-22)`() {
        rule.setContent {
            ReseApoteketTheme {
                EmptyState(R.drawable.ic_family_items, "Inga familjeartiklar än", "Solkräm och plåster.")
                EmptyState(R.drawable.ic_family_items, "Något gick fel", "Försök igen.", isError = true)
            }
        }
        rule.onAllNodesWithTag(EMPTY_STATE_MARK, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `sektionsrubrik utan ikonruta får prickarna, med ikonruta inte (NFR-22)`() {
        rule.setContent {
            ReseApoteketTheme {
                SectionHeader("Krämer och salvor")
                SectionHeader("Recept", icon = R.drawable.ic_family_items)
            }
        }
        rule.onAllNodesWithTag(SECTION_DOTS, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `laddningen läses som Laddar (NFR-22)`() {
        rule.setContent { ReseApoteketTheme { AppLoading() } }
        rule.onNodeWithContentDescription("Laddar").assertExists()
    }

    @Test
    fun `startskärmens ordmärke har appens färger i ljust läge (NFR-22)`() {
        assertEquals(AppColors.light.onSurfaceVariant.toArgb(), context.getColor(R.color.kodhast_branding_text))
        assertEquals(AppColors.light.primary.toArgb(), context.getColor(R.color.kodhast_branding_dots))
    }

    @Test
    @Config(qualifiers = "night")
    fun `startskärmens ordmärke har appens färger i mörkt läge (NFR-22)`() {
        assertEquals(AppColors.dark.onSurfaceVariant.toArgb(), context.getColor(R.color.kodhast_branding_text))
        assertEquals(AppColors.dark.primary.toArgb(), context.getColor(R.color.kodhast_branding_dots))
    }

    private companion object {
        // Bibliotekets testtaggar (internal i android-ui 1.1.0).
        const val EMPTY_STATE_MARK = "empty-state-mark"
        const val SECTION_DOTS = "section-dots"
    }
}
