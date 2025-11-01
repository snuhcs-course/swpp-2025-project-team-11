package com.fiveis.xend.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import com.fiveis.xend.data.model.EmailItem

/**
 * v1 -> v2: Contact/Group 관련 테이블 추가, EmailItem에 body 필드 추가
 * v2 -> v3: body 필드 DEFAULT 값 추가 (스키마만 변경, 마이그레이션 불필요)
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
    version = 3,
    exportSchema = true
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

        /**
         * Migration from v1 to v2:
         * - Add body column to emails table
         * - Contact/Group tables are created automatically by Room
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add body column to emails table
                database.execSQL("ALTER TABLE emails ADD COLUMN body TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration from v2 to v3:
         * - No schema changes (only identityHash changed due to DEFAULT value fix)
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No migration needed - v2 and v3 schemas are identical
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xend_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                instance = newInstance
                newInstance
            }
        }
    }
}
