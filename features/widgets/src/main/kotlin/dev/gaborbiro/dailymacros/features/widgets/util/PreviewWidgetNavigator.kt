package dev.gaborbiro.dailymacros.features.widgets.util

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import dev.gaborbiro.dailymacros.features.widgets.WidgetNavigator

/** Glance previews only; does not launch activities. */
internal object PreviewWidgetNavigator : WidgetNavigator {

    override fun createRecordWithCamera(): Action = noopAction()

    override fun createRecordWithImagePicker(): Action = noopAction()

    override fun createRecord(): Action = noopAction()

    override fun recordImageTapped(recordId: Long): Action = noopAction()

    override fun recordBodyTapped(recordId: Long): Action = noopAction()

    override fun quickPickImageTapped(templateId: Long): Action = noopAction()

    override fun quickPickBodyTapped(templateId: Long): Action = noopAction()

    override fun reload(): Action = noopAction()

    override fun openApp(): Action = noopAction()

    private fun noopAction(): Action = actionRunCallback<PreviewNoOpAction>()
}

private class PreviewNoOpAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) = Unit
}
