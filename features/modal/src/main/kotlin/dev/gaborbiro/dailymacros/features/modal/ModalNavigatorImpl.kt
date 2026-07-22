package dev.gaborbiro.dailymacros.features.modal

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import dev.gaborbiro.dailymacros.features.shared.ModalNavigator
import javax.inject.Inject

class ModalNavigatorImpl @Inject constructor() : ModalNavigator {

    override fun launchToShowRecordImage(context: Context, recordId: Long) =
        context.launchActivity { it.getShowRecordImageIntent(recordId) }

    override fun launchViewRecordDetails(context: Context, recordId: Long) {
        context.launchActivity { it.getViewRecordDetailsIntent(recordId) }
    }

    override fun launchToAddRecordFromPhotoRecognition(
        context: Context,
        imageFilename: String,
        recognisedTitle: String,
    ) {
        context.launchActivityInNewStack {
            it.getPhotoRecognitionDetailsIntent(imageFilename, recognisedTitle)
        }
    }

    override fun photoRecognitionDetailsPendingIntent(
        context: Context,
        requestCode: Int,
        imageFilename: String,
        recognisedTitle: String,
        notificationId: Int,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        context.getPhotoRecognitionDetailsIntent(imageFilename, recognisedTitle).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_DISMISS_NOTIFICATION_ID, notificationId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

fun Context.getShowRecordImageIntent(recordId: Long) =
    getViewImageIntent(EXTRA_RECORD_ID to recordId)

fun Context.getShowTemplateImageIntent(templateId: Long) =
    getViewImageIntent(EXTRA_TEMPLATE_ID to templateId)

fun Context.getViewRecordDetailsIntent(recordId: Long) =
    getModalIntent(Action.VIEW_RECORD_DETAILS, EXTRA_RECORD_ID to recordId)

fun Context.getViewTemplateDetailsIntent(templateId: Long) =
    getModalIntent(Action.VIEW_TEMPLATE_DETAILS, EXTRA_TEMPLATE_ID to templateId)

fun Context.getQuickPickWidgetConfirmIntent(templateId: Long, templateName: String) =
    getModalIntent(Action.QUICK_PICK_WIDGET_CONFIRM, EXTRA_TEMPLATE_ID to templateId, EXTRA_TEMPLATE_NAME to templateName)

fun Context.getPhotoRecognitionDetailsIntent(imageFilename: String, recognisedTitle: String) =
    getModalIntent(Action.PHOTO_RECOGNITION_DETAILS, EXTRA_IMAGE_FILENAME to imageFilename, EXTRA_RECOGNISED_TITLE to recognisedTitle)

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
    PHOTO_RECOGNITION_DETAILS,
}

const val EXTRA_RECORD_ID = "record_id"
const val EXTRA_TEMPLATE_ID = "template_id"
const val EXTRA_TEMPLATE_NAME = "template_name"
const val EXTRA_IMAGE_FILENAME = "image_filename"
const val EXTRA_RECOGNISED_TITLE = "recognised_title"
const val EXTRA_DISMISS_NOTIFICATION_ID = "dismiss_notification_id"
