package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "earthquakes")
data class EarthquakeEntity(
    @PrimaryKey val dateTime: String,
    val tanggal: String,
    val jam: String,
    val coordinates: String,
    val lintang: String,
    val bujur: String,
    val magnitude: String,
    val kedalaman: String,
    val wilayah: String,
    val potensi: String,
    val dirasakan: String?,
    val shakemap: String?,
    val userDistance: Double?, // Distance in km
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface EarthquakeDao {
    @Query("SELECT * FROM earthquakes ORDER BY timestamp DESC")
    fun getAllEarthquakes(): Flow<List<EarthquakeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEarthquake(earthquake: EarthquakeEntity)

    @Query("DELETE FROM earthquakes")
    suspend fun clearAll()
}

@Database(entities = [EarthquakeEntity::class], version = 1, exportSchema = false)
abstract class EarthquakeDatabase : RoomDatabase() {
    abstract fun earthquakeDao(): EarthquakeDao

    companion object {
        @Volatile
        private var INSTANCE: EarthquakeDatabase? = null

        fun getDatabase(context: Context): EarthquakeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EarthquakeDatabase::class.java,
                    "earthquake_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
