package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.common.view.LocalImage
import dev.gaborbiro.dailymacros.features.common.view.PreviewImageStoreProvider


@Composable
fun ListItemRecord(
    modifier: Modifier = Modifier,
    record: RecordUIModel,
    onRecordImageTapped: (id: Any) -> Unit,
    onRecordBodyTapped: (id: Any) -> Unit,
    rowMenu: @Composable () -> Unit,
) {
    val onBodyTapped = remember(record.id) { { onRecordBodyTapped(record.id) } }
    val onImageTapped = remember(record.id) { { onRecordImageTapped(record.id) } }

    Row(
        modifier = modifier
            .padding(start = PaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordImage(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onImageTapped),
            image = record.images.firstOrNull(),
            title = record.title,
        )
        Spacer(
            modifier = Modifier
                .size(PaddingHalf)
        )
        RecordTextContent(
            modifier = Modifier
                .clickable(onClick = onBodyTapped)
                .padding(end = PaddingHalf)
                .weight(1f),
            record = record
        )
        rowMenu()
    }
}

@Composable
private fun RecordImage(
    modifier: Modifier,
    title: String,
    image: String?,
) {
    image?.let {
        LocalImage(
            name = it,
            modifier = modifier
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
            contentDescription = "image of $title"
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
            fontWeight = FontWeight.Bold,
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
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(top = PaddingQuarter)
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListItemPreview() {
    PreviewImageStoreProvider {
        ListItemRecord(
            record = RecordUIModel(
                recordId = 1L,
                title = "Title",
                description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                templateId = 1L,
                images = listOf("", ""),
                timestamp = "2022-01-01 00:00:00",
                hasMacros = true,
            ),
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            rowMenu = {}
        )
    }
}
