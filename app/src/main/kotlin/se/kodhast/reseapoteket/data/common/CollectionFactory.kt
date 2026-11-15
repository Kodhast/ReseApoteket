package se.kodhast.reseapoteket.data.common

import se.kodhast.reseapoteket.core.model.FamilyItem
import se.kodhast.reseapoteket.core.model.Household
import se.kodhast.reseapoteket.core.model.PackingItem
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Regimen
import se.kodhast.reseapoteket.core.model.Trip

/** Skapar alla samlingar – sökvägarna finns på ett ställe (`Paths`). */
interface CollectionFactory {
    fun households(): EntityCollection<Household>

    fun persons(): EntityCollection<Person>

    fun regimens(personId: String): EntityCollection<Regimen>

    fun familyItems(): EntityCollection<FamilyItem>

    fun trips(): EntityCollection<Trip>

    fun packingItems(tripId: String): EntityCollection<PackingItem>
}
