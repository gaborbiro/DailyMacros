package dev.gaborbiro.dailymacros.store.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.ProvidedTypeConverter
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import dev.gaborbiro.dailymacros.store.db.records.RecordsDAO
import dev.gaborbiro.dailymacros.store.db.records.TemplatesDAO
import dev.gaborbiro.dailymacros.store.db.records.model.RecordDBModel
import dev.gaborbiro.dailymacros.store.db.records.model.TemplateDBModel

@Database(
    entities = [RecordDBModel::class, TemplateDBModel::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordsDAO(): RecordsDAO
    abstract fun templatesDAO(): TemplatesDAO

    companion object {

        @Volatile
        private lateinit var INSTANCE: AppDatabase

        fun init(appContext: Context) {
            INSTANCE = buildDatabase(appContext)
        }

        fun getInstance() = INSTANCE

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_db"
            )
                .build()
        }
    }
}
