package dev.gaborbiro.dailymacros.features.widget.views

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultHorizontal
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview
import dev.gaborbiro.dailymacros.util.randomBitmap

@Composable
internal fun TemplateListItem(
    template: TemplateUIModel,
    imageTapActionProvider: Action,
    bodyTapActionProvider: Action,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(bodyTapActionProvider)
            .padding(start = PaddingWidgetDefaultHorizontal),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        template.bitmap
            ?.let { image: Bitmap ->
                Image(
                    modifier = GlanceModifier
                        .size(WidgetTemplateImageSize)
                        .clickable(imageTapActionProvider)
                        .cornerRadius(6.dp),
                    provider = ImageProvider(image),
                    contentDescription = "note image",
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetTemplateImageSize))
            }
        Text(
            modifier = GlanceModifier
                .defaultWeight()
                .wrapContentHeight()
                .padding(horizontal = PaddingWidgetDefaultHorizontal),
            text = template.title,
            maxLines = 2,
            style = titleTextStyle,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun TemplateListItemPreview() {
    WidgetPreview {
        TemplateListItem(
            template = TemplateUIModel(
                title = "Breakfast",
                bitmap = randomBitmap(),
                templateId = 0L,
            ),
            imageTapActionProvider = action { },
            bodyTapActionProvider = action { },
        )
    }
}
