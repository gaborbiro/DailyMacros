package dev.gaborbiro.dailymacros

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import dagger.hilt.android.EntryPointAccessors
import dev.gaborbiro.dailymacros.features.main.MainActivity
import dev.gaborbiro.dailymacros.features.modal.ModalNavigator
import dev.gaborbiro.dailymacros.features.widget.WidgetGlanceEntryPoint
import dev.gaborbiro.dailymacros.features.widget.WidgetNavigator
import javax.inject.Inject

private const val PREFS_KEY_RECORD = "recordId"
private const val PREFS_KEY_TEMPLATE = "templateId"

class WidgetNavigatorImpl @Inject constructor(
    private val modalNavigator: ModalNavigator,
) : WidgetNavigator {

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

    private inner class CreateRecordWithCameraAction : ActionCallback {

        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            modalNavigator.launchToAddRecordWithCamera(context)
        }
    }

    private inner class CreateRecordWithImagePickerAction : ActionCallback {

        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            modalNavigator.launchToAddRecordWithImagePicker(context)
        }
    }

    private inner class CreateRecordAction : ActionCallback {

        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            modalNavigator.launchToAddRecord(context)
        }
    }

    private inner class RecordImageTappedAction : ActionCallback {

        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
            modalNavigator.launchToShowRecordImageNoApp(context, recordId)
        }
    }

    private inner class QuickPickImageTappedAction : ActionCallback {

        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
            modalNavigator.launchToShowTemplateImage(context, templateId)
        }
    }

    private inner class RecordBodyTappedAction : ActionCallback {

        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
            modalNavigator.launchToSelectRecordAction(context, recordId)
        }
    }

    private inner class QuickPickBodyTappedAction : ActionCallback {

        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
            modalNavigator.launchToSelectTemplateAction(context, templateId)
        }
    }
}

class RefreshAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetGlanceEntryPoint::class.java,
        ).foodDiaryWidgetReloader().scheduleReload(context.applicationContext)
    }
}
