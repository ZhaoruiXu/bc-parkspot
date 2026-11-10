package ca.bcparkspot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingDao {
    @Query("SELECT * FROM saved_lots ORDER BY savedAtTimestamp DESC")
    fun getAllSavedLots(): Flow<List<SavedLotEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_lots WHERE lotId = :lotId)")
    fun isLotSaved(lotId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLot(lot: SavedLotEntity)

    @Query("DELETE FROM saved_lots WHERE lotId = :lotId")
    suspend fun deleteSavedLot(lotId: String)

    @Query("DELETE FROM saved_lots")
    suspend fun clearAllSavedLots()

    @Query("SELECT * FROM parked_car WHERE id = 1 LIMIT 1")
    fun getParkedCar(): Flow<ParkedCarEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveParkedCar(car: ParkedCarEntity)

    @Query("DELETE FROM parked_car WHERE id = 1")
    suspend fun clearParkedCar()
}
