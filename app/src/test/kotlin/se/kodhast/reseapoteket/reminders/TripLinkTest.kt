package se.kodhast.reseapoteket.reminders

import android.content.Intent
import android.content.pm.PackageManager
import android.os.BadParcelableException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.kodhast.reseapoteket.MainActivity

/** Påminnelsens länk till en resa (PÅM-7, #88): bara via notisens alias och bara ett giltigt ID. */
@RunWith(RobolectricTestRunner::class)
class TripLinkTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `notisens länk ger resan, en gång`() {
        val intent = TripLink.intent(context, "gran-canaria")
        assertEquals("gran-canaria", TripLink.tripId(intent))
        TripLink.consume(intent)
        assertNull(TripLink.tripId(intent))
    }

    @Test
    fun `en annan app som startar MainActivity direkt med samma extra öppnar ingenting`() {
        val direct = Intent(context, MainActivity::class.java).putExtra(TripLink.EXTRA_TRIP_ID, "gran-canaria")
        assertNull(TripLink.tripId(direct))
        assertNull(TripLink.tripId(Intent().putExtra(TripLink.EXTRA_TRIP_ID, "gran-canaria")))
    }

    @Test
    fun `en notis som skapades före paketbytet (#156) öppnar resan via det gamla aliasnamnet`() {
        val legacy = Intent().setClassName(context, TripLink.LEGACY_ALIAS).putExtra(TripLink.EXTRA_TRIP_ID, "gran-canaria")
        assertEquals("gran-canaria", TripLink.tripId(legacy))
        TripLink.consume(legacy)
        assertNull(TripLink.tripId(legacy))
        // Det gamla namnet är ingen genväg förbi giltighetskontrollen.
        assertNull(TripLink.tripId(Intent().setClassName(context, TripLink.LEGACY_ALIAS).putExtra(TripLink.EXTRA_TRIP_ID, "../t1")))
        // Det gamla MainActivity-namnet är startikonens alias, inte notisens: det ger ingen resa.
        assertNull(TripLink.tripId(Intent().setClassName(context, "se.partee71.reseapoteket.MainActivity").putExtra(TripLink.EXTRA_TRIP_ID, "gran-canaria")))
    }

    @Test
    fun `ogiltiga ID ignoreras`() {
        listOf("", "a/b", "../t1", "x".repeat(2000)).forEach { id ->
            assertNull(TripLink.tripId(TripLink.intent(context, id)), id)
        }
    }

    @Test
    fun `extras som inte går att packa upp kraschar inte appen`() {
        val broken = object : Intent() {
            override fun getStringExtra(name: String?): String = throw BadParcelableException("främmande Parcelable")

            override fun removeExtra(name: String?) = throw BadParcelableException("främmande Parcelable")
        }.setClassName(context, TripLink.ALIAS)
        assertNull(TripLink.tripId(broken))
        TripLink.consume(broken)
    }

    @Test
    fun `aliaset finns i manifestet, pekar på MainActivity och är inte exporterat`() {
        val info = context.packageManager.getActivityInfo(android.content.ComponentName(context, TripLink.ALIAS), PackageManager.GET_META_DATA)
        assertEquals(MainActivity::class.java.name, info.targetActivity)
        assertEquals(false, info.exported)
    }

    @Test
    fun `det gamla aliasnamnet finns i manifestet, pekar på MainActivity och är inte exporterat`() {
        val info = context.packageManager.getActivityInfo(android.content.ComponentName(context, TripLink.LEGACY_ALIAS), PackageManager.GET_META_DATA)
        assertEquals(MainActivity::class.java.name, info.targetActivity)
        assertEquals(false, info.exported)
    }
}
