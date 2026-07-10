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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.SETTINGS_TRIGGER_CLOUD_SYNC_ARG
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import dev.gaborbiro.dailymacros.features.overview.OverviewScreen
import dev.gaborbiro.dailymacros.features.overview.OverviewViewModel
import dev.gaborbiro.dailymacros.features.settings.SettingsScreen
import dev.gaborbiro.dailymacros.features.settings.SettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.trends.TrendsScreen
import dev.gaborbiro.dailymacros.features.trends.TrendsViewModel
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import dev.gaborbiro.dailymacros.features.widgets.diary.DiaryWidgetReceiver
import dev.gaborbiro.dailymacros.features.settings.export.useCases.AutoSyncUseCase
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.util.showAutoSyncConflictNotification
import dev.gaborbiro.dailymacros.util.showAutoSyncFailureNotification
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
    lateinit var modalNavigator: ModalNavigator

    @Inject
    lateinit var autoSyncUseCase: AutoSyncUseCase

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            when (autoSyncUseCase.execute()) {
                AutoSyncUseCase.Result.ConflictDetected -> showAutoSyncConflictNotification()
                is AutoSyncUseCase.Result.Failure -> showAutoSyncFailureNotification()
                else -> Unit
            }
        }
    }

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
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val targetsSettingsViewModel: TargetsSettingsViewModel = hiltViewModel()
                val promptEditorViewModel: PromptEditorViewModel = hiltViewModel()
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
                                modalNavigator = modalNavigator,
                                navController = navController,
                                onAddWidget = {
                                    val mgr = AppWidgetManager.getInstance(this@MainActivity)
                                    val provider = ComponentName(this@MainActivity, DiaryWidgetReceiver::class.java)
                                    if (mgr.isRequestPinAppWidgetSupported) {
                                        mgr.requestPinAppWidget(provider, null, null)
                                    } else {
                                        Toast.makeText(this@MainActivity, "Pinning widgets is not supported on this launcher", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                    }
                    composable(
                        route = "$SETTINGS_ROUTE?$SETTINGS_HIGHLIGHT_TARGETS_ARG={$SETTINGS_HIGHLIGHT_TARGETS_ARG}&$SETTINGS_TRIGGER_CLOUD_SYNC_ARG={$SETTINGS_TRIGGER_CLOUD_SYNC_ARG}",
                        arguments = listOf(
                            navArgument(SETTINGS_HIGHLIGHT_TARGETS_ARG) {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                            navArgument(SETTINGS_TRIGGER_CLOUD_SYNC_ARG) {
                                type = NavType.BoolType
                                defaultValue = false
                            },
                        ),
                        enterTransition = {
                            scaleIn(
                                initialScale = 0.85f,
                                transformOrigin = TransformOrigin(1f, 0f),
                                animationSpec = tween(350, easing = FastOutSlowInEasing),
                            ) + fadeIn(animationSpec = tween(350))
                        },
                        exitTransition = {
                            scaleOut(
                                targetScale = 0.85f,
                                transformOrigin = TransformOrigin(1f, 0f),
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                            ) + fadeOut(animationSpec = tween(200))
                        },
                    ) { backStackEntry ->
                        val highlightTargets = backStackEntry.arguments?.getBoolean(SETTINGS_HIGHLIGHT_TARGETS_ARG) ?: false
                        val triggerCloudSync = backStackEntry.arguments?.getBoolean(SETTINGS_TRIGGER_CLOUD_SYNC_ARG) ?: false
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            targetsSettingsViewModel = targetsSettingsViewModel,
                            promptEditorViewModel = promptEditorViewModel,
                            navController = navController,
                            highlightTargets = highlightTargets,
                            triggerCloudSync = triggerCloudSync,
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

    }
}
