package se.kodhast.reseapoteket.navigation

import kotlin.test.assertEquals
import org.junit.Test

/** Plusknappens val öppnar ett nytt (tomt) formulär av rätt slag, i flikarnas ordning (NAV-6). */
class AddChoiceTest {

    @Test
    fun `varje val öppnar ett nytt formulär för sin flik`() {
        assertEquals(listOf<AppKey>(TripEditKey(), PersonEditKey(), FamilyItemEditKey()), AddChoice.entries.map { it.key })
    }

    @Test
    fun `ett val tryckt två gånger öppnar formuläret en gång`() {
        val backStack = appBackStack()
        backStack.push(AddChoice.Person.key)
        backStack.push(AddChoice.Person.key)
        assertEquals(listOf<AppKey>(TripsKey, PersonEditKey()), backStack.entries.toList())
    }
}
