package dev.gaborbiro.dailymacros.features.shared.notifications

interface TitleTextNotificationSender {
    fun showTitleTextNotification(
        id: Int,
        title: String,
        text: String,
        isError: Boolean,
    )
}
