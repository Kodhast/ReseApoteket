package se.kodhast.reseapoteket.data.auth

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Den inloggade användaren – bara det appen behöver. Namn, e-post och fotots adress visas i avataren och
 * Inställningar (NAV-5, SET-4) men loggas eller sparas aldrig; själva fotot cachas på enheten (AUTH-5).
 */
data class AuthUser(val uid: String, val name: String? = null, val email: String? = null, val photoUrl: String? = null)

/** Inloggning med Google (skill android:firebase-auth + android-projekt). Alla fel kommer som `DataError`, mappade en gång. */
interface AuthRepository {
    /** Aktuell användare vid varje ändring; `null` = utloggad. */
    val authState: Flow<AuthUser?>

    /** Visar Googles kontoväljare – kräver aktivitetens kontext. Avbrott ger `DataError.Cancelled`. */
    suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser>

    /** Rensar Credential Managers sparade inloggning, Firebase-sessionen och det valda hushållet. */
    suspend fun signOut(): Result<Unit>

    /**
     * Hämtar om den inloggades profil från servern (AUTH-5), så att ett utbytt namn eller foto når [authState].
     * Utloggad eller utan nät är ett fel (`DataError`).
     */
    suspend fun reload(): Result<Unit>
}
