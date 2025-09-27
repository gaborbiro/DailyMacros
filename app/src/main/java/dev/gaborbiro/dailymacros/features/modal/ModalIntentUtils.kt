package dev.gaborbiro.dailymacros.features.modal

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf

fun Context.getShowRecordImageIntent(recordId: Long) =
    getViewImageIntent(EXTRA_RECORD_ID to recordId)

fun Context.getShowTemplateImageIntent(templateId: Long) =
    getViewImageIntent(EXTRA_TEMPLATE_ID to templateId)

fun Context.getViewRecordDetailsIntent(recordId: Long) =
    getModalIntent(Action.VIEW_RECORD_DETAILS, EXTRA_RECORD_ID to recordId)

fun Context.getSelectRecordActionIntent(recordId: Long) =
    getModalIntent(Action.SELECT_RECORD_ACTION, EXTRA_RECORD_ID to recordId)

fun Context.getSelectTemplateActionIntent(templateId: Long) =
    getModalIntent(Action.SELECT_TEMPLATE_ACTION, EXTRA_TEMPLATE_ID to templateId)

private fun Context.getViewImageIntent(vararg extras: Pair<String, Any>) =
    getModalIntent(Action.VIEW_IMAGE, *extras)

fun Context.getCameraIntent() = getModalIntent(Action.CAMERA)

fun Context.getImagePickerIntent() = getModalIntent(Action.PICK_PHOTO)

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
    startActivity(constructIntent(this).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    })
}

fun Context.launchActivity(constructIntent: (Context) -> Intent) {
    startActivity(constructIntent(this))
}


const val EXTRA_ACTION = "extra_action"

enum class Action {
    CAMERA, QUICK_CAMERA, PICK_PHOTO, TEXT_ONLY, VIEW_RECORD_DETAILS, VIEW_IMAGE, SELECT_RECORD_ACTION, SELECT_TEMPLATE_ACTION
}

const val EXTRA_RECORD_ID = "record_id"
const val EXTRA_TEMPLATE_ID = "template_id"
