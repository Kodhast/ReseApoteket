package se.kodhast.reseapoteket.testing

import kotlinx.coroutines.flow.MutableStateFlow
import se.kodhast.reseapoteket.core.model.Household
import se.kodhast.reseapoteket.core.schema.HouseholdCodec
import se.kodhast.reseapoteket.data.common.fieldsForUpdate
import se.kodhast.reseapoteket.data.repository.HouseholdRepository

/**
 * Det aktiva hushållet – formulärens förhandsvisningar läser marginalen därifrån. [update] skriver
 * bara de valda fälten med samma regel som samlingarna (`fieldsForUpdate`) och sparar vilka ([updates]);
 * [nextUpdate] kan göra skrivningen till ett fel.
 */
class StubHouseholds(override val current: MutableStateFlow<Household?>) : HouseholdRepository {
    val updates = mutableListOf<Set<String>>()
    var nextUpdate: Result<Unit> = Result.success(Unit)

    override suspend fun create(name: String, ownerUid: String): Result<String> = error("används inte")

    override suspend fun select(householdId: String) = error("används inte")

    override suspend fun update(household: Household, fields: Set<String>): Result<Unit> = nextUpdate.onSuccess {
        updates += fields
        val stored = current.value ?: return@onSuccess
        current.value = HouseholdCodec.decode(stored.id, HouseholdCodec.encode(stored) + fieldsForUpdate(HouseholdCodec.encode(household), fields))
    }
}
