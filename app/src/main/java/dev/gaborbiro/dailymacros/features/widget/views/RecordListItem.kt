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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefault
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
            .padding(top = PaddingWidgetDefault)
    ) {
        record.bitmap
            ?.let { image: Bitmap ->
                Image(
                    modifier = GlanceModifier
                        .size(WidgetImageSize)
                        .clickable(imageTappedActionProvider)
                        .cornerRadius(8.dp),
                    provider = ImageProvider(image),
                    contentDescription = "meal photo",
                    contentScale = ContentScale.Crop,
                )
            }
            ?: run {
                Spacer(modifier = GlanceModifier.size(WidgetImageSize))
            }
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .wrapContentHeight()
                .padding(horizontal = PaddingWidgetDefault)
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
                description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0, Fiber: 1111dfdf sdf asdfasdf as df",
                timestamp = "Yesterday",
                bitmap = randomBitmap(),
            ),
            imageTappedActionProvider = action {},
            bodyTappedActionProvider = action {},
        )
    }
}
