package dev.gaborbiro.dailymacros.features.widgets.diarywidget.views

import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.features.widgets.R
import dev.gaborbiro.dailymacros.design.PaddingWidgetDouble
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext

@Composable
fun ButtonLayout(
    modifier: GlanceModifier,
    highlightRecordButtons: Boolean,
    launchNoteViaCameraAction: @Composable () -> Action,
    launchNewNoteViaImagePickerActionProvider: @Composable () -> Action,
    launchNewNoteViaTextOnlyActionProvider: @Composable () -> Action,
    reloadActionProvider: @Composable () -> Action,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetButton(
            modifier = GlanceModifier.defaultWeight(),
            iconResId = R.drawable.ic_add_photo,
            contentDescription = context.getString(R.string.widgets_content_new_record_via_camera_cd),
            tapAction = launchNoteViaCameraAction(),
            pulseLayoutResId = if (highlightRecordButtons) R.layout.widget_record_button_pulse_camera else null,
        )
        WidgetButton(
            modifier = GlanceModifier.defaultWeight(),
            iconResId = R.drawable.ic_add_picture,
            contentDescription = context.getString(R.string.widgets_content_new_record_via_image_cd),
            tapAction = launchNewNoteViaImagePickerActionProvider(),
            pulseLayoutResId = if (highlightRecordButtons) R.layout.widget_record_button_pulse_image else null,
        )
        WidgetButton(
            modifier = GlanceModifier.defaultWeight(),
            iconResId = R.drawable.ic_add_just_text,
            contentDescription = context.getString(R.string.widgets_content_new_record_cd),
            tapAction = launchNewNoteViaTextOnlyActionProvider(),
            pulseLayoutResId = if (highlightRecordButtons) R.layout.widget_record_button_pulse_text else null,
        )
        WidgetButton(
            modifier = GlanceModifier.defaultWeight(),
            iconResId = R.drawable.ic_refresh,
            contentDescription = context.getString(R.string.widgets_content_reload_cd),
            tapAction = reloadActionProvider(),
            pulseLayoutResId = null,
        )
    }
}

/**
 * @param pulseLayoutResId a widget_record_button_pulse_* layout to play behind the icon, or null for none
 */
@Composable
private fun WidgetButton(
    modifier: GlanceModifier,
    @DrawableRes iconResId: Int,
    contentDescription: String,
    tapAction: Action,
    @LayoutRes pulseLayoutResId: Int?,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (pulseLayoutResId != null) {
            // Fixed size: a fill-parent ProgressBar would stretch the whole row to the widget's height.
            AndroidRemoteViews(
                modifier = GlanceModifier.size(48.dp),
                remoteViews = RemoteViews(context.packageName, pulseLayoutResId),
            )
        }
        Image(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = PaddingWidgetDouble)
                .clickable(tapAction),
            provider = ImageProvider(resId = iconResId),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
        )
    }
}

@Preview(widthDp = 200, heightDp = 50)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun ButtonLayoutPreview() {
    WidgetPreviewContext {
        ButtonLayout(
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight(),
            highlightRecordButtons = true,
            launchNoteViaCameraAction = { action {} },
            launchNewNoteViaImagePickerActionProvider = { action {} },
            launchNewNoteViaTextOnlyActionProvider = { action {} },
            reloadActionProvider = { action {} },
        )
    }
}
