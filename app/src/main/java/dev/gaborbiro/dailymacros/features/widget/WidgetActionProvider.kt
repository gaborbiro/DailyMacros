package dev.gaborbiro.dailymacros.features.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import dev.gaborbiro.dailymacros.features.common.AppPrefs
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

    fun templateImageTapped(templateId: Long): Action

    fun templateBodyTapped(templateId: Long): Action

    fun reload(): Action

    fun openApp(): Action

    fun dismissQuickAddTooltip(): Action
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

    override fun templateImageTapped(templateId: Long): Action {
        return actionRunCallback<TemplateImageTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE) to templateId
            )
        )
    }

    override fun templateBodyTapped(templateId: Long): Action {
        return actionRunCallback<TemplateBodyTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE) to templateId
            )
        )
    }

    override fun dismissQuickAddTooltip(): Action {
        return actionRunCallback<DismissQuickAddTooltipAction>()
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
        ModalActivity.launchAddRecordWithCamera(context)
    }
}

class CreateRecordWithImagePickerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchAddRecordWithImagePicker(context)
    }
}

class CreateRecordAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchAddRecord(context)
    }
}

class RecordImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        ModalActivity.launchRecordImageViewRequest(context, recordId)
    }
}

class TemplateImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        ModalActivity.launchViewTemplateImage(context, templateId)
    }
}

class RecordBodyTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        ModalActivity.launchSelectRecordAction(context, recordId)
    }
}

class TemplateBodyTappedAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        ModalActivity.launchSelectTemplateAction(context, templateId)
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

class DismissQuickAddTooltipAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        println("DismissQuickAddTooltipAction")
        AppPrefs(context).showTooltipQuickAdd = false
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[DailyMacrosWidgetScreen.Companion.PREFS_SHOW_QUICK_ADD_TOOLTIP] = false
            prefs
        }
        DailyMacrosWidgetScreen().update(context, glanceId) // trigger recompose
    }
}
