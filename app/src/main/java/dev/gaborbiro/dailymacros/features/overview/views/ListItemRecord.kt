package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.common.view.LocalImage
import dev.gaborbiro.dailymacros.features.common.view.PreviewImageStoreProvider


@Composable
fun ListItemRecord(
    modifier: Modifier = Modifier,
    record: ListUIModelRecord,
    onRecordImageTapped: (id: Long) -> Unit,
    onRecordBodyTapped: (id: Long) -> Unit,
    rowMenu: @Composable () -> Unit,
) {
    val onBodyTapped = remember(record.listItemId) { { onRecordBodyTapped(record.listItemId) } }
    val onImageTapped = remember(record.listItemId) { { onRecordImageTapped(record.listItemId) } }

    Row(
        modifier = modifier
            .padding(start = PaddingHalf),
        verticalAlignment = Alignment.Top,
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
private fun RecordTextContent(modifier: Modifier, record: ListUIModelRecord) {
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f),
                text = record.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                modifier = Modifier
                    .padding(top = PaddingQuarter),
                text = record.timestamp,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        record.macros?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MacroPill(Modifier.weight(1f), it.calories, DailyMacrosColors.calorieColor)
                MacroPill(Modifier.weight(1f), it.protein, DailyMacrosColors.proteinColor)
                MacroPill(Modifier.weight(1f), it.fat, DailyMacrosColors.fatColor)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MacroPill(Modifier.weight(1f), it.carbs, DailyMacrosColors.carbsColor)
                MacroPill(Modifier.weight(1f), it.salt, DailyMacrosColors.saltColor)
                MacroPill(Modifier.weight(1f), it.fibre, DailyMacrosColors.fibreColor)
            }
        }
    }
}

@Composable
private fun MacroPill(
    modifier: Modifier,
    text: String?,
    bg: Color,
    color: Color = Color.Black,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (text != null) bg else Color.Transparent),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            text = text ?: "",
            color = color,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 200)
private fun OverviewListItemPreview() {
    PreviewImageStoreProvider {
        ListItemRecord(
            record = ListUIModelRecord(
                recordId = 1L,
                title = "Title",
                templateId = 1L,
                images = listOf("", ""),
                timestamp = "Tue 19 Aug, 20:49",
                macros = MacrosUIModel(
                    calories = "1008cal",
                    protein = "protein 8",
                    fat = "fat 4(2)",
                    carbs = "carbs 9(9)",
                    salt = "salt 2",
                    fibre = "fibre 4",
                ),
            ),
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            rowMenu = {}
        )
    }
}
