package ca.bcparkspot.app.data.local

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ca.bcparkspot.app.data.local.dao.ParkingDao
import ca.bcparkspot.app.data.local.entity.LOCAL_DEVICE_OWNER
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity

@Database(
    entities = [SavedLotEntity::class, ParkedCarEntity::class, OwnReportEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkingDao(): ParkingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 and Version 2 have identical schemas (no entity changed).
            }
        }

        /** 4.6: existing rows move to the device owner and need upload; the old car id column is replaced by the owner. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_lots_v3` (`ownerUid` TEXT NOT NULL, `lotId` TEXT NOT NULL, " +
                        "`parkName` TEXT NOT NULL, `lotName` TEXT NOT NULL, `region` TEXT NOT NULL, `userNotes` TEXT NOT NULL, " +
                        "`savedAtTimestamp` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, " +
                        "`version` INTEGER NOT NULL, `syncedVersion` INTEGER NOT NULL, `previousOwnerUid` TEXT, `syncError` TEXT, " +
                        "PRIMARY KEY(`ownerUid`, `lotId`))"
                )
                db.execSQL(
                    "INSERT INTO `saved_lots_v3` SELECT ?, `lotId`, `parkName`, `lotName`, `region`, `userNotes`, " +
                        "`savedAtTimestamp`, `savedAtTimestamp`, 0, 1, 0, NULL, NULL FROM `saved_lots`",
                    arrayOf(LOCAL_DEVICE_OWNER)
                )
                db.execSQL("DROP TABLE `saved_lots`")
                db.execSQL("ALTER TABLE `saved_lots_v3` RENAME TO `saved_lots`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `parked_car_v3` (`ownerUid` TEXT NOT NULL, `parkName` TEXT NOT NULL, " +
                        "`lotName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `stallNumber` TEXT NOT NULL, " +
                        "`notes` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isExactGpsLocation` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `version` INTEGER NOT NULL, " +
                        "`syncedVersion` INTEGER NOT NULL, `previousOwnerUid` TEXT, `syncError` TEXT, PRIMARY KEY(`ownerUid`))"
                )
                db.execSQL(
                    "INSERT INTO `parked_car_v3` SELECT ?, `parkName`, `lotName`, `latitude`, `longitude`, `stallNumber`, " +
                        "`notes`, `timestamp`, `isExactGpsLocation`, `timestamp`, 0, 1, 0, NULL, NULL FROM `parked_car` LIMIT 1",
                    arrayOf(LOCAL_DEVICE_OWNER)
                )
                db.execSQL("DROP TABLE `parked_car`")
                db.execSQL("ALTER TABLE `parked_car_v3` RENAME TO `parked_car`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `own_reports` (`id` TEXT NOT NULL, `ownerUid` TEXT NOT NULL, " +
                        "`lotId` TEXT NOT NULL, `parkName` TEXT NOT NULL, `reporterName` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                        "`comment` TEXT NOT NULL, `spotsEstimate` INTEGER, `observedAt` INTEGER NOT NULL, `syncState` TEXT NOT NULL, " +
                        "`rejectReason` TEXT, `previousOwnerUid` TEXT, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_own_reports_ownerUid` ON `own_reports` (`ownerUid`)")
            }
        }

        @VisibleForTesting
        fun resetForTests() = synchronized(this) {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bc_parking_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
