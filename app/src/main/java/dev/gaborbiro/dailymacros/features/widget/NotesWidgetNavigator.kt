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
import dev.gaborbiro.dailymacros.util.showSimpleNotification

interface NotesWidgetNavigator {

    fun getLaunchNewNoteViaCameraAction(): Action

    fun getLaunchNewNoteViaImagePickerAction(): Action

    fun getLaunchNewNoteViaTextOnlyAction(): Action

    fun getDuplicateRecordAction(recordId: Long): Action

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

    override fun getDuplicateRecordAction(recordId: Long): Action {
        return actionRunCallback<DuplicateNoteAction>(
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
        ModalActivity.launchCreateNoteWithCamera(context)
    }
}

class AddNoteWithImageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchAddNoteWithImage(context)
    }
}

class AddNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ModalActivity.launchAddNote(context)
    }
}

class DuplicateNoteAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val fileStore = FileStoreFactoryImpl(context).getStore("public", keepFiles = true)
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        val repo = RecordsRepository.get(fileStore)
        val oldRecord = repo.getRecord(recordId)
        val newRecordId = repo.duplicateRecord(recordId)
        val newRecord = repo.getRecord(newRecordId)
        context.showSimpleNotification(
            recordId, newRecord
            ?.let { "Created note with '${newRecord.template.name}'" }
            ?: run { "Could not create note with '${oldRecord?.template?.name ?: "?"}'" }
        )
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
