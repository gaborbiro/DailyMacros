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
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultHorizontal
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview
import dev.gaborbiro.dailymacros.util.randomBitmap

@Composable
fun RecordListItem(
    record: RecordUIModel,
    imageTappedActionProvider: Action,
    bodyTappedActionProvider: Action,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(start = PaddingWidgetDefaultHorizontal)
    ) {
        record.bitmap
            ?.let { image: Bitmap ->
                Image(
                    modifier = GlanceModifier
                        .width(WidgetImageSize * .75f)
                        .height(WidgetImageSize)
                        .clickable(imageTappedActionProvider)
                        .cornerRadius(8.dp),
                    provider = ImageProvider(image),
                    contentDescription = "meal photo",
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetImageSize * .75f))
            }
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .wrapContentHeight()
                .padding(horizontal = PaddingWidgetDefaultHorizontal)
                .clickable(bodyTappedActionProvider),
            verticalAlignment = Alignment.Vertical.Top,
        ) {
            Text(
                text = record.title,
                maxLines = 1,
                style = titleTextStyle,
            )
            Text(
                text = record.description,
                maxLines = 2,
                style = descriptionTextStyle,
            )
            Text(
                text = record.timestamp,
                maxLines = 1,
                style = dateTextStyle,
            )
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun RecordListItemPreview() {
    WidgetPreview {
        RecordListItem(
            record = RecordUIModel(
                recordId = 1,
                templateId = 1L,
                title = "Breakfast",
                description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0, Fibre: 1111dfdf sdf asdfasdf as df",
                timestamp = "Yesterday",
                bitmap = randomBitmap(),
                hasMacros = true,
            ),
            imageTappedActionProvider = action {},
            bodyTappedActionProvider = action {},
        )
    }
}
