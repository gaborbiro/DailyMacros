package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
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
import dev.gaborbiro.dailymacros.features.common.model.TemplateUIModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
internal fun ListItemTemplate(
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
        template.images.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier
                        .size(WidgetTemplateImageSize)
                        .clickable(imageTapActionProvider)
                        .cornerRadius(6.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetTemplateImageSize))
            }

        Text(
            modifier = GlanceModifier
                .wrapContentHeight()
                .padding(horizontal = PaddingWidgetDefaultHorizontal),
            text = template.title,
            maxLines = 3,
            style = titleTextStyle,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun TemplateListItemPreview() {
    WidgetPreview {
        ListItemTemplate(
            template = TemplateUIModel(
                title = "Breakfast",
                description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                images = listOf("", ""),
                templateId = 0L,
            ),
            imageTapActionProvider = action { },
            bodyTapActionProvider = action { },
        )
    }
}
