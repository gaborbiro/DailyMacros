package dev.gaborbiro.dailymacros.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.gaborbiro.dailymacros.data.db.model.entity.NutrientsEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity

@Database(
    entities = [RecordEntity::class, TemplateEntity::class, NutrientsEntity::class, ImageEntity::class],
    version = 2,
    exportSchema = true
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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
