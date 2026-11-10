package ca.bcparkspot.app

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.ui.theme.BCParkSpotTheme
import ca.bcparkspot.app.ui.viewmodel.ParkingViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LotsNavigationComposeE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: ParkingViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("ca.bcparkspot.app")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-api-key-for-test")
                    .build()
            )
        }
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = ParkingViewModel(application)
    }

    @Test
    fun testExploreScreen_rendersAndFindsNearestOpenSpot() {
        composeTestRule.setContent {
            BCParkSpotTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }

        // 1. Verify Explore Screen is visible
        composeTestRule.onNodeWithTag("lots_list_screen").assertIsDisplayed()

        // 2. Click "Find Nearest Open Spot" when lots are in initial baseline unknown status
        composeTestRule.onNodeWithTag("hero_nearest_open_button").performClick()

        // Verify that without confirmed open quorum, the app stays on the list screen and does not navigate
        composeTestRule.onNodeWithTag("lots_list_screen").assertIsDisplayed()

        // 3. Establish confirmed crowd consensus with quorum (>= 2 distinct users)
        val now = System.currentTimeMillis()
        viewModel.updateCommunityReportsForTesting(
            listOf(
                ca.bcparkspot.app.model.CommunityReport(
                    id = "rep_1",
                    userId = "user_1",
                    lotId = "whytecliff_park_lot",
                    reporterName = "Hiker 1",
                    minutesAgo = 5,
                    statusReported = ca.bcparkspot.app.model.AvailabilityStatus.AVAILABLE,
                    comment = "Lots of parking open at Whytecliff",
                    spotsEstimate = 40,
                    timestampMs = now - 300_000
                ),
                ca.bcparkspot.app.model.CommunityReport(
                    id = "rep_2",
                    userId = "user_2",
                    lotId = "whytecliff_park_lot",
                    reporterName = "Hiker 2",
                    minutesAgo = 10,
                    statusReported = ca.bcparkspot.app.model.AvailabilityStatus.AVAILABLE,
                    comment = "Plenty of spots",
                    spotsEstimate = 35,
                    timestampMs = now - 600_000
                )
            )
        )

        // 4. Click hero button again now that lot is confirmed available
        composeTestRule.onNodeWithTag("hero_nearest_open_button").performClick()

        // 5. Verify it transitions to Route Navigation Screen
        composeTestRule.onNodeWithTag("route_navigation_screen").assertIsDisplayed()
    }

    @Test
    fun testAuthDialog_opensAndDisplaysGoogleSignInButton() {
        composeTestRule.setContent {
            BCParkSpotTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }

        // Click account button in header
        composeTestRule.onNodeWithTag("account_button").performClick()

        // Verify Google Sign-In button is displayed
        composeTestRule.onNodeWithTag("dialog_google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun testBottomNavigation_switchesBetweenTabs() {
        composeTestRule.setContent {
            BCParkSpotTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }

        // Switch to Saved tab
        composeTestRule.onNodeWithTag("nav_tab_saved").performClick()
        composeTestRule.onNodeWithTag("saved_lots_screen").assertIsDisplayed()

        // Switch to Day Passes tab
        composeTestRule.onNodeWithTag("nav_tab_pass_guide").performClick()
        composeTestRule.onNodeWithTag("bc_pass_info_screen").assertIsDisplayed()

        // Switch back to Explore
        composeTestRule.onNodeWithTag("nav_tab_explore").performClick()
        composeTestRule.onNodeWithTag("lots_list_screen").assertIsDisplayed()
    }

    @Test
    fun testSwitchToMapMode_andBack() {
        composeTestRule.setContent {
            BCParkSpotTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }

        // Scroll to and click Map toggle button in LazyColumn
        composeTestRule.onNodeWithTag("lots_list_screen")
            .performScrollToNode(hasTestTag("explore_toggle_map"))
        composeTestRule.onNodeWithTag("explore_toggle_map").performClick()
        composeTestRule.waitForIdle()

        // Verify map container is displayed
        composeTestRule.onNodeWithTag("explore_map_container").assertIsDisplayed()

        // Toggle back to list
        composeTestRule.onNodeWithTag("explore_toggle_list").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lots_list_screen").assertIsDisplayed()
    }
}

