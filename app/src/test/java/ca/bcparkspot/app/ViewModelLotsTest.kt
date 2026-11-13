package ca.bcparkspot.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.testutil.awaitUntil
import ca.bcparkspot.app.ui.viewmodel.ParkingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelLotsTest {
    @Test
    fun `with no filters the list shows every lot`() {
        val viewModel = ParkingViewModel(ApplicationProvider.getApplicationContext<Application>())
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { viewModel.filteredLots.collect {} }
        val expected = BCParksDataSource.getInitialParkingLots().size
        try {
            awaitUntil { viewModel.filteredLots.value.size == expected }
        } finally {
            scope.cancel()
        }
        assertEquals(expected, viewModel.filteredLots.value.size)
    }
}
