package ca.bcparkspot.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_lots")
data class SavedLotEntity(
    @PrimaryKey val lotId: String,
    val parkName: String,
    val lotName: String,
    val region: String,
    val savedAtTimestamp: Long = System.currentTimeMillis(),
    val userNotes: String = ""
)

@Entity(tableName = "parked_car")
data class ParkedCarEntity(
    @PrimaryKey val id: Int = 1,
    val parkName: String,
    val lotName: String,
    val latitude: Double,
    val longitude: Double,
    val stallNumber: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isExactGpsLocation: Boolean = false
)
