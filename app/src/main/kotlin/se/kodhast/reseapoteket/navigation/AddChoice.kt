package se.kodhast.reseapoteket.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import se.kodhast.reseapoteket.R

/**
 * Plusknappens val i flikraden (NAV-6) och formuläret varje val öppnar: samma formulär som listornas egna knappar
 * öppnade förut (RES-2, PER-2, FAM-2). Recept och kosttillskott hör till en person och läggs till från persondetaljen.
 */
enum class AddChoice(@param:StringRes val label: Int, @param:DrawableRes val icon: Int, val key: AppKey) {
    Trip(R.string.trips_add, R.drawable.ic_luggage, TripEditKey()),
    Person(R.string.person_new, R.drawable.ic_person, PersonEditKey()),
    FamilyItem(R.string.family_item_new, R.drawable.ic_family_items, FamilyItemEditKey())
}
