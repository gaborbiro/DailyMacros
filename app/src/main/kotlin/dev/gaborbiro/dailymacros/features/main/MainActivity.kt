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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.overview.OverviewScreen
import dev.gaborbiro.dailymacros.features.overview.OverviewViewModel
import dev.gaborbiro.dailymacros.features.settings.SettingsScreen
import dev.gaborbiro.dailymacros.features.settings.SettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.di.SettingsViewModelFactory
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.trends.TrendsScreen
import dev.gaborbiro.dailymacros.features.trends.TrendsViewModel
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageStore: ImageStore

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    @Inject
    lateinit var appPrefs: AppPrefs

    @Inject
    lateinit var requestStatusRepository: RequestStatusRepository

    @Inject
    lateinit var settingsViewModelFactory: SettingsViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        )
        super.onCreate(savedInstanceState)

        analyticsLogger.setUserId(appPrefs.userUUID)
        lifecycleScope.launch {
            requestStatusRepository.deleteStale()
        }

        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()
                val overviewViewModel: OverviewViewModel = hiltViewModel()
                val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
                val targetsSettingsViewModel: TargetsSettingsViewModel = hiltViewModel()
                val trendsViewModel: TrendsViewModel = hiltViewModel()

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
                        // Slide the Settings screen in from the right (and out to the right on back).
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = {
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
                        // Same horizontal slide as Settings: Trends enters from the right and exits to the right.
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = {
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
