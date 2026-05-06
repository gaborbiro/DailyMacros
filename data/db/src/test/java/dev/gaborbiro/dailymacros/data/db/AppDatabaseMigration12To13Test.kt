package dev.gaborbiro.dailymacros.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs the real [MIGRATION_12_13] against a v12 database created from the exported Room schema,
 * then validates the result against the exported v13 schema (see [MigrationTestHelper]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class AppDatabaseMigration12To13Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate12To13_matchesExportedSchemaAndDefaultsNewColumns() {
        val dbName = "migrate-12-13-test"

        helper.createDatabase(dbName, 12).use { db ->
            seedV12(db)
        }

        helper.runMigrationsAndValidate(dbName, 13, true, MIGRATION_12_13).use { migrated ->
            migrated.query(
                "SELECT createdAtEpochMs, updatedAtEpochMs FROM templates WHERE _id = 77",
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0L, c.getLong(0))
                assertEquals(0L, c.getLong(1))
            }
            migrated.query(
                "SELECT templatesIngestWatermarkEpochMs FROM variability_snapshots WHERE _id = 3",
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0L, c.getLong(0))
            }
        }
    }

    private fun seedV12(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO templates (name, description, parentTemplateId, _id)
            VALUES ('t', 'd', NULL, 77)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO variability_snapshots (_id, minedAtEpochMs, profileJson)
            VALUES (3, 1, '{}')
            """.trimIndent(),
        )
    }
}
