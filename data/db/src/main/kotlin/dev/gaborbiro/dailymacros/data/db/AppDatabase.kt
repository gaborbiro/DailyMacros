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
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityArchetypeEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySnapshotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilitySlotEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEntity
import dev.gaborbiro.dailymacros.data.db.model.entity.VariabilityVariantEvidenceEntity
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
        VariabilitySnapshotEntity::class,
        VariabilityArchetypeEntity::class,
        VariabilitySlotEntity::class,
        VariabilityVariantEntity::class,
        VariabilityVariantEvidenceEntity::class,
    ],
    version = 13,
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
    abstract fun variabilityDao(): VariabilityDao

    companion object {

        const val DATABASE_FILE_NAME: String = "daily_macros_db"

        private val lock = Any()

        @Volatile
        private var instance: AppDatabase? = null

        fun init(appContext: Context) {
            synchronized(lock) {
                if (instance == null) {
                    instance = buildDatabase(appContext)
                }
            }
        }

        fun getInstance(): AppDatabase =
            synchronized(lock) {
                instance ?: error("AppDatabase not initialized")
            }

        /**
         * Closes the Room instance and clears the singleton holder.
         * Used when replacing the database file on disk (e.g. restore from backup).
         * The process should be restarted shortly after so [init] runs again on cold start.
         */
        fun closeSingleton() {
            synchronized(lock) {
                instance?.close()
                instance = null
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_FILE_NAME
            )
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .addMigrations(MIGRATION_8_9)
            .addMigrations(MIGRATION_9_10)
            .addMigrations(MIGRATION_10_11)
            .addMigrations(MIGRATION_11_12)
            .addMigrations(MIGRATION_12_13)
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
            "ALTER TABLE template_images ADD COLUMN isRepresentativeMealPhoto INTEGER DEFAULT NULL"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `variability_snapshots` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `minedAtEpochMs` INTEGER NOT NULL,
                `profileJson` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `variability_archetypes` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `snapshotId` INTEGER NOT NULL,
                `archetypeKey` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `titleAliasesJson` TEXT NOT NULL,
                `evidenceCount` INTEGER NOT NULL,
                `lastSeenTimestamp` TEXT,
                `archetypeNotes` TEXT,
                `deprecated` INTEGER NOT NULL,
                `deprecatedReason` TEXT,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`snapshotId`) REFERENCES `variability_snapshots`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_archetypes_snapshotId` ON `variability_archetypes` (`snapshotId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `variability_slots` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `archetypeId` INTEGER NOT NULL,
                `slotKey` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `nutritionalLeversJson` TEXT NOT NULL,
                `isHighVariability` INTEGER NOT NULL,
                `confidence` REAL NOT NULL,
                `rationale` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`archetypeId`) REFERENCES `variability_archetypes`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_slots_archetypeId` ON `variability_slots` (`archetypeId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `variability_variants` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `slotId` INTEGER NOT NULL,
                `variantKey` TEXT NOT NULL,
                `variantLabel` TEXT NOT NULL,
                `macroSource` TEXT NOT NULL,
                `notesExcerpt` TEXT NOT NULL,
                `typicalMacrosJson` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`slotId`) REFERENCES `variability_slots`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_variants_slotId` ON `variability_variants` (`slotId`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `variability_variant_evidence` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `variantId` INTEGER NOT NULL,
                `loggedAt` TEXT NOT NULL,
                `templateId` INTEGER,
                FOREIGN KEY(`variantId`) REFERENCES `variability_variants`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_variant_evidence_variantId` ON `variability_variant_evidence` (`variantId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_variant_evidence_templateId` ON `variability_variant_evidence` (`templateId`)"
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Room runs migrations inside a transaction. Rebuilding a parent table that other tables
        // reference requires deferring FK checks until commit — PRAGMA foreign_keys=OFF is a no-op
        // inside a transaction on SQLite.
        db.execSQL("PRAGMA defer_foreign_keys = ON")
        db.execSQL("ALTER TABLE `templates` RENAME TO `templates_old`")
        db.execSQL(
            """
            CREATE TABLE `templates` (
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `parentTemplateId` INTEGER,
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT,
                FOREIGN KEY(`parentTemplateId`) REFERENCES `templates`(`_id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `templates` (`name`, `description`, `parentTemplateId`, `_id`)
            SELECT `name`, `description`, NULL, `_id` FROM `templates_old`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `templates_old`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_templates_parentTemplateId` ON `templates` (`parentTemplateId`)",
        )
        db.execSQL("DELETE FROM sqlite_sequence WHERE `name` = 'templates'")
        db.execSQL(
            """
            INSERT INTO sqlite_sequence (`name`, `seq`)
            SELECT 'templates', IFNULL(MAX(`_id`), 0) FROM `templates`
            """.trimIndent(),
        )
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE variability_variant_evidence ADD COLUMN mealTitle TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA defer_foreign_keys = ON")
        db.execSQL(
            """
            CREATE TABLE `variability_variant_evidence_backup` (
                `_id` INTEGER PRIMARY KEY NOT NULL,
                `variantId` INTEGER NOT NULL,
                `loggedAt` TEXT NOT NULL,
                `templateId` INTEGER,
                `mealTitle` TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "INSERT INTO `variability_variant_evidence_backup` SELECT * FROM `variability_variant_evidence`",
        )
        db.execSQL("DROP TABLE `variability_variant_evidence`")
        db.execSQL(
            """
            CREATE TABLE `variability_variants_new` (
                `_id` INTEGER PRIMARY KEY NOT NULL,
                `slotId` INTEGER NOT NULL,
                `variantKey` TEXT NOT NULL,
                `variantLabel` TEXT NOT NULL,
                `notesExcerpt` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`slotId`) REFERENCES `variability_slots`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `variability_variants_new` (`_id`, `slotId`, `variantKey`, `variantLabel`, `notesExcerpt`, `sortOrder`)
            SELECT `_id`, `slotId`, `variantKey`, `variantLabel`, `notesExcerpt`, `sortOrder` FROM `variability_variants`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `variability_variants`")
        db.execSQL("ALTER TABLE `variability_variants_new` RENAME TO `variability_variants`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_variants_slotId` ON `variability_variants` (`slotId`)",
        )
        db.execSQL(
            """
            CREATE TABLE `variability_variant_evidence` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `variantId` INTEGER NOT NULL,
                `loggedAt` TEXT NOT NULL,
                `templateId` INTEGER,
                `mealTitle` TEXT,
                FOREIGN KEY(`variantId`) REFERENCES `variability_variants`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `variability_variant_evidence` (`_id`, `variantId`, `loggedAt`, `templateId`, `mealTitle`)
            SELECT `_id`, `variantId`, `loggedAt`, `templateId`, `mealTitle` FROM `variability_variant_evidence_backup`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `variability_variant_evidence_backup`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_variant_evidence_variantId` ON `variability_variant_evidence` (`variantId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_variability_variant_evidence_templateId` ON `variability_variant_evidence` (`templateId`)",
        )
        db.execSQL("DELETE FROM sqlite_sequence WHERE `name` IN ('variability_variants', 'variability_variant_evidence')")
        db.execSQL(
            """
            INSERT INTO sqlite_sequence (`name`, `seq`)
            SELECT 'variability_variants', IFNULL(MAX(`_id`), 0) FROM `variability_variants`
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO sqlite_sequence (`name`, `seq`)
            SELECT 'variability_variant_evidence', IFNULL(MAX(`_id`), 0) FROM `variability_variant_evidence`
            """.trimIndent(),
        )
    }
}
