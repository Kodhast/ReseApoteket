package se.kodhast.reseapoteket.testing

import kotlinx.coroutines.flow.MutableStateFlow
import se.kodhast.ui.account.NetworkStatus
import se.kodhast.ui.account.ProfilePhotoCache

/** Nätläget i proven: [online] sätts för hand. */
class FakeNetworkStatus(online: Boolean = false) : NetworkStatus {
    override val online = MutableStateFlow(online)
}

/** Profilfotots cache i proven: räknar tömningarna; [next] är utfallet. */
class FakeProfilePhotoCache : ProfilePhotoCache {
    var clears = 0
        private set
    var next: Result<Unit> = Result.success(Unit)

    override suspend fun clear(): Result<Unit> {
        clears++
        return next
    }
}
