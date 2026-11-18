package se.kodhast.reseapoteket.ui.sync

import app.cash.turbine.test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import se.kodhast.reseapoteket.data.common.DataError
import se.kodhast.reseapoteket.data.common.SyncStatus
import se.kodhast.reseapoteket.testing.FakeNetworkStatus
import se.kodhast.reseapoteket.testing.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    @get:Rule
    val main = MainDispatcherRule(StandardTestDispatcher())

    private class FakeSyncStatus : SyncStatus {
        override val syncing = MutableStateFlow(false)
        override val lastWriteError = MutableStateFlow<DataError?>(null)
        override fun clearWriteError() {
            lastWriteError.value = null
        }
    }

    private val status = FakeSyncStatus()
    private val network = FakeNetworkStatus()
    private val viewModel = SyncViewModel(status, network)

    @Test
    fun `väntande ändringar visas först efter två sekunder (NFR-6)`() = runTest(main.dispatcher) {
        viewModel.state.test {
            assertEquals(SyncUiState(), awaitItem())
            status.syncing.value = true
            testScheduler.advanceTimeBy(SyncViewModel.PENDING_DELAY.inWholeMilliseconds - 1)
            expectNoEvents()
            testScheduler.advanceTimeBy(2)
            assertEquals(SyncUiState(pending = true), awaitItem())
            status.syncing.value = false
            assertEquals(SyncUiState(), awaitItem())
        }
    }

    @Test
    fun `en nekad skrivning visas tills den släppts`() = runTest(main.dispatcher) {
        viewModel.state.test {
            assertEquals(SyncUiState(), awaitItem())
            status.lastWriteError.value = DataError.PermissionDenied
            assertEquals(DataError.PermissionDenied, awaitItem().writeError?.cause)
            viewModel.onEvent(SyncEvent.WriteErrorShown)
            assertEquals(SyncUiState(), awaitItem())
        }
        assertNull(status.lastWriteError.value)
    }

    @Test
    fun `anslutningen följer nätläget (NAV-5)`() = runTest(main.dispatcher) {
        viewModel.state.test {
            assertEquals(SyncUiState(online = false), awaitItem())
            network.online.value = true
            assertEquals(SyncUiState(online = true), awaitItem())
            network.online.value = false
            assertEquals(SyncUiState(online = false), awaitItem())
        }
    }
}
