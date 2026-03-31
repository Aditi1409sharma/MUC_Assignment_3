package com.example.mucassignment3.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "safety_events")
data class SafetyEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val distance: Float,
    val light: Int,
    val state: String
)

@Dao
interface SafetyDao {
    @Insert
    suspend fun insertEvent(event: SafetyEvent)
}

@Database(entities = [SafetyEvent::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun safetyDao(): SafetyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safety_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}