package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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
        template.images.firstOrNull()
            ?.let {
                LocalImage(
                    it,
                    modifier = GlanceModifier
                        .size(WidgetImageSize)
                        .clickable(imageTapActionProvider)
                        .cornerRadius(6.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetTemplateImageSize))
            }

        Column {
            Text(
                modifier = GlanceModifier
                    .defaultWeight()
                    .wrapContentHeight()
                    .padding(horizontal = PaddingWidgetDefaultHorizontal),
                text = template.title,
                maxLines = 1,
                style = titleTextStyle,
            )
            template.description?.let {
                Text(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .wrapContentHeight()
                        .padding(horizontal = PaddingWidgetDefaultHorizontal),
                    text = template.description,
                    maxLines = 2,
                    style = descriptionTextStyle,
                )
            }
        }
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
                description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                images = emptyList(),
                templateId = 0L,
            ),
            imageTapActionProvider = action { },
            bodyTapActionProvider = action { },
        )
    }
}
