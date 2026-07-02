package dev.gaborbiro.dailymacros.features.modal

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import javax.inject.Inject

class ModalNavigatorImpl @Inject constructor() : ModalNavigator {

    override fun launchToAddRecordWithCamera(context: Context) =
        context.launchActivityInNewStack(Context::getCameraIntent)

    override fun launchToAddRecordWithImagePicker(context: Context) =
        context.launchActivityInNewStack(Context::getImagePickerIntent)

    override fun launchToShowRecordImage(context: Context, recordId: Long) =
        context.launchActivity { it.getShowRecordImageIntent(recordId) }

    override fun launchToShowRecordImageNoApp(context: Context, recordId: Long) =
        context.launchActivityInNewStack { it.getShowRecordImageIntent(recordId) }

    override fun launchToShowTemplateImage(context: Context, templateId: Long) =
        context.launchActivityInNewStack { it.getShowTemplateImageIntent(templateId) }

    override fun launchToAddRecord(context: Context) =
        context.launchActivityInNewStack(Context::getTextOnlyIntent)

    override fun launchViewRecordDetails(context: Context, recordId: Long) {
        context.launchActivity { it.getViewRecordDetailsIntent(recordId) }
    }

    override fun launchViewRecordDetailsFromWidget(context: Context, recordId: Long) {
        context.launchActivityInNewStack { it.getViewRecordDetailsIntent(recordId) }
    }

    override fun launchViewTemplateDetailsFromWidget(context: Context, templateId: Long) {
        context.launchActivityInNewStack { it.getViewTemplateDetailsIntent(templateId) }
    }

    override fun launchQuickPickWidgetConfirmDialog(context: Context, templateId: Long) {
        context.launchActivityInNewStack { it.getQuickPickWidgetConfirmIntent(templateId) }
    }
}

fun Context.getShowRecordImageIntent(recordId: Long) =
    getViewImageIntent(EXTRA_RECORD_ID to recordId)

fun Context.getShowTemplateImageIntent(templateId: Long) =
    getViewImageIntent(EXTRA_TEMPLATE_ID to templateId)

fun Context.getViewRecordDetailsIntent(recordId: Long) =
    getModalIntent(Action.VIEW_RECORD_DETAILS, EXTRA_RECORD_ID to recordId)

fun Context.getViewTemplateDetailsIntent(templateId: Long) =
    getModalIntent(Action.VIEW_TEMPLATE_DETAILS, EXTRA_TEMPLATE_ID to templateId)

fun Context.getQuickPickWidgetConfirmIntent(templateId: Long) =
    getModalIntent(Action.QUICK_PICK_WIDGET_CONFIRM, EXTRA_TEMPLATE_ID to templateId)

private fun Context.getViewImageIntent(vararg extras: Pair<String, Any>) =
    getModalIntent(Action.VIEW_IMAGE, *extras)

fun Context.getCameraIntent() = getModalIntent(Action.CAMERA)

fun Context.getImagePickerIntent() = getModalIntent(Action.BROWSE_IMAGES)

fun Context.getTextOnlyIntent() = getModalIntent(Action.TEXT_ONLY)

private fun Context.getModalIntent(
    action: Action,
    vararg extras: Pair<String, Any>,
) = Intent(this, ModalActivity::class.java).also {
    it.putExtra(EXTRA_ACTION, action.name)
    it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    if (extras.isNotEmpty()) {
        it.putExtras(bundleOf(*extras))
    }
}

fun Context.launchActivityInNewStack(constructIntent: (Context) -> Intent) {
    launchActivity {
        constructIntent(this).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
}

fun Context.launchActivity(constructIntent: (Context) -> Intent) {
    startActivity(
        constructIntent(this)
    )
}


const val EXTRA_ACTION = "extra_action"

enum class Action {
    CAMERA,
    BROWSE_IMAGES,
    TEXT_ONLY,
    VIEW_RECORD_DETAILS,
    VIEW_TEMPLATE_DETAILS,
    VIEW_IMAGE,
    QUICK_PICK_WIDGET_CONFIRM,
}

const val EXTRA_RECORD_ID = "record_id"
const val EXTRA_TEMPLATE_ID = "template_id"
