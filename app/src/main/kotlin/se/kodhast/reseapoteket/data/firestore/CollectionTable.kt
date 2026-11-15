package se.kodhast.reseapoteket.data.firestore

import se.kodhast.reseapoteket.core.model.Identified
import se.kodhast.reseapoteket.core.schema.DocCodec
import se.kodhast.reseapoteket.core.schema.FamilyItemCodec
import se.kodhast.reseapoteket.core.schema.HouseholdCodec
import se.kodhast.reseapoteket.core.schema.PackingItemCodec
import se.kodhast.reseapoteket.core.schema.PersonCodec
import se.kodhast.reseapoteket.core.schema.RegimenCodec
import se.kodhast.reseapoteket.core.schema.TripCodec
import se.kodhast.reseapoteket.data.common.CollectionFactory
import se.kodhast.reseapoteket.data.common.DataError
import se.kodhast.reseapoteket.data.common.EntityCollection

/**
 * Varje samlings namn, codec och sökväg – ett ställe, delat av den riktiga fabriken och
 * testernas `FakeCollectionFactory`, så att de bevisligen använder samma tabell.
 */
abstract class CollectionTable : CollectionFactory {

    /** Skapar en samling; [path] kastar [DataError.NoHousehold] om den kräver ett hushåll. */
    protected abstract fun <T : Identified> create(
        codec: DocCodec<T>,
        name: String,
        path: (householdId: String?) -> String
    ): EntityCollection<T>

    override fun households() = create(HouseholdCodec, Paths.HOUSEHOLDS) { Paths.HOUSEHOLDS }

    override fun persons() = create(PersonCodec, Paths.PERSONS) { Paths.persons(required(it)) }

    override fun regimens(personId: String) = create(RegimenCodec, Paths.REGIMENS) { Paths.regimens(required(it), personId) }

    override fun familyItems() = create(FamilyItemCodec, Paths.FAMILY_ITEMS) { Paths.familyItems(required(it)) }

    override fun trips() = create(TripCodec, Paths.TRIPS) { Paths.trips(required(it)) }

    override fun packingItems(tripId: String) = create(PackingItemCodec, Paths.PACKING_ITEMS) { Paths.packingItems(required(it), tripId) }

    private fun required(householdId: String?): String = householdId ?: throw DataError.NoHousehold
}
