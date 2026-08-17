package dev.gaborbiro.dailymacros.repositories.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.repositories.settings.domain.PendingDriveSyncInfoStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingDriveSyncInfoStoreImpl @Inject constructor(
    @ApplicationContext context: Context,
) : PendingDriveSyncInfoStore {

    private val file = File(context.filesDir, "pending_drive_sync_info")

    override fun write(driveModifiedAtMs: Long) {
        file.writeText(driveModifiedAtMs.toString())
    }

    override fun consumeIfPresent(): Long? {
        if (!file.isFile) return null
        val value = file.readText().toLongOrNull()
        file.delete()
        return value
    }
}
