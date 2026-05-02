package dev.gaborbiro.dailymacros.repositories.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupRepository
import dev.gaborbiro.dailymacros.repositories.backup.domain.model.DatabaseBackupImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

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

    private val tarDatabaseEntry: String
        get() = "databases/${AppDatabase.DATABASE_FILE_NAME}"

    override suspend fun prepareBackupArchiveForExport(): File =
        withContext(Dispatchers.IO) {
            val dbFile = appContext.getDatabasePath(AppDatabase.DATABASE_FILE_NAME)
            if (!dbFile.exists()) {
                throw IllegalStateException(
                    "Nothing to export yet — use the app once so the database is created.",
                )
            }
            val writable = AppDatabase.getInstance().openHelper.writableDatabase
            checkpointWalForExport(writable)

            val outFile = File(appContext.cacheDir, "backup_export_${System.nanoTime()}.tar")
            TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { tout ->
                tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)

                writeTarUtf8Entry(tout, BACKUP_MANIFEST_ENTRY, BACKUP_MANIFEST_JSON)

                val dbEntry = TarArchiveEntry(dbFile, tarDatabaseEntry)
                tout.putArchiveEntry(dbEntry)
                FileInputStream(dbFile).use { it.copyTo(tout) }
                tout.closeArchiveEntry()

                val publicRoot = File(appContext.filesDir, "public")
                if (publicRoot.exists()) {
                    publicRoot.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val rel = file.relativeTo(publicRoot).invariantSeparatorsPath
                            val entryName = "$PUBLIC_TAR_PREFIX/$rel"
                            val entry = TarArchiveEntry(file, entryName)
                            tout.putArchiveEntry(entry)
                            FileInputStream(file).use { it.copyTo(tout) }
                            tout.closeArchiveEntry()
                        }
                }
                val sharedPrefsRoot = File(appContext.filesDir.parentFile, "shared_prefs")
                if (sharedPrefsRoot.exists()) {
                    sharedPrefsRoot.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val rel = file.relativeTo(sharedPrefsRoot).invariantSeparatorsPath
                            val entryName = "$SHARED_PREFS_TAR_PREFIX/$rel"
                            val entry = TarArchiveEntry(file, entryName)
                            tout.putArchiveEntry(entry)
                            FileInputStream(file).use { it.copyTo(tout) }
                            tout.closeArchiveEntry()
                        }
                }
            }
            outFile
        }

    override suspend fun importBackup(source: InputStream): DatabaseBackupImportResult =
        withContext(Dispatchers.IO) {
            val temp = File(appContext.cacheDir, "import_${System.nanoTime()}.bin")
            try {
                FileOutputStream(temp).use { out -> source.copyTo(out) }
                if (temp.isTarArchive()) {
                    importTarBackup(temp)
                } else {
                    DatabaseBackupImportResult.InvalidFile
                }
            } finally {
                temp.delete()
            }
        }

    private fun importTarBackup(tarFile: File): DatabaseBackupImportResult {
        val extractRoot = File(appContext.cacheDir, "tar_import_${System.nanoTime()}")
        try {
            extractTar(tarFile, extractRoot)
            val stagedDb = File(extractRoot, tarDatabaseEntry)
            if (!stagedDb.isFile) {
                return DatabaseBackupImportResult.InvalidFile
            }
            if (!validateSqliteDatabaseFile(stagedDb)) {
                return DatabaseBackupImportResult.InvalidFile
            }
            val stagedPublic = File(extractRoot, PUBLIC_TAR_PREFIX)
            val publicToRestore = if (stagedPublic.exists()) stagedPublic else null
            val stagedSharedPrefs = File(extractRoot, SHARED_PREFS_TAR_PREFIX)
            val sharedPrefsToRestore = if (stagedSharedPrefs.exists()) stagedSharedPrefs else null
            return replaceDatabaseAndMaybeFiles(
                stagedDb = stagedDb,
                stagedPublicDir = publicToRestore,
                stagedSharedPrefsDir = sharedPrefsToRestore,
            )
        } finally {
            extractRoot.deleteRecursively()
        }
    }

    /**
     * Replaces the live DB from [stagedDb].
     * If [stagedPublicDir] is non-null, replaces [files/public] entirely.
     * If [stagedSharedPrefsDir] is non-null, replaces [shared_prefs] entirely.
     * Null staged folders are treated as legacy import and left unchanged.
     */
    private fun replaceDatabaseAndMaybeFiles(
        stagedDb: File,
        stagedPublicDir: File?,
        stagedSharedPrefsDir: File?,
    ): DatabaseBackupImportResult {
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
                return DatabaseBackupImportResult.IoFailure(err)
            }
        }

        val publicLive = File(appContext.filesDir, "public")
        val publicBak =
            if (stagedPublicDir != null && publicLive.exists()) {
                File(appContext.filesDir, "public.import_bak_$tag").also { aside ->
                    if (!publicLive.renameTo(aside)) {
                        restoreLiveDbFromAside(rollout)
                        AppDatabase.init(appContext)
                        return DatabaseBackupImportResult.IoFailure("Could not stage public image folder.")
                    }
                }
            } else {
                null
            }
        val sharedPrefsLive = File(appContext.filesDir.parentFile, "shared_prefs")
        val sharedPrefsBak =
            if (stagedSharedPrefsDir != null && sharedPrefsLive.exists()) {
                File(appContext.filesDir.parentFile, "shared_prefs.import_bak_$tag").also { aside ->
                    if (!sharedPrefsLive.renameTo(aside)) {
                        publicBak?.takeIf { it.exists() }?.renameTo(publicLive)
                        restoreLiveDbFromAside(rollout)
                        AppDatabase.init(appContext)
                        return DatabaseBackupImportResult.IoFailure("Could not stage shared preferences folder.")
                    }
                }
            } else {
                null
            }

        try {
            stagedDb.copyTo(rollout.main, overwrite = false)

            SQLiteDatabase.openDatabase(
                rollout.main.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { verify ->
                verify.rawQuery("SELECT count(*) FROM sqlite_master", null)
                    .use { it.moveToFirst() }
            }

            if (stagedPublicDir != null) {
                if (publicLive.exists()) {
                    publicLive.deleteRecursively()
                }
                publicLive.mkdirs()
                stagedPublicDir.copyRecursively(publicLive, overwrite = true)
            }
            if (stagedSharedPrefsDir != null) {
                if (sharedPrefsLive.exists()) {
                    sharedPrefsLive.deleteRecursively()
                }
                sharedPrefsLive.mkdirs()
                stagedSharedPrefsDir.copyRecursively(sharedPrefsLive, overwrite = true)
            }
        } catch (e: Throwable) {
            if (stagedSharedPrefsDir != null) {
                sharedPrefsLive.deleteRecursively()
                sharedPrefsBak?.takeIf { it.exists() }?.renameTo(sharedPrefsLive)
            }
            if (stagedPublicDir != null) {
                publicLive.deleteRecursively()
                publicBak?.takeIf { it.exists() }?.renameTo(publicLive)
            }
            discardFailedReplacement(rollout)
            restoreLiveDbFromAside(rollout)
            AppDatabase.init(appContext)
            return DatabaseBackupImportResult.IoFailure(e.message ?: e.toString())
        }

        rollout.mainBak.takeIf { it.exists() }?.delete()
        rollout.walBak.takeIf { it.exists() }?.delete()
        rollout.shmBak.takeIf { it.exists() }?.delete()
        publicBak?.takeIf { it.exists() }?.deleteRecursively()
        sharedPrefsBak?.takeIf { it.exists() }?.deleteRecursively()

        return DatabaseBackupImportResult.ReplacementApplied
    }

    private fun extractTar(tarFile: File, destDir: File) {
        destDir.mkdirs()
        val rootPath = destDir.canonicalPath
        TarArchiveInputStream(BufferedInputStream(FileInputStream(tarFile))).use { tin ->
            while (true) {
                val entry = tin.nextTarEntry ?: break
                if (entry.name.isBlank()) continue
                val normalized = entry.name.trimStart('/').replace('\\', '/')
                val outFile = File(destDir, normalized)
                val outPath = outFile.canonicalPath
                require(
                    outPath.startsWith("$rootPath${File.separator}") || outPath == rootPath,
                ) {
                    "Illegal tar entry path: ${entry.name}"
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> tin.copyTo(fos) }
                }
            }
        }
    }

    private fun writeTarUtf8Entry(tout: TarArchiveOutputStream, entryName: String, utf8: String) {
        val bytes = utf8.toByteArray(StandardCharsets.UTF_8)
        val entry = TarArchiveEntry(entryName)
        entry.size = bytes.size.toLong()
        tout.putArchiveEntry(entry)
        tout.write(bytes)
        tout.closeArchiveEntry()
    }

    private fun File.isSqliteMagicFile(): Boolean {
        if (!isFile || length() < SQLITE_MAGIC.size) return false
        FileInputStream(this).use { stream ->
            val header = ByteArray(SQLITE_MAGIC.size)
            if (stream.read(header) != header.size) return false
            return header.contentEquals(SQLITE_MAGIC)
        }
    }

    private fun File.isTarArchive(): Boolean =
        try {
            TarArchiveInputStream(BufferedInputStream(FileInputStream(this))).use { tin ->
                tin.nextTarEntry != null
            }
        } catch (_: Exception) {
            false
        }

    private fun validateSqliteDatabaseFile(file: File): Boolean {
        if (!file.isFile || !file.isSqliteMagicFile()) return false
        return try {
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { sanity ->
                sanity.rawQuery("SELECT count(*) FROM sqlite_master", null)
                    .use { it.moveToFirst() }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun checkpointWalForExport(writable: SupportSQLiteDatabase) {
        repeat(CHECKPOINT_MAX_ATTEMPTS) { attempt ->
            writable.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)")).use { cursor ->
                if (!cursor.moveToFirst()) {
                    return
                }
                val busy = cursor.getInt(0)
                if (busy == 0) {
                    return
                }
            }
            if (attempt < CHECKPOINT_MAX_ATTEMPTS - 1) {
                Thread.sleep(CHECKPOINT_RETRY_DELAY_MS)
            }
        }
        throw IllegalStateException(
            "Could not flush the database for export (journal busy). Try again in a moment.",
        )
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

    private companion object {
        const val CHECKPOINT_MAX_ATTEMPTS = 40
        const val CHECKPOINT_RETRY_DELAY_MS = 25L

        const val BACKUP_MANIFEST_ENTRY = "backup_manifest.json"
        const val BACKUP_MANIFEST_JSON = """{"format":1,"kind":"dailymacros-full"}"""
        const val PUBLIC_TAR_PREFIX = "files/public"
        const val SHARED_PREFS_TAR_PREFIX = "shared_prefs"
    }
}

private val File.invariantSeparatorsPath: String
    get() = path.replace(File.separatorChar, '/')
