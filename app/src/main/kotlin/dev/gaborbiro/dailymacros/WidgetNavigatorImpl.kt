package dev.gaborbiro.dailymacros

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.features.main.MainActivity
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import dev.gaborbiro.dailymacros.features.widgets.diary.DiaryWidgetScreen
import dev.gaborbiro.dailymacros.features.widgets.WidgetNavigator
import javax.inject.Inject

private const val PREFS_KEY_RECORD = "recordId"
private const val PREFS_KEY_TEMPLATE = "templateId"

class WidgetNavigatorImpl @Inject constructor() : WidgetNavigator {

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

    override fun quickPickWidgetTapped(templateId: Long): Action {
        return actionRunCallback<QuickPickWidgetTappedAction>(
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

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface WidgetActionsEntryPoint {
    fun modalNavigator(): ModalNavigator
}

private fun Context.modalNavigator(): ModalNavigator =
    EntryPointAccessors.fromApplication(
        applicationContext,
        WidgetActionsEntryPoint::class.java,
    ).modalNavigator()

class CreateRecordWithCameraAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.modalNavigator().launchToAddRecordWithCamera(context)
    }
}

class CreateRecordWithImagePickerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.modalNavigator().launchToAddRecordWithImagePicker(context)
    }
}

class CreateRecordAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.modalNavigator().launchToAddRecord(context)
    }
}

class RecordImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        context.modalNavigator().launchToShowRecordImageNoApp(context, recordId)
    }
}

class QuickPickImageTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        context.modalNavigator().launchToShowTemplateImage(context, templateId)
    }
}

class RecordBodyTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val recordId = parameters[ActionParameters.Key<Long>(PREFS_KEY_RECORD)]!!
        context.modalNavigator().launchViewRecordDetailsFromWidget(context, recordId)
    }
}

class QuickPickBodyTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        context.modalNavigator().launchViewTemplateDetailsFromWidget(context, templateId)
    }
}

class QuickPickWidgetTappedAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val templateId = parameters[ActionParameters.Key<Long>(PREFS_KEY_TEMPLATE)]!!
        val appContext = context.applicationContext
        appContext.modalNavigator().launchQuickPickWidgetConfirmDialog(appContext, templateId)
    }
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
