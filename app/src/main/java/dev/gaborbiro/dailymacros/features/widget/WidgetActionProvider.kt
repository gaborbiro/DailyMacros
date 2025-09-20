package dev.gaborbiro.dailymacros.features.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import dev.gaborbiro.dailymacros.features.main.MainActivity
import dev.gaborbiro.dailymacros.features.modal.ModalActivity


private const val PREFS_KEY_RECORD = "recordId"
private const val PREFS_KEY_TEMPLATE = "templateId"


internal interface WidgetActionProvider {

    fun createRecordWithCamera(): Action

    fun createRecordWithImagePicker(): Action

    fun createRecord(): Action

    fun recordImageTapped(recordId: Long): Action

    fun recordBodyTapped(recordId: Long): Action

    fun quickPickImageTapped(templateId: Long): Action

    fun quickPickBodyTapped(templateId: Long): Action

    fun reload(): Action

    fun openApp(): Action
}

internal class WidgetActionProviderImpl : WidgetActionProvider {

    override fun createRecordWithCamera(): Action {
        return actionRunCallback<CreateRecordWithCameraAction>()
    }

    override fun createRecordWithImagePicker(): Action {
        return actionRunCallback<CreateRecordWithImagePickerAction>()
    }

    override fun createRecord(): Action {
        return actionRunCallback<CreateRecordAction>()
    }

    override fun recordImageTapped(recordId: Long): Action {
        return actionRunCallback<RecordImageTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_RECORD) to recordId
            )
        )
    }

    override fun recordBodyTapped(recordId: Long): Action {
        return actionRunCallback<RecordBodyTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_RECORD) to recordId
            )
        )
    }

    override fun quickPickImageTapped(templateId: Long): Action {
        return actionRunCallback<QuickPickImageTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE) to templateId
            )
        )
    }

    override fun quickPickBodyTapped(templateId: Long): Action {
        return actionRunCallback<QuickPickBodyTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE) to templateId
            )
        )
    }

    override fun reload(): Action {
        return actionRunCallback<RefreshAction>()
    }

    override fun openApp(): Action {
        return actionStartActivity<MainActivity>()
    }
}

class CreateRecordWithCameraAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchToAddRecordWithCamera(context)
    }
}

class CreateRecordWithImagePickerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchToAddRecordWithImagePicker(context)
    }
}

class CreateRecordAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchToAddRecord(context)
    }
}

class RecordImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        ModalActivity.launchToShowRecordImage(context, recordId)
    }
}

class QuickPickImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        ModalActivity.launchToShowTemplateImage(context, templateId)
    }
}

class RecordBodyTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        ModalActivity.launchToSelectRecordAction(context, recordId)
    }
}

class QuickPickBodyTappedAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        ModalActivity.launchToSelectTemplateAction(context, templateId)
    }
}

class RefreshAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        DailyMacrosWidgetScreen.reload()
    }
}
