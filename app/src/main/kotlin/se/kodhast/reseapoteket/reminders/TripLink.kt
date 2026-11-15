package se.kodhast.reseapoteket.reminders

import android.content.Context
import android.content.Intent
import se.kodhast.reseapoteket.MainActivity
import se.kodhast.reseapoteket.core.schema.DocumentIds

/**
 * Länken från en påminnelse till resans packlista (PÅM-7). Notisen öppnar appen via ett
 * icke-exporterat alias av [MainActivity] (`OpenTripFromReminder` i manifestet): bara då läses
 * resan, så en annan app som startar [MainActivity] med samma extra öppnar ingenting.
 */
object TripLink {
    /** Nyckeln står kvar från före paketbytet (#156), så att en notis som redan ligger framme fungerar. */
    const val EXTRA_TRIP_ID = "se.partee71.reseapoteket.TRIP_ID"

    /** Aliasets klassnamn – i appens kodpaket, oberoende av applicationId (debug-suffix). */
    val ALIAS: String = "${MainActivity::class.java.name.substringBeforeLast('.')}.OpenTripFromReminder"

    /** Aliaset under namnet från före paketbytet (#156), för notiser som skapades före uppdateringen. */
    const val LEGACY_ALIAS: String = "se.partee71.reseapoteket.OpenTripFromReminder"

    fun intent(context: Context, tripId: String): Intent = Intent().setClassName(context, ALIAS).putExtra(EXTRA_TRIP_ID, tripId)

    /**
     * Resan i [intent], eller `null` om den inte kom via notisens alias, saknas eller inte är ett
     * giltigt ID. Okända extras från en annan app (som inte går att packa upp på Android 11–12)
     * blir också `null` i stället för en krasch.
     */
    fun tripId(intent: Intent): String? = try {
        if (intent.component?.className !in setOf(ALIAS, LEGACY_ALIAS)) null else intent.getStringExtra(EXTRA_TRIP_ID)?.takeIf(DocumentIds::isValid)
    } catch (_: RuntimeException) {
        null
    }

    /** Tar bort resan ur [intent], så att den inte öppnas igen; tål samma främmande extras som [tripId]. */
    fun consume(intent: Intent) {
        try {
            intent.removeExtra(EXTRA_TRIP_ID)
        } catch (_: RuntimeException) {
            // Kunde inte packas upp – då finns heller ingen resa att ta bort.
        }
    }
}
