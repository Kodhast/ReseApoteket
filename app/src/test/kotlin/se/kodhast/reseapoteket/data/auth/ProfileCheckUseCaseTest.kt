package se.kodhast.reseapoteket.data.auth

import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import se.kodhast.reseapoteket.testing.FakeNetworkStatus
import se.kodhast.reseapoteket.ui.auth.FakeAuthRepository

/** Profilkontrollen (AUTH-5): en lyckad hämtning per inloggad användare, bara ansluten, nytt försök efter ett fel. */
class ProfileCheckUseCaseTest {

    private val auth = FakeAuthRepository()
    private val network = FakeNetworkStatus()

    private fun runCheck(block: suspend () -> Unit) = runTest(UnconfinedTestDispatcher()) {
        val job = launch { ProfileCheckUseCase(auth, network)() }
        block()
        job.cancel()
    }

    @Test
    fun `utloggad görs ingenting, inte heller ansluten`() = runCheck {
        network.online.value = true
        assertEquals(0, auth.reloadCalls)
    }

    @Test
    fun `inloggad utan nät väntar tills appen blir ansluten`() = runCheck {
        auth.authState.value = AuthUser("uid-anna")
        assertEquals(0, auth.reloadCalls)
        network.online.value = true
        assertEquals(1, auth.reloadCalls)
    }

    @Test
    fun `efter en lyckad hämtning görs inget mer för samma konto`() = runCheck {
        network.online.value = true
        auth.authState.value = AuthUser("uid-anna")
        network.online.value = false
        network.online.value = true
        auth.authState.value = AuthUser("uid-anna", name = "Anna")
        assertEquals(1, auth.reloadCalls)
    }

    @Test
    fun `ett misslyckat försök görs om nästa gång appen blir ansluten`() = runCheck {
        auth.nextReload = Result.failure(IllegalStateException("nät"))
        network.online.value = true
        auth.authState.value = AuthUser("uid-anna")
        assertEquals(1, auth.reloadCalls)
        auth.nextReload = Result.success(Unit)
        network.online.value = false
        network.online.value = true
        assertEquals(2, auth.reloadCalls)
        network.online.value = false
        network.online.value = true
        assertEquals(2, auth.reloadCalls)
    }

    @Test
    fun `ett nytt konto kontrolleras igen`() = runCheck {
        network.online.value = true
        auth.authState.value = AuthUser("uid-anna")
        auth.authState.value = null
        auth.authState.value = AuthUser("uid-erik")
        assertEquals(2, auth.reloadCalls)
    }
}
