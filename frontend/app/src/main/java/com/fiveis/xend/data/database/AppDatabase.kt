package com.fiveis.xend.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fiveis.xend.data.model.EmailItem

@Database(
    entities = [EmailItem::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xend_database"
                ).build()
                instance = newInstance
                newInstance
            }
        }
    }
}
