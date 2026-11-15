package se.kodhast.reseapoteket.core.schema

import se.kodhast.reseapoteket.core.model.FamilyItem
import se.kodhast.reseapoteket.core.model.FamilyItemCategory
import se.kodhast.reseapoteket.core.model.Household
import se.kodhast.reseapoteket.core.model.HouseholdSettings
import se.kodhast.reseapoteket.core.model.ItemUnit
import se.kodhast.reseapoteket.core.model.MedicineForm
import se.kodhast.reseapoteket.core.model.Person
import se.kodhast.reseapoteket.core.model.Regimen
import se.kodhast.reseapoteket.core.model.RegimenKind

object HouseholdCodec : DocCodec<Household> {
    // Fälten som Inställningar skriver var för sig (SET-2) – aldrig memberUids eller schemaVersion.
    const val NAME = "name"
    const val MARGIN_DAYS = "marginDays"
    private const val SCHEMA_VERSION = "schemaVersion"

    override fun encode(value: Household): Doc = mapOf(
        NAME to value.name,
        "memberUids" to value.memberUids,
        MARGIN_DAYS to value.marginDays,
        SCHEMA_VERSION to value.schemaVersion,
        "createdAt" to value.createdAt
    )

    override fun decode(id: String, map: Doc) = Household(
        id = id,
        name = map.string(NAME),
        memberUids = map.stringList("memberUids"),
        marginDays = map.int(MARGIN_DAYS, HouseholdSettings.DEFAULT_MARGIN_DAYS),
        // Saknad version = första formatet – aldrig "senaste", då skulle migrering hoppas över.
        schemaVersion = Schema.versionOf(map[SCHEMA_VERSION]),
        createdAt = map.instant("createdAt")
    )
}

object PersonCodec : DocCodec<Person> {
    override fun encode(value: Person): Doc = mapOf(
        "name" to value.name,
        "emoji" to value.emoji,
        "colorHex" to value.colorHex,
        "sortOrder" to value.sortOrder,
        "archived" to value.archived
    )

    override fun decode(id: String, map: Doc) = Person(
        id = id,
        name = map.string("name"),
        emoji = map.string("emoji"),
        colorHex = map.string("colorHex"),
        sortOrder = map.int("sortOrder"),
        archived = map.bool("archived")
    )
}

object RegimenCodec : DocCodec<Regimen> {
    override fun encode(value: Regimen): Doc = mapOf(
        "kind" to value.kind.encodeEnum(),
        "name" to value.name,
        "form" to value.form.encodeEnum(),
        "strength" to value.strength,
        "unit" to value.unit.encodeEnum(),
        "packageSize" to value.packageSize,
        "schedule" to DoseScheduleCodec.encode(value.schedule),
        "notes" to value.notes,
        "active" to value.active,
        TRAVEL_DAY to value.travelDay,
        TRAVEL_DAY_TIME to value.travelDayTime.encodeTime(),
        "sortOrder" to value.sortOrder,
        "archived" to value.archived
    )

    override fun decode(id: String, map: Doc) = Regimen(
        id = id,
        kind = map.enum("kind", RegimenKind.MEDICINE),
        name = map.string("name"),
        form = map.enum("form", MedicineForm.OTHER),
        strength = map.string("strength"),
        unit = map.enum("unit", ItemUnit.TABLETT),
        packageSize = map.intOrNull("packageSize"),
        schedule = map.nested("schedule", DoseScheduleCodec),
        notes = map.string("notes"),
        active = map.bool("active", default = true),
        travelDay = map.bool(TRAVEL_DAY),
        travelDayTime = map.localTime(TRAVEL_DAY_TIME),
        sortOrder = map.int("sortOrder"),
        archived = map.bool("archived")
    )
}

object FamilyItemCodec : DocCodec<FamilyItem> {
    override fun encode(value: FamilyItem): Doc = mapOf(
        "name" to value.name,
        "category" to value.category.encodeEnum(),
        "unit" to value.unit.encodeEnum(),
        "rule" to QuantityRuleCodec.encode(value.rule),
        "notes" to value.notes,
        "active" to value.active,
        TRAVEL_DAY to value.travelDay,
        TRAVEL_DAY_TIME to value.travelDayTime.encodeTime(),
        "sortOrder" to value.sortOrder,
        "archived" to value.archived
    )

    override fun decode(id: String, map: Doc) = FamilyItem(
        id = id,
        name = map.string("name"),
        category = map.enum("category", FamilyItemCategory.OTHER),
        unit = map.enum("unit", ItemUnit.ST),
        rule = map.nested("rule", QuantityRuleCodec),
        notes = map.string("notes"),
        active = map.bool("active", default = true),
        travelDay = map.bool(TRAVEL_DAY),
        travelDayTime = map.localTime(TRAVEL_DAY_TIME),
        sortOrder = map.int("sortOrder"),
        archived = map.bool("archived")
    )
}
