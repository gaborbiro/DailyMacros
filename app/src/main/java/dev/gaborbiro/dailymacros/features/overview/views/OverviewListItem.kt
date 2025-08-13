package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.util.randomBitmap


@Composable
fun OverviewListItem(
    modifier: Modifier = Modifier,
    record: RecordUIModel,
    onRepeatMenuItemTapped: (RecordUIModel) -> Unit,
    onChangeImageMenuItemTapped: (RecordUIModel) -> Unit,
    onDeleteImageMenuItemTapped: (RecordUIModel) -> Unit,
    onEditRecordMenuItemTapped: (RecordUIModel) -> Unit,
    onDeleteRecordMenuItemTapped: (RecordUIModel) -> Unit,
    onNutrientsMenuItemTapped: (RecordUIModel) -> Unit,
    onRecordImageTapped: (RecordUIModel) -> Unit,
    onRecordBodyTapped: (RecordUIModel) -> Unit,
) {
    Row(
        modifier = modifier
            .padding(start = PaddingDefault, end = PaddingDefault)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordImage(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = { onRecordImageTapped(record) }),
            bitmap = record.bitmap,
        )
        Spacer(
            modifier = Modifier
                .size(PaddingDefault)
        )
        RecordTextContent(
            modifier = Modifier
                .wrapContentHeight()
                .clickable(onClick = { onRecordBodyTapped(record) })
                .padding(end = PaddingDefault)
                .weight(1f),
            record = record
        )
        PopupMenu(
            onRepeatMenuItemTapped = { onRepeatMenuItemTapped(record) },
            onChangeImageMenuItemTapped = { onChangeImageMenuItemTapped(record) },
            onDeleteImageMenuItemTapped = { onDeleteImageMenuItemTapped(record) },
            onEditRecordMenuItemTapped = { onEditRecordMenuItemTapped(record) },
            onDeleteRecordMenuItemTapped = { onDeleteRecordMenuItemTapped(record) },
            onNutrientsMenuItemTapped = { onNutrientsMenuItemTapped(record) },
        )
    }
}

@Composable
private fun RecordImage(
    modifier: Modifier,
    bitmap: Bitmap?,
) {
    bitmap?.let {
        Image(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp)),
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentScale = ContentScale.Crop,
            contentDescription = "note image",
        )
    } ?: run {
        Spacer(modifier)
    }
}

@Composable
private fun RecordTextContent(modifier: Modifier, record: RecordUIModel) {
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = record.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = record.description,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = record.timestamp,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(top = PaddingQuarter)
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListItemPreview() {
    OverviewListItem(
        record = RecordUIModel(
            recordId = 1L,
            title = "Title",
            description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
            templateId = 1L,
            bitmap = randomBitmap(),
            timestamp = "2022-01-01 00:00:00"
        ),
        onRepeatMenuItemTapped = {},
        onChangeImageMenuItemTapped = {},
        onDeleteImageMenuItemTapped = {},
        onEditRecordMenuItemTapped = {},
        onDeleteRecordMenuItemTapped = {},
        onRecordImageTapped = {},
        onRecordBodyTapped = {},
        onNutrientsMenuItemTapped = {},
    )
}
