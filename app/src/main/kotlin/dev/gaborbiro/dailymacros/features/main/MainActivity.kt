package dev.gaborbiro.dailymacros.features.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.BuildConfig
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.common.NutrientsUiMapper
import dev.gaborbiro.dailymacros.features.common.RecordsMapper
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.SharedRecordsUiMapper
import dev.gaborbiro.dailymacros.features.common.util.viewModelFactory
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.overview.OverviewPrefs
import dev.gaborbiro.dailymacros.features.overview.OverviewScreen
import dev.gaborbiro.dailymacros.features.overview.OverviewUiMapper
import dev.gaborbiro.dailymacros.features.overview.OverviewViewModel
import dev.gaborbiro.dailymacros.features.settings.SettingsAppInfo
import dev.gaborbiro.dailymacros.features.settings.SettingsScreen
import dev.gaborbiro.dailymacros.features.settings.SettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.export.CreatePublicDocumentUseCaseImpl
import dev.gaborbiro.dailymacros.features.settings.export.SharePublicUriLauncher
import dev.gaborbiro.dailymacros.features.settings.export.StreamWriter
import dev.gaborbiro.dailymacros.features.settings.export.useCases.ExportFoodDiaryUseCase
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.trends.TrendsPreferencesImpl
import dev.gaborbiro.dailymacros.features.trends.TrendsScreen
import dev.gaborbiro.dailymacros.features.trends.TrendsUiMapper
import dev.gaborbiro.dailymacros.features.trends.TrendsViewModel
import dev.gaborbiro.dailymacros.repositories.records.RecordsApiMapper
import dev.gaborbiro.dailymacros.repositories.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.records.RequestStatusRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.settings.SettingsMapper
import dev.gaborbiro.dailymacros.repositories.settings.SettingsRepositoryImpl
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        )
        super.onCreate(savedInstanceState)

        val analyticsLogger = AnalyticsLogger()

        val fileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = true)
        val imageStore = ImageStoreImpl(fileStore)
        val db = AppDatabase.getInstance()
        val recordsRepository = RecordsRepositoryImpl(
            templatesDAO = db.templatesDAO(),
            recordsDAO = db.recordsDAO(),
            mapper = RecordsApiMapper(),
            imageStore = imageStore,
            analyticsLogger = analyticsLogger,
        )
        val nutrientsUiMapper = NutrientsUiMapper()

        val settingsRepository = SettingsRepositoryImpl(this@MainActivity, SettingsMapper())
        val appPrefs = AppPrefs(this@MainActivity)
        val overviewPrefs = OverviewPrefs(this@MainActivity)
        analyticsLogger.setUserId(appPrefs.userUUID)
        lifecycleScope.launch {
            RequestStatusRepositoryImpl(db.requestStatusDAO()).deleteStale()
        }
        val createRecordFromTemplateUseCase = CreateRecordFromTemplateUseCase(recordsRepository)
        val repeatRecordUseCase = RepeatRecordUseCase(
            recordsRepository = recordsRepository,
            createRecordFromTemplateUseCase = createRecordFromTemplateUseCase,
        )
        val createJsonDocumentUseCase = CreatePublicDocumentUseCaseImpl(this@MainActivity)
        val streamWriter = StreamWriter(this@MainActivity)
        val sharePublicUriLauncher = SharePublicUriLauncher(this@MainActivity)
        val exportFoodDiaryUseCase = ExportFoodDiaryUseCase(
            recordRepository = recordsRepository,
            createPublicDocumentUseCase = createJsonDocumentUseCase,
            streamWriter = streamWriter,
            sharePublicUriLauncher = sharePublicUriLauncher,
        )

        val recordsUiMapper = SharedRecordsUiMapper(nutrientsUiMapper)
        val recordsMapper = RecordsMapper()
        val overviewUiMapper = OverviewUiMapper(recordsUiMapper, nutrientsUiMapper, recordsMapper)

        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()
                val overviewViewModel = viewModelFactory {
                    OverviewViewModel(
                        recordsRepository = recordsRepository,
                        repeatRecordUseCase = repeatRecordUseCase,
                        settingsRepository = settingsRepository,
                        uiMapper = overviewUiMapper,
                        overviewPrefs = overviewPrefs,
                    )
                }

                val settingsAppInfo = remember {
                    object : SettingsAppInfo {
                        override val versionLabel: String
                            get() = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  |  UserID: ${appPrefs.userUUID}"
                    }
                }
                val settingsViewModel = viewModelFactory {
                    SettingsViewModel(
                        appInfo = settingsAppInfo,
                        exportFoodDiaryUseCase = exportFoodDiaryUseCase,
                    )
                }
                val targetsSettingsViewModel = viewModelFactory {
                    TargetsSettingsViewModel(
                        repo = settingsRepository,
                    )
                }

                val trendsPreferences = remember {
                    TrendsPreferencesImpl(this@MainActivity.applicationContext)
                }
                val trendsViewModel = viewModelFactory {
                    TrendsViewModel(
                        recordsRepository = recordsRepository,
                        settingsRepository = settingsRepository,
                        preferences = trendsPreferences,
                        mapper = TrendsUiMapper(trendsPreferences),
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = OVERVIEW_ROUTE,
                ) {
                    composable(
                        route = OVERVIEW_ROUTE,
                    ) {
                        CompositionLocalProvider(LocalImageStore provides imageStore) {
                            OverviewScreen(
                                viewModel = overviewViewModel,
                                navController = navController,
                            )
                        }
                    }
                    composable(
                        route = SETTINGS_ROUTE,
                        enterTransition = {
                            // Slide in from right
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = {
                            // Slide out to right
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                    ) {
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            targetsSettingsViewModel = targetsSettingsViewModel,
                            navController = navController,
                        )
                    }
                    composable(
                        route = TRENDS_ROUTE,
                        enterTransition = {
                            // Slide in from right
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = {
                            // Slide out to right
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                    ) {
                        TrendsScreen(
                            trendsViewModel = trendsViewModel,
                            targetsSettingsViewModel = targetsSettingsViewModel,
                            navController = navController,
                        )
                    }
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    /* activity = */ this,
                    /* permissions = */ arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    /* requestCode = */ 123
                )
            }
        }
    }
}
