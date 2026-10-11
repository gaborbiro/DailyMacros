package dev.gaborbiro.dailymacros.repositories.backup.domain

/**
 * Identifies this install as the writer of a cloud backup, so a backup this install uploaded
 * itself (but failed to record locally) isn't mistaken for another device's write.
 */
interface BackupClientIdProvider {
    val clientId: String
}
