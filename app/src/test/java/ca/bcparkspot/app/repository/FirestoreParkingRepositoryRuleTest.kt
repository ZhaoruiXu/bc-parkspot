package ca.bcparkspot.app.repository

import ca.bcparkspot.app.base.FirestoreEmulatorTestBase
import ca.bcparkspot.app.data.repository.FirestoreParkingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreParkingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canSaveAndObserveLot() = runBlocking {
        val aliceUid = signInTestUser("alice@example.com")
        val repository = FirestoreParkingRepository(firestore, auth)

        repository.saveLot(
            userId = aliceUid,
            lotId = "garibaldi_rubble_creek",
            parkName = "Garibaldi Provincial Park",
            lotName = "Rubble Creek Main Trailhead",
            region = "Sea-to-Sky",
            userNotes = "Bring microspikes in autumn"
        )

        val lots = repository.observeSavedLots(aliceUid).first()
        assertTrue("Alice should have at least 1 saved lot", lots.isNotEmpty())
        val saved = lots.first { it.lotId == "garibaldi_rubble_creek" }
        assertEquals("garibaldi_rubble_creek", saved.lotId)
        assertEquals("Garibaldi Provincial Park", saved.parkName)
    }

    @Test
    fun unauthenticatedUser_cannotAccessSavedLots() {
        val repository = FirestoreParkingRepository(firestore, auth)
        auth.signOut()

        var threwException = false
        try {
            runBlocking {
                repository.saveLot(
                    userId = "some_random_uid",
                    lotId = "test_lot",
                    parkName = "Test Park",
                    lotName = "Test Lot",
                    region = "Metro Vancouver",
                    userNotes = ""
                )
            }
        } catch (e: Exception) {
            threwException = true
        }
        assertTrue("Unauthenticated call must fail fast or be rejected by rules", threwException)
    }

    @Test
    fun authenticatedUser_canPostAndReadCommunityReports() = runBlocking {
        val userUid = signInTestUser("hiker_bob@example.com")
        val repository = FirestoreParkingRepository(firestore, auth)

        val reportId = repository.postCommunityReport(
            lotId = "joffre_lakes_main",
            parkName = "Joffre Lakes Provincial Park",
            status = "AVAILABLE",
            comment = "15 spots available at 8:00 AM",
            spotsEstimate = 15,
            reporterName = "Bob Hiker"
        )
        assertNotNull("Report ID must be generated", reportId)

        val reports = repository.observeCommunityReports().first()
        assertTrue("Reports list should contain newly posted report", reports.any { it.lotId == "joffre_lakes_main" })
    }
}
