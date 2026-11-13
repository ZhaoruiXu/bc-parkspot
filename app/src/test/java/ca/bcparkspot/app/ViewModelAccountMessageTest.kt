package ca.bcparkspot.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.auth.GoogleTokenResult
import ca.bcparkspot.app.testutil.awaitUntil
import ca.bcparkspot.app.ui.viewmodel.ParkingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelAccountMessageTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val noAccountText get() = app.getString(R.string.google_no_account)

    private fun goOnline() {
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        val caps = ShadowNetworkCapabilities.newInstance().also {
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        shadowOf(connectivity).networkCallbacks.forEach { it.onCapabilitiesChanged(connectivity.activeNetwork!!, caps) }
    }

    private fun withViewModel(block: (ParkingViewModel) -> Unit) {
        goOnline()
        val viewModel = ParkingViewModel(app)
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { viewModel.account.collect {} }
        try {
            try {
                awaitUntil { viewModel.account.value.online && viewModel.account.value.busy == null }
            } catch (e: IllegalStateException) {
                throw AssertionError("account never became ready: ${viewModel.account.value}", e)
            }
            block(viewModel)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `a sign-in problem shows inside the open account sheet, not behind it`() = withViewModel { viewModel ->
        viewModel.showAuthDialog(true)
        viewModel.signInWithGoogle { GoogleTokenResult.NoAccount }
        awaitUntil { viewModel.uiState.value.accountMessage != null }
        assertEquals(noAccountText, viewModel.uiState.value.accountMessage)
        assertNull(viewModel.uiState.value.userNotification)
    }

    @Test
    fun `closing and reopening the sheet clears the old message`() = withViewModel { viewModel ->
        viewModel.showAuthDialog(true)
        viewModel.signInWithGoogle { GoogleTokenResult.NoAccount }
        awaitUntil { viewModel.uiState.value.accountMessage != null }
        viewModel.showAuthDialog(false)
        viewModel.showAuthDialog(true)
        assertNull(viewModel.uiState.value.accountMessage)
    }

    @Test
    fun `with the sheet closed the message is a snackbar`() = withViewModel { viewModel ->
        viewModel.signInWithGoogle { GoogleTokenResult.NoAccount }
        awaitUntil { viewModel.uiState.value.userNotification != null }
        assertEquals(noAccountText, viewModel.uiState.value.userNotification)
        assertNull(viewModel.uiState.value.accountMessage)
    }
}
