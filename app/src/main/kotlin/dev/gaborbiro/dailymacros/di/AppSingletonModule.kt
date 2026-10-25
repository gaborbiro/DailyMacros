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
import dev.gaborbiro.dailymacros.core.featureflags.RemoteConfigClientIdProvider
import dev.gaborbiro.dailymacros.features.settings.SettingsAppInfo
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import dev.gaborbiro.dailymacros.features.shared.di.ApplicationScope
import dev.gaborbiro.dailymacros.features.shared.notifications.MacroResultsNotificationSender
import dev.gaborbiro.dailymacros.repositories.backup.domain.BackupClientIdProvider
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.RepositoryClientIdProvider
import dev.gaborbiro.dailymacros.util.showMacroResultsNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSingletonModule {

    @Provides
    @Singleton
    fun gson(): Gson = Gson()

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun settingsAppInfo(appPrefs: AppPrefs): SettingsAppInfo =
        object : SettingsAppInfo {
            override val versionLabel: String
                get() = "${BuildConfig.VERSION_NAME}\nClient ID: ${appPrefs.clientId}"
            override val isDebugBuild: Boolean
                get() = BuildConfig.DEBUG
        }

    @Provides
    @Singleton
    fun clientIdProvider(appPrefs: AppPrefs): RepositoryClientIdProvider =
        object : RepositoryClientIdProvider {
            override val clientId: String
                get() = appPrefs.clientId
        }

    @Provides
    @Singleton
    fun remoteConfigClientIdProvider(appPrefs: AppPrefs): RemoteConfigClientIdProvider =
        object : RemoteConfigClientIdProvider {
            override val clientId: String
                get() = appPrefs.clientId
        }

    @Provides
    @Singleton
    fun backupClientIdProvider(appPrefs: AppPrefs): BackupClientIdProvider =
        object : BackupClientIdProvider {
            override val clientId: String
                get() = appPrefs.clientId
        }

    @Provides
    @Singleton
    fun macroResultsNotificationSender(
        @ApplicationContext context: Context,
        modalNavigator: ModalNavigator,
    ): MacroResultsNotificationSender =
        object : MacroResultsNotificationSender {
            override fun showMacroResultsNotification(
                id: Long,
                recordId: Long,
                title: String?,
                message: String,
                isError: Boolean,
                subscriptionRequired: Boolean,
            ) {
                context.showMacroResultsNotification(id, recordId, title, message, isError, modalNavigator, subscriptionRequired)
            }
        }
}
