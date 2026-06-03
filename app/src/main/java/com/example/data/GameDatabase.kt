package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// Game Progress Entity
@Entity(tableName = "game_progress")
data class GameProgress(
    @PrimaryKey val id: Int = 1,
    val sector: String = "", // "TECH", "RETAIL", "LOGISTICS" or "" if not chosen
    val cash: Double = 500.0,
    val totalTaps: Int = 0,
    val loanAmount: Double = 0.0,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val departmentsJson: String = "",
    val activeUpgradesJson: String = "",
    val activeEventJson: String = ""
)

// Data Models for Game Progress
data class DepartmentState(
    val id: String,
    val name: String,
    val level: Int,
    val baseCost: Double,
    val costMultiplier: Double,
    val baseRevenue: Double,
    val revenueMultiplier: Double,
    val isAutomated: Boolean,
    val managerCost: Double,
    val managerName: String
)

data class ActiveUpgradeState(
    val id: String,
    val title: String,
    val departmentId: String,
    val cost: Double,
    val currentLevel: Int,
    val targetLevel: Int,
    val totalDurationSeconds: Long,
    val finishTimestamp: Long, // Epoch timestamp in MS
    val hasStarted: Boolean = false,
    val isFinished: Boolean = false
)

data class MarketEventState(
    val id: String,
    val title: String,
    val description: String,
    val revenueMultiplier: Double,
    val finishTimestamp: Long, // Epoch timestamp in MS
    val type: String // "BOOM", "CRASH", "NORMAL"
)

// Moshi Converters for Room
class GameConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromString(value: String): GameProgress? {
        return moshi.adapter(GameProgress::class.java).fromJson(value)
    }

    @TypeConverter
    fun toString(progress: GameProgress): String {
        return moshi.adapter(GameProgress::class.java).toJson(progress)
    }
}

@Dao
interface GameProgressDao {
    @Query("SELECT * FROM game_progress WHERE id = :id LIMIT 1")
    suspend fun getProgress(id: Int = 1): GameProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: GameProgress)

    @Query("DELETE FROM game_progress")
    suspend fun clearProgress()
}

@Database(entities = [GameProgress::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameProgressDao(): GameProgressDao
}
