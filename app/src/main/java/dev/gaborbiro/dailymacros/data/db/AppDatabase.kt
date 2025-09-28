package dev.gaborbiro.dailymacros.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.gaborbiro.dailymacros.data.db.model.entity.ImageEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.MacrosEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatusEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import java.time.LocalDateTime
import java.time.ZoneId

@Database(
    entities = [
        RecordEntity::class,
        TemplateEntity::class,
        MacrosEntity::class,
        ImageEntity::class,
        RequestStatusEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordsDAO(): RecordsDAO
    abstract fun templatesDAO(): TemplatesDAO
    abstract fun requestStatusDAO(): RequestStatusDAO

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
                "daily_macros_db"
            )
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .build()
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {
        val zoneIdString: String = ZoneId.systemDefault().id

        database.execSQL(
            "ALTER TABLE records ADD COLUMN zoneId TEXT NOT NULL DEFAULT '$zoneIdString'"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE records ADD COLUMN epochMillis INTEGER NOT NULL DEFAULT 0"
        )

        val cursor = database.query("SELECT _id, timestamp, zoneId FROM records")
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val localDateTimeStr = cursor.getString(1)
            val zoneIdStr = cursor.getString(2)

            val ldt = LocalDateTime.parse(localDateTimeStr)
            val zone = ZoneId.of(zoneIdStr)
            val epochMillis = ldt.atZone(zone).toInstant().toEpochMilli()

            database.execSQL(
                "UPDATE records SET epochMillis = ? WHERE _id = ?",
                arrayOf(epochMillis, id)
            )
        }
        cursor.close()
    }
}
