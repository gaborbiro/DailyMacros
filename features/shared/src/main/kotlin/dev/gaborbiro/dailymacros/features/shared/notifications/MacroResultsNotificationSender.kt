package dev.gaborbiro.dailymacros.features.shared.notifications

interface MacroResultsNotificationSender {
    fun showMacroResultsNotification(
        id: Long,
        recordId: Long,
        title: String?,
        message: String?,
        isError: Boolean,
    )
}
