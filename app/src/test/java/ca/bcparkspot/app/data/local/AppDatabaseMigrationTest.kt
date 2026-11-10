package ca.bcparkspot.app.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import ca.bcparkspot.app.data.local.AppDatabase.Companion.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    private val testDbName = "migration-test-db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_preservesSavedLotAndParkedCar() {
        // 1. Create database at version 1 and seed data
        var db: SupportSQLiteDatabase = helper.createDatabase(testDbName, 1).apply {
            val lotValues = ContentValues().apply {
                put("lotId", "joffre_lakes")
                put("parkName", "Joffre Lakes Provincial Park")
                put("lotName", "Main Trailhead")
                put("region", "Sea-to-Sky")
                put("savedAtTimestamp", 1700000000000L)
                put("userNotes", "Day-pass required in peak season")
            }
            insert("saved_lots", SQLiteDatabase.CONFLICT_REPLACE, lotValues)

            val carValues = ContentValues().apply {
                put("id", 1)
                put("parkName", "Joffre Lakes")
                put("lotName", "Upper Lot")
                put("latitude", 50.316)
                put("longitude", -122.484)
                put("stallNumber", "Row B-12")
                put("notes", "Parked near trailhead sign")
                put("timestamp", 1700000000000L)
                put("isExactGpsLocation", 1)
            }
            insert("parked_car", SQLiteDatabase.CONFLICT_REPLACE, carValues)
            close()
        }

        // 2. Migrate to version 2 using MIGRATION_1_2 and validate schema
        db = helper.runMigrationsAndValidate(testDbName, 2, true, MIGRATION_1_2)

        // 3. Verify saved lot data survives migration
        val lotCursor = db.query("SELECT * FROM saved_lots WHERE lotId = ?", arrayOf("joffre_lakes"))
        assertNotNull(lotCursor)
        lotCursor.use {
            assertTrue("Expected 1 saved lot after migration", it.moveToFirst())
            assertEquals("joffre_lakes", it.getString(it.getColumnIndexOrThrow("lotId")))
            assertEquals("Joffre Lakes Provincial Park", it.getString(it.getColumnIndexOrThrow("parkName")))
            assertEquals("Main Trailhead", it.getString(it.getColumnIndexOrThrow("lotName")))
            assertEquals("Sea-to-Sky", it.getString(it.getColumnIndexOrThrow("region")))
            assertEquals(1700000000000L, it.getLong(it.getColumnIndexOrThrow("savedAtTimestamp")))
            assertEquals("Day-pass required in peak season", it.getString(it.getColumnIndexOrThrow("userNotes")))
        }

        // 4. Verify parked car pin survives migration
        val carCursor = db.query("SELECT * FROM parked_car WHERE id = 1")
        assertNotNull(carCursor)
        carCursor.use {
            assertTrue("Expected parked car after migration", it.moveToFirst())
            assertEquals("Joffre Lakes", it.getString(it.getColumnIndexOrThrow("parkName")))
            assertEquals("Upper Lot", it.getString(it.getColumnIndexOrThrow("lotName")))
            assertEquals(50.316, it.getDouble(it.getColumnIndexOrThrow("latitude")), 0.0001)
            assertEquals(-122.484, it.getDouble(it.getColumnIndexOrThrow("longitude")), 0.0001)
            assertEquals("Row B-12", it.getString(it.getColumnIndexOrThrow("stallNumber")))
            assertEquals("Parked near trailhead sign", it.getString(it.getColumnIndexOrThrow("notes")))
            assertEquals(1, it.getInt(it.getColumnIndexOrThrow("isExactGpsLocation")))
        }
    }
}
