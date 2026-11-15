package se.kodhast.reseapoteket.ui.tabs

import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import se.kodhast.reseapoteket.core.model.FamilyItem
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Trip
import se.kodhast.reseapoteket.data.FakeCollectionFactory
import se.kodhast.reseapoteket.data.repository.DefaultFamilyItemRepository
import se.kodhast.reseapoteket.data.repository.DefaultPackingRepository
import se.kodhast.reseapoteket.data.repository.DefaultPersonRepository
import se.kodhast.reseapoteket.data.repository.DefaultRegimenRepository
import se.kodhast.reseapoteket.data.repository.DefaultTripRepository
import se.kodhast.reseapoteket.testing.MainDispatcherRule
import se.kodhast.reseapoteket.ui.familyitems.FamilyItemsViewModel
import se.kodhast.reseapoteket.ui.persons.PersonsViewModel
import se.kodhast.reseapoteket.ui.trips.TripsViewModel
import se.kodhast.ui.common.ListUiState

/** Flikarnas ViewModels visar samlingens aktiva dokument; arkiverade döljs. */
class TabViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val collections = FakeCollectionFactory()

    @Test
    fun `Personer visar aktiva personer i ordning`() = runTest {
        val repository = DefaultPersonRepository(collections)
        repository.upsert(Person("p2", "Erik", sortOrder = 2))
        repository.upsert(Person("p1", "Anna", sortOrder = 1))
        repository.upsert(Person("p3", "Bo", archived = true))
        PersonsViewModel(repository, DefaultRegimenRepository(collections)).state.test {
            assertEquals(listOf("Anna", "Erik"), (expectMostRecentItem() as ListUiState.Content).items.map { it.person.name })
        }
    }

    @Test
    fun `Resor och Familjeartiklar är tomma utan dokument`() = runTest {
        TripsViewModel(DefaultTripRepository(collections), DefaultPersonRepository(collections), DefaultPackingRepository(collections)).state.test { assertEquals(ListUiState.Empty, expectMostRecentItem()) }
        FamilyItemsViewModel(DefaultFamilyItemRepository(collections)).state.test { assertEquals(ListUiState.Empty, expectMostRecentItem()) }
    }

    @Test
    fun `Resor och Familjeartiklar visar sina dokument`() = runTest {
        DefaultTripRepository(collections).upsert(Trip("t1", "Kreta"))
        DefaultFamilyItemRepository(collections).upsert(FamilyItem("f1", "Solkräm"))
        TripsViewModel(DefaultTripRepository(collections), DefaultPersonRepository(collections), DefaultPackingRepository(collections)).state.test {
            assertEquals(listOf("Kreta"), (expectMostRecentItem() as ListUiState.Content).items.map { it.trip.name })
        }
        FamilyItemsViewModel(DefaultFamilyItemRepository(collections)).state.test {
            assertEquals(listOf("Solkräm"), (expectMostRecentItem() as ListUiState.Content).items.map { it.name })
        }
    }
}
