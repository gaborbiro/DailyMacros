package dev.gaborbiro.dailymacros.repositories.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DatabaseBackupImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

private class ImportRollout(
    val main: File,
    val wal: File,
    val shm: File,
    val mainBak: File,
    val walBak: File,
    val shmBak: File,
) {
    var shmMoved: Boolean = false
    var walMoved: Boolean = false
    var mainMoved: Boolean = false
}

class BackupRepositoryImpl(
    context: Context,
) : BackupRepository {

    private val appContext = context.applicationContext

    override suspend fun prepareSqliteFileForExport(): File =
        withContext(Dispatchers.IO) {
            val dbFile = appContext.getDatabasePath(AppDatabase.DATABASE_FILE_NAME)
            if (!dbFile.exists()) {
                throw IllegalStateException(
                    "Nothing to export yet — use the app once so the database is created.",
                )
            }
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            ).use { raw ->
                raw.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
            }
            dbFile
        }

    override suspend fun importSqliteReplacement(source: InputStream): DatabaseBackupImportResult =
        withContext(Dispatchers.IO) {
            val temp = File(appContext.cacheDir, "db_restore_${System.nanoTime()}.db")
            try {
                try {
                    FileOutputStream(temp).use { output ->
                        source.copyTo(output)
                        output.fd.sync()
                    }
                } catch (e: Exception) {
                    return@withContext DatabaseBackupImportResult.IoFailure(
                        e.message ?: e.toString(),
                    )
                }

                FileInputStream(temp).use { headerStream ->
                    val header = ByteArray(SQLITE_MAGIC.size)
                    var offset = 0
                    while (offset < header.size) {
                        val n = headerStream.read(header, offset, header.size - offset)
                        if (n < 0) return@withContext DatabaseBackupImportResult.InvalidFile
                        offset += n
                    }
                    if (!header.contentEquals(SQLITE_MAGIC)) {
                        return@withContext DatabaseBackupImportResult.InvalidFile
                    }
                }

                try {
                    SQLiteDatabase.openDatabase(
                        temp.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY,
                    ).use { sanity ->
                        sanity.rawQuery("SELECT count(*) FROM sqlite_master", null)
                            .use { it.moveToFirst() }
                    }
                } catch (_: Exception) {
                    return@withContext DatabaseBackupImportResult.InvalidFile
                }

                AppDatabase.closeSingleton()

                val dbFile = appContext.getDatabasePath(AppDatabase.DATABASE_FILE_NAME)
                dbFile.parentFile?.mkdirs()

                val tag = ".import_swap_${System.nanoTime()}"
                val base = AppDatabase.DATABASE_FILE_NAME
                val dir = dbFile.parentFile!!
                val rollout = ImportRollout(
                    main = dbFile,
                    wal = File(dir, "$base-wal"),
                    shm = File(dir, "$base-shm"),
                    mainBak = File(dir, "$base$tag"),
                    walBak = File(dir, "$base-wal$tag"),
                    shmBak = File(dir, "$base-shm$tag"),
                )

                moveLiveDbAsideOrRollback(rollout).let { err ->
                    if (err != null) {
                        AppDatabase.init(appContext)
                        return@withContext DatabaseBackupImportResult.IoFailure(err)
                    }
                }

                try {
                    temp.copyTo(rollout.main, overwrite = false)

                    SQLiteDatabase.openDatabase(
                        rollout.main.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY,
                    ).use { verify ->
                        verify.rawQuery("SELECT count(*) FROM sqlite_master", null)
                            .use { it.moveToFirst() }
                    }
                } catch (e: Throwable) {
                    discardFailedReplacement(rollout)
                    restoreLiveDbFromAside(rollout)
                    AppDatabase.init(appContext)
                    return@withContext DatabaseBackupImportResult.IoFailure(
                        e.message ?: e.toString(),
                    )
                }

                rollout.mainBak.takeIf { it.exists() }?.delete()
                rollout.walBak.takeIf { it.exists() }?.delete()
                rollout.shmBak.takeIf { it.exists() }?.delete()

                DatabaseBackupImportResult.ReplacementApplied
            } finally {
                temp.delete()
            }
        }

    private fun moveLiveDbAsideOrRollback(rollout: ImportRollout): String? {
        try {
            if (rollout.shm.exists()) {
                if (!rollout.shm.renameTo(rollout.shmBak)) {
                    return "Could not stage database files for import."
                }
                rollout.shmMoved = true
            }
            if (rollout.wal.exists()) {
                if (!rollout.wal.renameTo(rollout.walBak)) {
                    restoreLiveDbFromAside(rollout)
                    return "Could not stage database files for import."
                }
                rollout.walMoved = true
            }
            if (rollout.main.exists()) {
                if (!rollout.main.renameTo(rollout.mainBak)) {
                    restoreLiveDbFromAside(rollout)
                    return "Could not stage database files for import."
                }
                rollout.mainMoved = true
            }
        } catch (e: Exception) {
            restoreLiveDbFromAside(rollout)
            return e.message ?: e.toString()
        }
        return null
    }

    private fun restoreLiveDbFromAside(rollout: ImportRollout) {
        if (rollout.mainMoved) {
            rollout.main.delete()
            rollout.mainBak.renameTo(rollout.main)
            rollout.mainMoved = false
        }
        if (rollout.walMoved) {
            rollout.wal.delete()
            rollout.walBak.renameTo(rollout.wal)
            rollout.walMoved = false
        }
        if (rollout.shmMoved) {
            rollout.shm.delete()
            rollout.shmBak.renameTo(rollout.shm)
            rollout.shmMoved = false
        }
    }

    private fun discardFailedReplacement(rollout: ImportRollout) {
        rollout.main.delete()
        rollout.wal.delete()
        rollout.shm.delete()
    }
}
