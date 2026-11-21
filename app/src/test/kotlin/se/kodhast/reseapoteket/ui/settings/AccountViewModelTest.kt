package se.kodhast.reseapoteket.ui.settings

import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import se.kodhast.reseapoteket.data.auth.AuthUser
import se.kodhast.reseapoteket.data.common.DataError
import se.kodhast.reseapoteket.testing.FakeProfilePhotoCache
import se.kodhast.reseapoteket.testing.MainDispatcherRule
import se.kodhast.reseapoteket.ui.auth.FakeAuthRepository

/** Utloggningen i avatarmenyn (SET-4, AUTH-4, AUTH-5). */
class AccountViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val auth = FakeAuthRepository().apply { authState.value = AuthUser("uid-anna", "Anna Svensson", "anna.svensson@gmail.com") }
    private val photos = FakeProfilePhotoCache()

    @Test
    fun `utloggning loggar ut, ett fel visas och släpps när det visats (SET-4)`() = runTest {
        val vm = AccountViewModel(auth, photos)
        vm.error.test {
            assertNull(awaitItem())
            auth.nextSignOut = Result.failure(DataError.Offline)
            vm.signOut()
            assertEquals(DataError.Offline, expectMostRecentItem()?.cause)
            auth.nextSignOut = Result.success(Unit)
            vm.signOut()
            assertNull(expectMostRecentItem(), "en ny utloggning släpper det gamla felet")
        }
        assertEquals(2, auth.signOutCalls)
        assertNull(auth.authState.value)
        assertEquals(1, photos.clears, "fotot tas bort bara efter en lyckad utloggning (AUTH-4)")
    }

    @Test
    fun `ett fel kan släppas utan ny utloggning`() = runTest {
        val vm = AccountViewModel(auth, photos)
        auth.nextSignOut = Result.failure(DataError.Offline)
        vm.signOut()
        assertEquals(DataError.Offline, vm.error.value?.cause)
        vm.onErrorShown()
        assertNull(vm.error.value)
    }

    @Test
    fun `ett fel när fotot tas bort stoppar inte utloggningen (AUTH-5)`() = runTest {
        photos.next = Result.failure(IllegalStateException("disk"))
        val vm = AccountViewModel(auth, photos)
        vm.signOut()
        assertNull(auth.authState.value)
        assertNull(vm.error.value)
        assertEquals(1, photos.clears)
    }
}
