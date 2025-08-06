package dev.gaborbiro.nutri.features.widget.views

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import dev.gaborbiro.nutri.R

@Composable
fun WidgetButtonLayout(
    modifier: GlanceModifier,
    launchNoteViaCameraAction: () -> Action,
    launchNewNoteViaImagePickerActionProvider: () -> Action,
    launchNewNoteViaTextOnlyActionProvider: () -> Action,
    reloadActionProvider: () -> Action,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetButton(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = 10.dp),
            iconResId = R.drawable.ic_add_photo,
            contentDescription = "New note via camera",
            tapAction = launchNoteViaCameraAction(),
        )
        WidgetButton(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = 10.dp),
            iconResId = R.drawable.ic_add_picture,
            contentDescription = "New note via existing image",
            tapAction = launchNewNoteViaImagePickerActionProvider(),
        )
        WidgetButton(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = 10.dp),
            iconResId = R.drawable.ic_add,
            contentDescription = "New note",
            tapAction = launchNewNoteViaTextOnlyActionProvider(),
        )
        WidgetButton(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(vertical = 10.dp),
            iconResId = R.drawable.ic_refresh,
            contentDescription = "Reload",
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
        provider = ImageProvider(resId = iconResId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
        modifier = modifier.clickable(tapAction)
    )
}
