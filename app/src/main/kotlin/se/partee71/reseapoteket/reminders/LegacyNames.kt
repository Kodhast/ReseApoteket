package se.partee71.reseapoteket.reminders

import android.content.Context
import androidx.work.WorkerParameters

// Klassnamnen från före paketbytet (#156). WorkManager sparar jobbets klassnamn och en notis sin
// "Senare"-mottagare, så jobb och notiser från den gamla versionen hittar hit och gör samma sak
// som förut (PÅM-6, PÅM-7). Kan tas bort när ingen telefon längre har en version före 0.6.2.

/** Köade jobb från före bytet: genomgången och "Senare". */
class ReminderWorker(context: Context, params: WorkerParameters) : se.kodhast.reseapoteket.reminders.ReminderWorker(context, params)

/** "Senare" i en notis som visades före bytet. */
class ReminderSnoozeReceiver : se.kodhast.reseapoteket.reminders.ReminderSnoozeReceiver()
