package dev.gaborbiro.dailymacros.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.QuickPickOverrideEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatusEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TopContributorsEntity

@Database(
    entities = [
        RecordEntity::class,
        TemplateEntity::class,
        MacrosEntity::class,
        TopContributorsEntity::class,
        ImageEntity::class,
        RequestStatusEntity::class,
        QuickPickOverrideEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class, QuickPickOverrideEntity.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordsDAO(): RecordsDAO
    abstract fun templatesDAO(): TemplatesDAO
    abstract fun requestStatusDAO(): RequestStatusDAO

    companion object {

        const val DATABASE_FILE_NAME: String = "daily_macros_db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE template_images ADD COLUMN sourceMediaStoreId INTEGER")
            }
        }

        internal fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_FILE_NAME,
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
