package dev.gaborbiro.dailymacros.features.shared.notifications

interface MacroResultsNotificationSender {
    fun showMacroResultsNotification(
        id: Long,
        recordId: Long,
        title: String?,
        message: String,
        isError: Boolean,
        /** When true, tapping the notification deep-links straight to the paywall instead of
         *  Overview - this is the only actionable thing the user can do about it. */
        subscriptionRequired: Boolean = false,
    )
}
