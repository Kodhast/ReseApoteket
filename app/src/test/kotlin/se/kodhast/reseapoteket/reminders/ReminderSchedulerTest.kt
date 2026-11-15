package se.kodhast.reseapoteket.reminders

import android.content.Context
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.kodhast.reseapoteket.data.FixedClock

/** Schemaläggningen med WorkManager (PÅM-6): ett jobb till nästa genomgång, ett per "Senare". */
@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = FixedClock()

    @Before
    fun workManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
    }

    private fun all(name: String): List<WorkInfo> = WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get()

    private fun waiting(name: String): List<WorkInfo> = all(name).filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun `nästa genomgång ersätter den som väntar`() {
        val scheduler = ReminderScheduler(context, clock)
        scheduler.checkAt(clock.instant + 5.hours)
        scheduler.checkAt(clock.instant + 2.hours)
        val work = waiting(ReminderScheduler.CHECK).single()
        assertEquals(2.hours.inWholeMilliseconds, work.initialDelayMillis)
    }

    @Test
    fun `en passerad tid körs direkt`() {
        ReminderScheduler(context, clock).checkAt(clock.instant - 1.hours)
        // Utan väntetid startar testets WorkManager jobbet direkt – det ligger inte kvar som väntande.
        assertEquals(0, all(ReminderScheduler.CHECK).single().initialDelayMillis)
    }

    @Test
    fun `Senare väntar en timme och sparar bara påminnelsens nyckel`() {
        ReminderScheduler(context, clock).snooze("gc/DAY_BEFORE/18:00")
        val work = waiting("${ReminderScheduler.SNOOZE_PREFIX}gc/DAY_BEFORE/18:00").single()
        assertEquals(1.hours.inWholeMilliseconds, work.initialDelayMillis)
    }

    @Test
    fun `nästa genomgång ersätter ett jobb som köades under klassnamnet från före paketbytet (#156)`() {
        // WorkManager bär jobbets klassnamn och taggar jobbet med det; ett jobb från före bytet pekar på en klass som inte finns.
        val stale = OneTimeWorkRequest.Builder(StaleWorker::class.java).setInitialDelay(5, java.util.concurrent.TimeUnit.HOURS).addTag(LEGACY_WORKER).build()
        WorkManager.getInstance(context).enqueueUniqueWork(ReminderScheduler.CHECK, androidx.work.ExistingWorkPolicy.REPLACE, stale).result.get()
        assertTrue(LEGACY_WORKER in waiting(ReminderScheduler.CHECK).single().tags)

        ReminderScheduler(context, clock).checkAt(clock.instant + 2.hours)

        val work = waiting(ReminderScheduler.CHECK).single()
        assertTrue(ReminderWorker::class.java.name in work.tags)
        assertTrue(LEGACY_WORKER !in work.tags)
    }

    /** Står i för det gamla jobbet; bara taggen med det gamla namnet är det testet bryr sig om. */
    class StaleWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result = Result.success()
    }

    private companion object {
        const val LEGACY_WORKER = "se.partee71.reseapoteket.reminders.ReminderWorker"
    }
}
