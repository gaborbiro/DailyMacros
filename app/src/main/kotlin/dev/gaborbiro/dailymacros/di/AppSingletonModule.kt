package dev.gaborbiro.dailymacros.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.features.settings.SettingsAppInfo
import dev.gaborbiro.dailymacros.features.shared.notifications.MacroResultsNotificationSender
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.ClientIdProvider
import dev.gaborbiro.dailymacros.util.showMacroResultsNotification
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSingletonModule {

    @Provides
    @Singleton
    fun gson(): Gson = Gson()

    @Provides
    @Singleton
    fun settingsAppInfo(appPrefs: AppPrefs): SettingsAppInfo =
        object : SettingsAppInfo {
            override val versionLabel: String
                get() = "${BuildConfig.VERSION_NAME}\nUserID: ${appPrefs.userUUID}"
        }

    // Same source as the "UserID" shown on the Settings screen above, so the id
    // the proxy stores matches exactly what a user reports in a support email.
    @Provides
    @Singleton
    fun clientIdProvider(appPrefs: AppPrefs): ClientIdProvider =
        object : ClientIdProvider {
            override val clientId: String
                get() = appPrefs.userUUID
        }

    @Provides
    @Singleton
    fun macroResultsNotificationSender(@ApplicationContext context: Context): MacroResultsNotificationSender =
        object : MacroResultsNotificationSender {
            override fun showMacroResultsNotification(
                id: Long,
                recordId: Long,
                title: String?,
                message: String,
                isError: Boolean,
            ) {
                context.showMacroResultsNotification(id, recordId, title, message, isError)
            }
        }
}
