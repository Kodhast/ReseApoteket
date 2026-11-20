package se.kodhast.reseapoteket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.reseapoteket.R
import se.kodhast.reseapoteket.testing.captureLightAndDark
import se.kodhast.reseapoteket.ui.theme.ReseApoteketTheme
import se.kodhast.ui.components.APP_HEADER_VERSION_TAG
import se.kodhast.ui.components.AppHeader
import se.kodhast.ui.components.AppIdentity
import se.kodhast.ui.components.LocalAppIdentity

/** Versionen i sidhuvudet (NAV-4): temats `appVersion` står efter appnamnet; utan den ritas ingen version. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class AppHeaderVersionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(appVersion: String?) = rule.setContent {
        ReseApoteketTheme(appVersion = appVersion) { AppHeader("Resor", TopAppBarDefaults.exitUntilCollapsedScrollBehavior()) }
    }

    private fun version() = rule.onNodeWithTag(APP_HEADER_VERSION_TAG, useUnmergedTree = true)

    @Test
    fun `temats appVersion står efter appnamnet`() {
        show("0.15.0")
        version().assertIsDisplayed()
        version().assertTextEquals("0.15.0")
    }

    @Test
    fun `utan appVersion ritas ingen version`() {
        show(null)
        version().assertDoesNotExist()
    }

    /** Identiteten sätts direkt så att bilden bär ett påhittat nummer, inte appens riktiga. */
    @Test
    fun `skärmdump - versionen i sidhuvudet`() {
        rule.captureLightAndDark("AppHeader_vila_version") {
            CompositionLocalProvider(LocalAppIdentity provides AppIdentity(R.drawable.ic_app_header, "ReseApoteket", "0.15.0")) {
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    AppHeader("Resor", TopAppBarDefaults.exitUntilCollapsedScrollBehavior(), subtitle = "Tre kommande")
                }
            }
        }
    }
}
