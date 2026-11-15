package se.kodhast.reseapoteket.ui.tabs

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.reseapoteket.core.model.FamilyItem
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Trip
import se.kodhast.reseapoteket.testing.captureLightAndDark
import se.kodhast.reseapoteket.ui.contracts
import se.kodhast.reseapoteket.ui.familyitems.FamilyItemsContent
import se.kodhast.reseapoteket.ui.familyitems.FamilyItemsEvent
import se.kodhast.reseapoteket.ui.persons.PersonListItem
import se.kodhast.reseapoteket.ui.persons.PersonsContent
import se.kodhast.reseapoteket.ui.persons.PersonsEvent
import se.kodhast.reseapoteket.ui.trips.TripListItem
import se.kodhast.reseapoteket.ui.trips.TripsContent
import se.kodhast.reseapoteket.ui.trips.TripsEvent
import se.kodhast.ui.common.ListEvent
import se.kodhast.ui.common.ListUiState
import se.kodhast.ui.testing.runListScreenContract

/** Flikarna Resor, Personer och Familjeartiklar är listskärmar på `EntityListScreen` (NAV-1, NFR-1). */
@RunWith(RobolectricTestRunner::class)
class TabScreensTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `Resor uppfyller listkontraktet`() = rule.runListScreenContract(contracts, TripListItem(Trip("t1", "Kreta", days = 7)), "Kreta", "Inga resor än", "Ny resa") { state, onAdd, onRetry ->
        TripsContent(state, { if (it == TripsEvent.List(ListEvent.Retry)) onRetry() }, onAdd, {})
    }

    @Test
    fun `Personer uppfyller listkontraktet`() = rule.runListScreenContract(contracts, PersonListItem(Person("p1", "Anna", "🦊", "#D97757")), "Anna", "Inga personer än", "Lägg till person") { state, onAdd, onRetry ->
        PersonsContent(state, { if (it == PersonsEvent.List(ListEvent.Retry)) onRetry() }, onAdd, {})
    }

    @Test
    fun `Familjeartiklar uppfyller listkontraktet`() = rule.runListScreenContract(contracts, FamilyItem("f1", "Solkräm"), "Solkräm", "Inga familjeartiklar än", "Lägg till") { state, onAdd, onRetry ->
        FamilyItemsContent(state, { if (it == FamilyItemsEvent.List(ListEvent.Retry)) onRetry() }, onAdd, {})
    }

    @Test
    fun `tomma flikar och innehåll`() {
        captureLightAndDark("TripsScreen_tom") { TripsContent(ListUiState.Empty, {}, {}, {}) }
        captureLightAndDark("PersonsScreen_innehall") {
            PersonsContent(
                ListUiState.Content(
                    persistentListOf(
                        PersonListItem(Person("p1", "Anna", "🦊", "#D97757"), medicines = 3, supplements = 1),
                        PersonListItem(Person("p2", "Erik", "🐻", "#3B7EA1"), medicines = 1),
                        PersonListItem(Person("p3", "Lova", "🐰", "#7A9A3C"))
                    )
                ),
                {},
                {},
                {}
            )
        }
        captureLightAndDark("FamilyItemsScreen_tom") { FamilyItemsContent(ListUiState.Empty, {}, {}, {}) }
    }
}
