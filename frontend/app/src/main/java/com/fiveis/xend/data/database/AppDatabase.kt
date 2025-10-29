package com.fiveis.xend.data.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import com.fiveis.xend.data.model.EmailItem

/**
 * v1 -> v2: Contact/Group 관련 테이블 추가
 * 테이블 추가만 있으므로 AutoMigration
 */
@Database(
    entities = [
        EmailItem::class,
        GroupEntity::class,
        ContactEntity::class,
        ContactContextEntity::class,
        PromptOptionEntity::class,
        GroupPromptOptionCrossRef::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao
    abstract fun groupDao(): GroupDao
    abstract fun contactDao(): ContactDao
    abstract fun promptOptionDao(): PromptOptionDao

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
