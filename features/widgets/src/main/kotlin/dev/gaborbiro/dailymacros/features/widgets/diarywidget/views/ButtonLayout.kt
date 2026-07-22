package dev.gaborbiro.dailymacros.features.widgets.diarywidget.views

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.features.widgets.R
import dev.gaborbiro.dailymacros.design.PaddingWidgetDouble
import dev.gaborbiro.dailymacros.features.widgets.util.WidgetPreviewContext

@Composable
fun ButtonLayout(
    modifier: GlanceModifier,
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
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = PaddingWidgetDouble),
            iconResId = R.drawable.ic_add_photo,
            contentDescription = context.getString(R.string.widgets_content_new_record_via_camera_cd),
            tapAction = launchNoteViaCameraAction(),
        )
        WidgetButton(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = PaddingWidgetDouble),
            iconResId = R.drawable.ic_add_picture,
            contentDescription = context.getString(R.string.widgets_content_new_record_via_image_cd),
            tapAction = launchNewNoteViaImagePickerActionProvider(),
        )
        WidgetButton(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = PaddingWidgetDouble),
            iconResId = R.drawable.ic_add_just_text,
            contentDescription = context.getString(R.string.widgets_content_new_record_cd),
            tapAction = launchNewNoteViaTextOnlyActionProvider(),
        )
        WidgetButton(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = PaddingWidgetDouble),
            iconResId = R.drawable.ic_refresh,
            contentDescription = context.getString(R.string.widgets_content_reload_cd),
            tapAction = reloadActionProvider(),
        )
    }
}

@Composable
private fun WidgetButton(
    modifier: GlanceModifier,
    @DrawableRes iconResId: Int,
    contentDescription: String,
    tapAction: Action,
) {
    Image(
        modifier = modifier
            .clickable(tapAction),
        provider = ImageProvider(resId = iconResId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
    )
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
            launchNoteViaCameraAction = { action {} },
            launchNewNoteViaImagePickerActionProvider = { action {} },
            launchNewNoteViaTextOnlyActionProvider = { action {} },
            reloadActionProvider = { action {} },
        )
    }
}
