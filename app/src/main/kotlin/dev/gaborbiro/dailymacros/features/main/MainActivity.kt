package dev.gaborbiro.dailymacros.features.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.gaborbiro.dailymacros.features.settings.SettingsEffectHandler
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.SettingsRowId
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import dev.gaborbiro.dailymacros.features.onboarding.OnboardingScreen
import dev.gaborbiro.dailymacros.features.common.ONBOARDING_ROUTE
import dev.gaborbiro.dailymacros.features.common.PAYWALL_ROUTE
import dev.gaborbiro.dailymacros.features.overview.OverviewScreen
import dev.gaborbiro.dailymacros.features.paywall.PaywallScreen
import dev.gaborbiro.dailymacros.features.settings.SettingsScreen
import dev.gaborbiro.dailymacros.features.settings.SettingsViewModel
import dev.gaborbiro.dailymacros.features.settings.promptEditor.PromptEditorViewModel
import dev.gaborbiro.dailymacros.features.settings.targetsSettings.TargetsSettingsViewModel
import dev.gaborbiro.dailymacros.features.trends.TrendsScreen
import dev.gaborbiro.dailymacros.features.trends.TrendsViewModel
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import dev.gaborbiro.dailymacros.features.widgets.diarywidget.DiaryWidgetReceiver
import dev.gaborbiro.dailymacros.features.settings.export.useCases.AutoSyncUseCase
import dev.gaborbiro.dailymacros.features.settings.export.rememberOpenPublicDocumentUseCase
import dev.gaborbiro.dailymacros.repositories.records.domain.RequestStatusRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.util.cancelAutoSyncNotifications
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

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // Same instance as the hiltViewModel() in setContent: both are scoped to this Activity.
    private val settingsViewModel: SettingsViewModel by viewModels()

    // Set from a notification's Intent extra; consumed by a LaunchedEffect inside setContent
    // once the NavHost exists, since a PendingIntent can't target a Compose nav route directly.
    private var pendingHighlightRowId by mutableStateOf<SettingsRowId?>(null)

    // Same idea as [pendingHighlightRowId], for the failed-AI-analysis notification, which
    // deep-links straight to the paywall rather than to Settings (see Notifications.kt).
    private var pendingOpenPaywall by mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingHighlightRowId = intent.highlightRowIdExtra()
        pendingOpenPaywall = intent.openPaywallExtra()
    }

    override fun onResume() {
        super.onResume()
        EndOfDayAutoSyncWorker.schedule(applicationContext, settingsRepository)
        lifecycleScope.launch {
            when (val result = autoSyncUseCase.execute()) {
                is AutoSyncUseCase.Result.ConflictDetected ->
                    if (result.shouldNotify) showAutoSyncConflictNotification()
                is AutoSyncUseCase.Result.Failure ->
                    if (result.shouldNotify) showAutoSyncFailureNotification()
                AutoSyncUseCase.Result.Success,
                AutoSyncUseCase.Result.Skipped,
                -> cancelAutoSyncNotifications()
            }
            settingsViewModel.onAutoSyncFinished()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        )
        super.onCreate(savedInstanceState)

        // Set by ProcessRestarter.restartApplication right after a local/cloud
        // restore (see its EXTRA_JUST_RESTORED doc comment - string literal
        // duplicated here since that constant lives in a different module).
        // Reading/writing appPrefs.hasCompletedOnboarding here, on this fresh
        // process's first-ever access to that SharedPreferences file, means
        // it reflects the just-restored file's real on-disk content rather
        // than some stale in-memory snapshot from before the restore.
        if (intent.getBooleanExtra("dev.gaborbiro.dailymacros.EXTRA_JUST_RESTORED", false)) {
            appPrefs.hasCompletedOnboarding = true
        }

        analyticsLogger.setUserId(appPrefs.userUUID)
        lifecycleScope.launch {
            requestStatusRepository.deleteStale()
        }
        pendingHighlightRowId = intent.highlightRowIdExtra()
        pendingOpenPaywall = intent.openPaywallExtra()

        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()
                val targetsSettingsViewModel: TargetsSettingsViewModel = hiltViewModel()
                val promptEditorViewModel: PromptEditorViewModel = hiltViewModel()
                val trendsViewModel: TrendsViewModel = hiltViewModel()

                LaunchedEffect(pendingHighlightRowId) {
                    pendingHighlightRowId?.let { rowId ->
                        navController.navigate("$SETTINGS_ROUTE?$SETTINGS_HIGHLIGHT_ROW_ARG=${rowId.name}") {
                            launchSingleTop = true
                            popUpTo(SETTINGS_ROUTE_PATTERN) { inclusive = true }
                        }
                        pendingHighlightRowId = null
                    }
                }

                LaunchedEffect(pendingOpenPaywall) {
                    if (pendingOpenPaywall) {
                        navController.navigate(PAYWALL_ROUTE)
                        pendingOpenPaywall = false
                    }
                }

                val onAddWidget: () -> Unit = {
                    val mgr = AppWidgetManager.getInstance(this@MainActivity)
                    val provider = ComponentName(this@MainActivity, DiaryWidgetReceiver::class.java)
                    if (mgr.isRequestPinAppWidgetSupported) {
                        mgr.requestPinAppWidget(provider, null, null)
                    } else {
                        Toast.makeText(this@MainActivity, "Pinning widgets is not supported on this launcher", Toast.LENGTH_SHORT).show()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (appPrefs.hasCompletedOnboarding) OVERVIEW_ROUTE else ONBOARDING_ROUTE,
                ) {
                    composable(
                        route = ONBOARDING_ROUTE,
                    ) {
                        val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                        val openPublicDocumentUseCase = rememberOpenPublicDocumentUseCase()
                        OnboardingScreen(
                            navController = navController,
                            onAddWidget = onAddWidget,
                            onRestoreFromCloud = settingsViewModel::onCloudSyncForRestoreTapped,
                            restoreFromCloudInProgress = settingsUiState.cloudSyncInProgress,
                            onRestoreFromLocalBackup = { settingsViewModel.onImportDbTapped(openPublicDocumentUseCase) },
                            restoreFromLocalBackupInProgress = settingsUiState.importDataInProgress,
                        )
                    }
                    composable(
                        route = OVERVIEW_ROUTE,
                    ) {
                        CompositionLocalProvider(LocalImageStore provides imageStore) {
                            OverviewScreen(
                                modalNavigator = modalNavigator,
                                navController = navController,
                                onAddWidget = onAddWidget,
                            )
                        }
                    }
                    composable(
                        route = SETTINGS_ROUTE_PATTERN,
                        arguments = listOf(
                            navArgument(SETTINGS_HIGHLIGHT_ROW_ARG) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
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
                        val highlightRowId = backStackEntry.arguments
                            ?.getString(SETTINGS_HIGHLIGHT_ROW_ARG)
                            ?.let { runCatching { SettingsRowId.valueOf(it) }.getOrNull() }
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            targetsSettingsViewModel = targetsSettingsViewModel,
                            promptEditorViewModel = promptEditorViewModel,
                            navController = navController,
                            highlightRowId = highlightRowId,
                        )
                    }
                    composable(
                        route = PAYWALL_ROUTE,
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
                        PaywallScreen(navController = navController)
                    }
                    composable(
                        route = TRENDS_ROUTE_PATTERN,
                        arguments = listOf(
                            navArgument(TRENDS_SCROLL_EPOCH_DAY_ARG) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument(TRENDS_TIMESCALE_ARG) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
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
                    ) { backStackEntry ->
                        val initialScrollEpochDay = backStackEntry.arguments
                            ?.getString(TRENDS_SCROLL_EPOCH_DAY_ARG)
                            ?.toLongOrNull()
                        val initialTimescale = backStackEntry.arguments
                            ?.getString(TRENDS_TIMESCALE_ARG)
                        TrendsScreen(
                            trendsViewModel = trendsViewModel,
                            targetsSettingsViewModel = targetsSettingsViewModel,
                            navController = navController,
                            initialScrollEpochDay = initialScrollEpochDay,
                            initialTimescale = initialTimescale,
                        )
                    }
                }

                // Rendered after the NavHost so its snackbar overlay draws on top of
                // whichever screen (including onboarding) triggered the feedback.
                SettingsEffectHandler(
                    settingsViewModel = settingsViewModel,
                )
            }
        }

    }

    companion object {
        const val EXTRA_HIGHLIGHT_ROW_ID = "highlight_row_id"
        const val EXTRA_OPEN_PAYWALL = "open_paywall"
    }
}

private fun Intent.highlightRowIdExtra(): SettingsRowId? =
    getStringExtra(MainActivity.EXTRA_HIGHLIGHT_ROW_ID)?.let { name ->
        runCatching { SettingsRowId.valueOf(name) }.getOrNull()
    }

private fun Intent.openPaywallExtra(): Boolean =
    getBooleanExtra(MainActivity.EXTRA_OPEN_PAYWALL, false)
