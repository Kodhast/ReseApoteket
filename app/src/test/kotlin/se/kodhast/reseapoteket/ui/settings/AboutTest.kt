package se.kodhast.reseapoteket.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.reseapoteket.testing.FakeMedicines
import se.kodhast.reseapoteket.testing.MainDispatcherRule
import se.kodhast.reseapoteket.testing.captureLightAndDark
import se.kodhast.reseapoteket.ui.theme.ReseApoteketTheme

/** Om appen (SET-6): version, läkemedelslistans datum och källa, signaturen. */
@RunWith(RobolectricTestRunner::class)
class AboutTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val updated = LocalDate(2026, 10, 4)

    @Test
    fun `signaturen En kodhäst-app står under versionen (SET-6)`() {
        rule.setContent { ReseApoteketTheme { AboutContent("0.1.0") } }
        val version = rule.onNodeWithText("Version 0.1.0", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        val signature = rule.onNodeWithText("En kodhäst-app", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(signature.top >= version.bottom)
        rule.onNodeWithText("Om ReseApoteket").assertIsDisplayed()
    }

    @Test
    fun `läkemedelslistans datum och källa står under Om (REC-6)`() = runTest {
        assertEquals(updated, AboutViewModel(FakeMedicines()).medicinesUpdated.first { it != null })
        rule.setContent { ReseApoteketTheme { AboutContent("0.1.0", medicinesUpdated = updated) } }
        rule.onNodeWithText("Läkemedelsverket, sön 4 okt 2026 · CC BY 4.0").assertIsDisplayed()
        rule.onNodeWithText("github.com/Kodhast/ReseApoteket").assertIsDisplayed()
    }

    @Test
    fun `Om appen`() = captureLightAndDark("AboutScreen_standard") {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { AboutContent("0.16.0", onBack = {}, medicinesUpdated = updated) }
    }
}
