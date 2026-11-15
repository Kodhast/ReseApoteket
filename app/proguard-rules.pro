# Keep-regler för release-minify (skill data-privacy-security, regel 6).
# Hilt och Compose har egna konsumentregler. Regler för kotlinx.serialization och
# Firestore-modeller läggs till här när de börjar användas (#14, #15).

# Strippa loggning ur releasebygget – appen hanterar hälsodata (NFR-8).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Köade jobb från före paketbytet (#156) bär det gamla klassnamnet. Klassen refereras inte från koden,
# och WorkManagers egen regel är bara -keepnames, så utan den här raden tar R8 bort den.
-keep class se.partee71.reseapoteket.reminders.ReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
