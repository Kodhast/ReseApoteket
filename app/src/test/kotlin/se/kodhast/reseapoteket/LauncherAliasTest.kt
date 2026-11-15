package se.kodhast.reseapoteket

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Paketbytet (#156): koden bor i se.kodhast.reseapoteket, men applicationId och startikonens
 * komponentnamn är kvar från se.partee71, så att uppdateringen behåller appen och hemskärmens genväg.
 */
@RunWith(RobolectricTestRunner::class)
class LauncherAliasTest {

    private val context = RuntimeEnvironment.getApplication()
    private val legacyMain = "se.partee71.reseapoteket.MainActivity"

    @Test
    fun `applicationId är kvar från före paketbytet`() {
        assertEquals("se.partee71.reseapoteket", BuildConfig.APPLICATION_ID)
        assertEquals("se.partee71.reseapoteket", context.packageName)
    }

    @Test
    fun `startavsikten löser till det gamla MainActivity-namnet`() {
        val launch = assertNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        assertEquals(legacyMain, launch.component?.className)

        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val launchers = context.packageManager.queryIntentActivities(query, 0)
        assertEquals(listOf(legacyMain), launchers.map { it.activityInfo.name })
    }

    @Test
    fun `aliaset pekar på MainActivity i det nya paketet och är exporterat`() {
        val info = context.packageManager.getActivityInfo(ComponentName(context.packageName, legacyMain), PackageManager.GET_META_DATA)
        assertEquals("se.kodhast.reseapoteket.MainActivity", info.targetActivity)
        assertEquals(true, info.exported)
    }

    @Test
    fun `jobb köade före bytet hittar en worker som gör samma sak`() {
        // WorkManager skapar jobbet ur det sparade klassnamnet (PÅM-6).
        val legacy = Class.forName("se.partee71.reseapoteket.reminders.ReminderWorker")
        assertEquals(true, se.kodhast.reseapoteket.reminders.ReminderWorker::class.java.isAssignableFrom(legacy))
    }

    @Test
    fun `Senare i en notis från före bytet når en icke-exporterad mottagare`() {
        val name = "se.partee71.reseapoteket.reminders.ReminderSnoozeReceiver"
        val info = context.packageManager.getReceiverInfo(ComponentName(context.packageName, name), 0)
        assertEquals(false, info.exported)
        val legacy = Class.forName(name)
        assertEquals(true, se.kodhast.reseapoteket.reminders.ReminderSnoozeReceiver::class.java.isAssignableFrom(legacy))
    }
}
