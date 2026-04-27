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
import dev.gaborbiro.dailymacros.data.db.model.entity.QuickPickOverrideEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RecordEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.RequestStatusEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TemplateEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.TopContributorsEntity
import java.time.LocalDateTime
import java.time.ZoneId

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
    version = 11,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 7, to = 8), // adding top_contributors table
    ]
)
@TypeConverters(Converters::class, QuickPickOverrideEntity.Converters::class)
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
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .addMigrations(MIGRATION_8_9)
            .addMigrations(MIGRATION_9_10)
            .addMigrations(MIGRATION_10_11)
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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE macros ADD COLUMN ofWhichAddedSugar REAL DEFAULT NULL"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE QuickPickOverride (
                templateId INTEGER NOT NULL,
                overrideType TEXT NOT NULL,
                PRIMARY KEY(templateId),
                FOREIGN KEY(templateId)
                    REFERENCES templates(_id)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE QuickPickOverride ADD COLUMN sortOrder INTEGER DEFAULT NULL"
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE macros ADD COLUMN analysisComponentsJson TEXT DEFAULT NULL"
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE template_images ADD COLUMN coverPhoto INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `template_images_new` (
                `templateId` INTEGER NOT NULL,
                `image` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `coverPhoto` INTEGER,
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT,
                FOREIGN KEY(`templateId`) REFERENCES `templates`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `template_images_new` (`templateId`, `image`, `sortOrder`, `coverPhoto`, `_id`)
            SELECT `templateId`, `image`, `sortOrder`, NULL, `_id` FROM `template_images`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `template_images`")
        db.execSQL("ALTER TABLE `template_images_new` RENAME TO `template_images`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_template_images_templateId` ON `template_images` (`templateId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_template_images_image` ON `template_images` (`image`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_template_images_templateId_sortOrder` ON `template_images` (`templateId`, `sortOrder`)"
        )
    }
}
