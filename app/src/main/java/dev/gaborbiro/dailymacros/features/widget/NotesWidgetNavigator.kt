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

interface NotesWidgetNavigator {

    fun getLaunchNewNoteViaCameraAction(): Action

    fun getLaunchNewNoteViaImagePickerAction(): Action

    fun getLaunchNewNoteViaTextOnlyAction(): Action

    fun getRecordImageTappedAction(recordId: Long): Action

    fun getRecordBodyTappedAction(recordId: Long): Action

    fun getTemplateImageTappedAction(templateId: Long): Action

    fun getTemplateBodyTappedAction(templateId: Long): Action

    fun getReloadAction(): Action

    fun getOpenAppAction(): Action
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
        return actionRunCallback<RecordImageTappedAction>(
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

    override fun getTemplateImageTappedAction(templateId: Long): Action {
        return actionRunCallback<TemplateImageTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE) to templateId
            )
        )
    }

    override fun getTemplateBodyTappedAction(templateId: Long): Action {
        return actionRunCallback<TemplateBodyTappedAction>(
            actionParametersOf(
                ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE) to templateId
            )
        )
    }

    override fun getReloadAction(): Action {
        return actionRunCallback<RefreshAction>()
    }

    override fun getOpenAppAction(): Action {
        return actionStartActivity<MainActivity>()
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

class RecordImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        ModalActivity.launchViewRecordImage(context, recordId)
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

//class ApplyTemplateAction : ActionCallback {
//
//    override suspend fun onAction(
//        context: Context,
//        glanceId: GlanceId,
//        parameters: ActionParameters,
//    ) {
//        val fileStore = FileStoreFactoryImpl(context).getStore("public", keepFiles = true)
//        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
//        val recordsRepository = RecordsRepositoryImpl(
//            templatesDAO = AppDatabase.getInstance().templatesDAO(),
//            recordsDAO = AppDatabase.getInstance().recordsDAO(),
//            dBMapper = DBMapper(),
//            bitmapStore = BitmapStore(fileStore),
//        )
//        recordsRepository.applyTemplate(templateId)
//    }
//}

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
        NotesWidget.reload(context)
    }
}

private const val PREFS_KEY_RECORD = "recordId"
private const val PREFS_KEY_TEMPLATE = "templateId"
