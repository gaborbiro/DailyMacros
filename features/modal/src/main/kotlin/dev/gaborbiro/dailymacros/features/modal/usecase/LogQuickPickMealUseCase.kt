package dev.gaborbiro.dailymacros.features.modal.usecase

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.core.analytics.AnalyticsLogger
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.shared.UserFacingError
import dev.gaborbiro.dailymacros.features.shared.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Log meal again" on a quick pick, from the widget (confirmation off) or its confirmation
 * dialog: logs the meal and confirms it with a toast. Runs in the application scope, not the
 * screen's, because the screen closes straight away (with the confirmation off it never even
 * draws), and the write must not be cancelled with it.
 */
class LogQuickPickMealUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val logMealFromTemplateUseCase: LogMealFromTemplateUseCase,
    private val analyticsLogger: AnalyticsLogger,
) {

    fun execute(templateId: Long) {
        applicationScope.launch {
            val message = try {
                logMealFromTemplateUseCase.execute(templateId)
                R.string.quick_pick_confirm_logged_toast
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                analyticsLogger.logError(UserFacingError(appContext.getString(R.string.quick_pick_confirm_log_failed_toast), t))
                R.string.quick_pick_confirm_log_failed_toast
            }
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
