package dev.gaborbiro.dailymacros

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.gaborbiro.dailymacros.features.main.MainActivity
import dev.gaborbiro.dailymacros.features.modal.getCameraIntent
import dev.gaborbiro.dailymacros.features.modal.getImagePickerIntent
import dev.gaborbiro.dailymacros.features.modal.getQuickPickWidgetConfirmIntent
import dev.gaborbiro.dailymacros.features.modal.getShowRecordImageIntent
import dev.gaborbiro.dailymacros.features.modal.getShowTemplateImageIntent
import dev.gaborbiro.dailymacros.features.modal.getTextOnlyIntent
import dev.gaborbiro.dailymacros.features.modal.getViewRecordDetailsIntent
import dev.gaborbiro.dailymacros.features.modal.getViewTemplateDetailsIntent
import dev.gaborbiro.dailymacros.features.widgets.WidgetNavigator
import dev.gaborbiro.dailymacros.features.widgets.diarywidget.DiaryWidgetScreen
import javax.inject.Inject

class WidgetNavigatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetNavigator {

    override fun createRecordWithCamera(): Action =
        launchInNewStack(context.getCameraIntent())

    override fun createRecordWithImagePicker(): Action =
        launchInNewStack(context.getImagePickerIntent())

    override fun createRecord(): Action =
        launchInNewStack(context.getTextOnlyIntent())

    override fun recordImageTapped(recordId: Long): Action =
        launchInNewStack(context.getShowRecordImageIntent(recordId))

    override fun recordBodyTapped(recordId: Long): Action =
        launchInNewStack(context.getViewRecordDetailsIntent(recordId))

    override fun quickPickImageTapped(templateId: Long): Action =
        launchInNewStack(context.getShowTemplateImageIntent(templateId))

    override fun quickPickBodyTapped(templateId: Long): Action =
        launchInNewStack(context.getViewTemplateDetailsIntent(templateId))

    override fun quickPickWidgetTapped(templateId: Long, templateName: String): Action =
        launchInNewStack(context.getQuickPickWidgetConfirmIntent(templateId, templateName))

    override fun reload(): Action {
        return actionRunCallback<RefreshAction>()
    }

    override fun openApp(): Action {
        return actionStartActivity<MainActivity>()
    }

    /**
     * Builds an activity launch [Action] that the widget host (launcher) fires directly, so the
     * activity starts with the host's foreground privileges. This avoids the Background Activity
     * Launch restrictions that block starting an activity from within an [ActionCallback], which
     * runs in the background (see BAL grace-period aborts in logcat).
     */
    private fun launchInNewStack(intent: Intent): Action =
        actionStartActivityIntent(
            intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
}

class RefreshAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        DiaryWidgetScreen.reload(context.applicationContext)
    }
}
