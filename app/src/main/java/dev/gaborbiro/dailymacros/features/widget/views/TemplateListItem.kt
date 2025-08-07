package dev.gaborbiro.dailymacros.features.widget.views

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.features.common.model.TemplateUIModel
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.design.PaddingWidgetHalf

@Composable
fun WidgetTemplateListItem(
    template: TemplateUIModel,
    tapActionProvider: Action,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = PaddingWidgetHalf, horizontal = PaddingWidgetDefault)
            .clickable(tapActionProvider)
    ) {
        template.bitmap
            ?.let { image: Bitmap ->
                Image(
                    provider = ImageProvider(image),
                    contentDescription = "note image",
                    modifier = GlanceModifier
                        .size(WidgetTemplateImageSize),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetTemplateImageSize))
            }
        Text(
            text = template.title,
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(start = PaddingWidgetDefault),
            maxLines = 2,
            style = titleTextStyle
        )
    }
}
