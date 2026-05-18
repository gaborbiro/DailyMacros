package dev.gaborbiro.dailymacros.features.settings.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.scopes.ActivityScoped
import dev.gaborbiro.dailymacros.features.settings.SettingsAppInfo
import dev.gaborbiro.dailymacros.features.settings.SettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportSqliteDatabaseUseCase
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ImportSqliteDatabaseUseCase
import javax.inject.Inject

/**
 * Binds activity-scoped export/import use cases into [SettingsViewModel], which cannot receive them
 * directly via `@HiltViewModel` (ViewModelComponent does not see ActivityComponent bindings).
 */
@ActivityScoped
class SettingsViewModelFactory @Inject constructor(
    private val application: Application,
    private val settingsAppInfo: SettingsAppInfo,
    private val exportFoodDiaryUseCase: ExportFoodDiaryUseCase,
    private val exportSqliteDatabaseUseCase: ExportSqliteDatabaseUseCase,
    private val importSqliteDatabaseUseCase: ImportSqliteDatabaseUseCase,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                application = application,
                appInfo = settingsAppInfo,
                exportFoodDiaryUseCase = exportFoodDiaryUseCase,
                exportSqliteDatabaseUseCase = exportSqliteDatabaseUseCase,
                importSqliteDatabaseUseCase = importSqliteDatabaseUseCase,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
