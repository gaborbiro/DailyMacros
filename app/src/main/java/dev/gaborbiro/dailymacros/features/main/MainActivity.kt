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
import dev.gaborbiro.dailymacros.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.db.AppDatabase
import dev.gaborbiro.dailymacros.data.file.FileStoreFactoryImpl
import dev.gaborbiro.dailymacros.data.image.ImageStoreImpl
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.AppPrefs
import dev.gaborbiro.dailymacros.features.common.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.common.DateUIMapper
import dev.gaborbiro.dailymacros.features.common.MacrosUIMapper
import dev.gaborbiro.dailymacros.features.common.RecordsUIMapper
import dev.gaborbiro.dailymacros.features.common.RepeatRecordUseCase
import dev.gaborbiro.dailymacros.features.common.viewModelFactory
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.trends.TrendsScreen
import dev.gaborbiro.dailymacros.features.trends.TrendsViewModel
import dev.gaborbiro.dailymacros.features.overview.OverviewNavigatorImpl
import dev.gaborbiro.dailymacros.features.overview.OverviewScreen
import dev.gaborbiro.dailymacros.features.overview.OverviewUIMapper
import dev.gaborbiro.dailymacros.features.overview.OverviewViewModel
import dev.gaborbiro.dailymacros.features.settings.SettingsNavigatorImpl
import dev.gaborbiro.dailymacros.features.settings.SettingsScreen
import dev.gaborbiro.dailymacros.features.settings.SettingsViewModel
import dev.gaborbiro.dailymacros.features.trends.TrendsNavigatorImpl
import dev.gaborbiro.dailymacros.repo.records.RecordsApiMapper
import dev.gaborbiro.dailymacros.repo.records.RecordsRepositoryImpl
import dev.gaborbiro.dailymacros.repo.requestStatus.RequestStatusRepositoryImpl
import dev.gaborbiro.dailymacros.repo.settings.SettingsMapper
import dev.gaborbiro.dailymacros.repo.settings.SettingsRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        )
        super.onCreate(savedInstanceState)

        val fileStore = FileStoreFactoryImpl(this).getStore("public", keepFiles = true)
        val imageStore = ImageStoreImpl(fileStore)
        val db = AppDatabase.getInstance()
        val recordsRepository = RecordsRepositoryImpl(
            templatesDAO = db.templatesDAO(),
            recordsDAO = db.recordsDAO(),
            mapper = RecordsApiMapper(),
            imageStore = imageStore,
        )
        val dateUIMapper = DateUIMapper()
        val macrosUIMapper = MacrosUIMapper(dateUIMapper)

        val settingsRepository = SettingsRepository(this@MainActivity, SettingsMapper())
        val appPrefs = AppPrefs(this@MainActivity)
        AnalyticsLogger().setUserId(appPrefs.userUUID)
        lifecycleScope.launch {
            RequestStatusRepositoryImpl(db.requestStatusDAO()).deleteStale()
        }
        val createRecordFromTemplateUseCase = CreateRecordFromTemplateUseCase(recordsRepository)
        val repeatRecordUseCase = RepeatRecordUseCase(
            recordsRepository = recordsRepository,
            createRecordFromTemplateUseCase = createRecordFromTemplateUseCase,
        )

        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()
                val overviewNavigator = remember {
                    OverviewNavigatorImpl(
                        appContext = this@MainActivity,
                        navController = navController,
                    )
                }
                val recordsUIMapper = RecordsUIMapper(macrosUIMapper, dateUIMapper)
                val overviewViewModel = viewModelFactory {
                    OverviewViewModel(
                        navigator = overviewNavigator,
                        recordsRepository = recordsRepository,
                        repeatRecordUseCase = repeatRecordUseCase,
                        recordsUIMapper = recordsUIMapper,
                        overviewUIMapper = OverviewUIMapper(recordsUIMapper, macrosUIMapper),
                        settingsRepository = settingsRepository,
                        appPrefs = appPrefs,
                    )
                }

                val settingsNavigator = remember { SettingsNavigatorImpl(navController) }
                val settingsViewModel = viewModelFactory {
                    SettingsViewModel(
                        navigator = settingsNavigator,
                        repo = settingsRepository,
                        appPrefs = appPrefs
                    )
                }

                val trendsNavigator = remember {
                    TrendsNavigatorImpl(
                        navController = navController,
                    )
                }
                val trendsViewModel = viewModelFactory {
                    TrendsViewModel(
                        navigator = trendsNavigator,
                        recordsRepository = recordsRepository,
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
                            OverviewScreen(overviewViewModel)
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
                        SettingsScreen(settingsViewModel)
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
                        TrendsScreen(trendsViewModel)
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
