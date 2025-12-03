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
import com.fiveis.xend.data.database.entity.ProfileEntity
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import com.fiveis.xend.data.model.DraftItem
import com.fiveis.xend.data.model.EmailItem

/**
 * v1 -> v2: Contact/Group 관련 테이블 추가, EmailItem에 body 필드 추가
 * v2 -> v3: body 필드 DEFAULT 값 추가 (스키마만 변경, 마이그레이션 불필요)
 * v3 -> v4: drafts 테이블 추가
 * v4 -> v5: groups 테이블에 emoji 필드 추가, EmailItem에 toEmail/attachments 컬럼 추가
 * v5 -> v6: emails 테이블에 dateTimestamp 필드 추가 (정렬용 epoch timestamp)
 * v6 -> v7: emails 테이블에 displayDate/displaySenderName 필드 추가 (UI 최적화)
 * v7 -> v8: profile 테이블 추가 (프로필 캐시)
 * v8 -> v9: emails 테이블에 sourceLabel 필드 추가 (캐시 출처 구분)
 */
@Database(
    entities = [
        EmailItem::class,
        DraftItem::class,
        GroupEntity::class,
        ContactEntity::class,
        ContactContextEntity::class,
        PromptOptionEntity::class,
        GroupPromptOptionCrossRef::class,
        ProfileEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao
    abstract fun groupDao(): GroupDao
    abstract fun contactDao(): ContactDao
    abstract fun promptOptionDao(): PromptOptionDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Migration from v1 to v2:
         * - Add body column to emails table
         * - Create Contact/Group related tables
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add body column to emails table
                database.execSQL("ALTER TABLE emails ADD COLUMN body TEXT NOT NULL DEFAULT ''")

                // Create groups table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `groups` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `createdAt` TEXT,
                        `updatedAt` TEXT,
                        PRIMARY KEY(`id`)
                    )"""
                )

                // Create contacts table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `contacts` (
                        `id` INTEGER NOT NULL,
                        `groupId` INTEGER,
                        `name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `createdAt` TEXT,
                        `updatedAt` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )"""
                )

                // Create index for contacts.groupId
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_contacts_groupId` ON `contacts` (`groupId`)"
                )

                // Create contact_contexts table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `contact_contexts` (
                        `contactId` INTEGER NOT NULL,
                        `senderRole` TEXT,
                        `recipientRole` TEXT,
                        `relationshipDetails` TEXT,
                        `personalPrompt` TEXT,
                        `languagePreference` TEXT,
                        `createdAt` TEXT,
                        `updatedAt` TEXT,
                        PRIMARY KEY(`contactId`)
                    )"""
                )

                // Create prompt_options table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `prompt_options` (
                        `id` INTEGER NOT NULL,
                        `key` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `prompt` TEXT NOT NULL,
                        `createdAt` TEXT,
                        `updatedAt` TEXT,
                        PRIMARY KEY(`id`)
                    )"""
                )

                // Create group_prompt_option_cross_ref table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `group_prompt_option_cross_ref` (
                        `groupId` INTEGER NOT NULL,
                        `optionId` INTEGER NOT NULL,
                        PRIMARY KEY(`groupId`, `optionId`),
                        FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`optionId`) REFERENCES `prompt_options`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )"""
                )

                // Create indices for group_prompt_option_cross_ref
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_group_prompt_option_cross_ref_groupId` " +
                        "ON `group_prompt_option_cross_ref` (`groupId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_group_prompt_option_cross_ref_optionId` " +
                        "ON `group_prompt_option_cross_ref` (`optionId`)"
                )
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

        /**
         * Migration from v3 to v4:
         * - Create drafts table with recipients column
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `drafts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `recipients` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )"""
                )
            }
        }

        /**
         * Migration from v4 to v5:
         * - Add toEmail + attachments columns to emails table (stores metadata references)
         * - Add emoji column to groups table
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add toEmail and attachments to emails table
                database.execSQL(
                    "ALTER TABLE emails ADD COLUMN toEmail TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE emails ADD COLUMN attachments TEXT NOT NULL DEFAULT '[]'"
                )

                // Add emoji to groups table
                database.execSQL("ALTER TABLE `groups` ADD COLUMN `emoji` TEXT")
            }
        }

        /**
         * Migration from v5 to v6:
         * - Add dateTimestamp column to emails table for proper chronological sorting
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE emails ADD COLUMN dateTimestamp INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Migration from v6 to v7:
         * - Add displayDate and displaySenderName columns for cached UI strings
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE emails ADD COLUMN displayDate TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE emails ADD COLUMN displaySenderName TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * Migration from v7 to v8:
         * - Create profile table for caching profile data
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `profile` (
                        `id` INTEGER NOT NULL,
                        `displayName` TEXT,
                        `info` TEXT,
                        `languagePreference` TEXT,
                        PRIMARY KEY(`id`)
                    )"""
                )
            }
        }

        /**
         * Migration from v8 to v9:
         * - Add sourceLabel column to emails table to track mailbox origin
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE emails ADD COLUMN sourceLabel TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    """
                    UPDATE emails SET sourceLabel = 
                    CASE
                        WHEN labelIds LIKE '%INBOX%' THEN 'INBOX'
                        WHEN labelIds LIKE '%SENT%' THEN 'SENT'
                        ELSE ''
                    END
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xend_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigration() // Add this line
                    .build()
                instance = newInstance
                newInstance
            }
        }
    }
}
