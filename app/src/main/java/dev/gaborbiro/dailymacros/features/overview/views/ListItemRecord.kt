package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
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
            .padding(start = PaddingDefault),
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
                style = MaterialTheme.typography.labelMedium,
            )
        }
        record.macros?.let {
            FlowRow(
                horizontalArrangement = Arrangement.Start,
            ) {
                MacroPill(
                    text = it.calories,
                    bg = DailyMacrosColors.calorieColor,
                    color = Color.Black.copy(alpha = .7f),
                )
                MacroPill(
                    text = it.protein,
                    bg = DailyMacrosColors.proteinColor,
                    color = Color.Black.copy(alpha = .7f),
                )
                MacroPill(
                    text = it.fat,
                    bg = DailyMacrosColors.fatColor,
                    color = Color.Black.copy(alpha = .7f),
                )
                MacroPill(
                    text = it.carbs,
                    bg = DailyMacrosColors.carbsColor,
                    color = Color.Black.copy(alpha = .7f),
                )
                MacroPill(
                    text = it.salt,
                    bg = DailyMacrosColors.saltColor,
                    color = Color.Black.copy(alpha = .7f),
                )
                MacroPill(
                    text = it.fibre,
                    bg = DailyMacrosColors.fibreColor,
                    color = Color.Black.copy(alpha = .7f),
                )
            }
        }
    }
}

@Composable
private fun MacroPill(
    text: String?,
    bg: Color,
    color: Color,
) {
    Card(
        modifier = Modifier
            .width(86.dp)
            .padding(top = 4.dp)
            .padding(end = 4.dp),
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
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
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
                    calories = "8cal",
                    protein = "prot 8",
                    fat = "fat 4(2)",
                    carbs = "carb 9(9)",
                    salt = "sal 2",
                    fibre = "fib 4",
                ),
            ),
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            rowMenu = {}
        )
    }
}
