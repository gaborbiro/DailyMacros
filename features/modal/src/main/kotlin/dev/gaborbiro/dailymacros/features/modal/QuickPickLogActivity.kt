package dev.gaborbiro.dailymacros.features.modal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.features.modal.usecase.LogMealFromTemplateUseCase
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Invisible (Theme.NoDisplay) target of a quick pick widget tap. With the "Log meal again?"
 * confirmation turned off it logs the meal and finishes before anything is drawn, so the tap
 * doesn't flash ModalActivity's dimmed window. With it on, it hands over to ModalActivity's
 * confirmation dialog, as the widget did before.
 *
 * The widget can't decide this itself: its tap action is fixed when the widget is drawn, and an
 * Activity can't be started from a background widget callback. Launched by the widget host, this
 * Activity is in the foreground, so it may start ModalActivity.
 */
@AndroidEntryPoint
class QuickPickLogActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var logMealFromTemplateUseCase: LogMealFromTemplateUseCase

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val templateId = intent.getLongExtra(EXTRA_TEMPLATE_ID, -1L)
        val templateName = intent.getStringExtra(EXTRA_TEMPLATE_NAME) ?: ""
        if (templateId != -1L) {
            if (settingsRepository.getQuickPickConfirmationEnabled()) {
                startActivity(
                    getQuickPickWidgetConfirmIntent(templateId, templateName).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
            } else {
                logInBackground(templateId)
            }
        }
        // Theme.NoDisplay requires finishing before onResume.
        finish()
    }

    /** Outlives this Activity, which finishes immediately; the write takes milliseconds. */
    private fun logInBackground(templateId: Long) {
        val appContext = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            val message = try {
                logMealFromTemplateUseCase.execute(templateId)
                R.string.quick_pick_confirm_logged_toast
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                analyticsLogger.logError(t)
                R.string.quick_pick_confirm_log_failed_toast
            }
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context.getQuickPickWidgetTapIntent(templateId: Long, templateName: String) =
    Intent(this, QuickPickLogActivity::class.java)
        .putExtra(EXTRA_TEMPLATE_ID, templateId)
        .putExtra(EXTRA_TEMPLATE_NAME, templateName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
