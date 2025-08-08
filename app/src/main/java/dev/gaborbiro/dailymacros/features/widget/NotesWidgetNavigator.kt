package dev.gaborbiro.dailymacros.features.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import dev.gaborbiro.dailymacros.data.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.features.modal.ModalActivity
import dev.gaborbiro.dailymacros.store.file.FileStoreFactoryImpl

interface NotesWidgetNavigator {

    fun getLaunchNewNoteViaCameraAction(): Action

    fun getLaunchNewNoteViaImagePickerAction(): Action

    fun getLaunchNewNoteViaTextOnlyAction(): Action

    fun getRecordImageTappedAction(recordId: Long): Action

    fun getRecordBodyTappedAction(recordId: Long): Action

    fun getApplyTemplateAction(templateId: Long): Action

    fun getReloadAction(): Action
}

class NotesWidgetNavigatorImpl : NotesWidgetNavigator {

    override fun getLaunchNewNoteViaCameraAction(): Action {
        return actionRunCallback<AddNoteWithCameraAction>()
    }

    override fun getLaunchNewNoteViaImagePickerAction(): Action {
        return actionRunCallback<AddNoteWithImageAction>()
    }

    override fun getLaunchNewNoteViaTextOnlyAction(): Action {
        return actionRunCallback<AddNoteAction>()
    }

    override fun getRecordImageTappedAction(recordId: Long): Action {
        return actionRunCallback<ImageTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_RECORD) to recordId
            )
        )
    }

    override fun getRecordBodyTappedAction(recordId: Long): Action {
        return actionRunCallback<RecordBodyTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_RECORD) to recordId
            )
        )
    }

    override fun getApplyTemplateAction(templateId: Long): Action {
        return actionRunCallback<ApplyTemplateAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE) to templateId
            )
        )
    }

    override fun getReloadAction(): Action {
        return actionRunCallback<RefreshAction>()
    }
}

class AddNoteWithCameraAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchAddRecordWithCamera(context)
    }
}

class AddNoteWithImageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchAddRecordWithImagePicker(context)
    }
}

class AddNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchAddRecord(context)
    }
}

class ImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        ModalActivity.launchViewImage(context, recordId)
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

class ApplyTemplateAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val fileStore = FileStoreFactoryImpl(context).getStore("public", keepFiles = true)
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        RecordsRepository.get(fileStore).applyTemplate(templateId)
    }
}

class RefreshAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        NotesWidget.reload(context)
    }
}

private const val PREFS_KEY_RECORD = "recordId"
private const val PREFS_KEY_TEMPLATE = "templateId"
