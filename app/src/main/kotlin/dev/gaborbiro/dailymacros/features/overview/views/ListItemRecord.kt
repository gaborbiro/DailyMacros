package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.views.LocalImage
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.features.shared.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.widget.views.ListItemImageCornerRadius
import dev.gaborbiro.dailymacros.ui.components.CompactMacroNutrientsGrid

@Composable
fun ListItemRecord(
    modifier: Modifier = Modifier,
    record: ListUiModelRecord,
    onRecordImageTapped: (recordId: Long) -> Unit,
    onRecordBodyTapped: (recordId: Long) -> Unit,
    rowMenu: @Composable () -> Unit,
) {
    val onBodyTapped = remember(record.listItemId) { { onRecordBodyTapped(record.listItemId) } }
    val onImageTapped = remember(record.listItemId) { { onRecordImageTapped(record.listItemId) } }

    Row(
        modifier = modifier
            .padding(start = PaddingHalf)
            .clickable(onClick = onBodyTapped),
        verticalAlignment = Alignment.Top,
    ) {
        RecordImage(
            modifier = Modifier
                .size(73.dp)
                .padding(top = 4.dp)
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
                .clip(RoundedCornerShape(ListItemImageCornerRadius)),
            contentScale = ContentScale.Crop,
            contentDescription = "image of $title"
        )
    } ?: run {
        Spacer(modifier)
    }
}

@Composable
private fun RecordTextContent(modifier: Modifier, record: ListUiModelRecord) {
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (record.showOtherLoggedVariantsIcon) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.record_title_has_other_versions_a11y),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f),
                text = record.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.W400,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier
                    .padding(start = PaddingHalf, top = PaddingQuarter),
                text = record.timestamp,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (record.showLoadingIndicator) {
            Text(
                text = "Analyzing…",
                maxLines = 1,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
        } else {
            record.nutrients?.let { nutrients ->
                CompactMacroNutrientsGrid(
                    modifier = Modifier.fillMaxWidth(),
                    nutrients = nutrients,
                )
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 260)
private fun OverviewListItemPreview() {
    ViewPreviewContext {
        ListItemRecord(
            record = ListUiModelRecord(
                recordId = 1L,
                title = "Title",
                templateId = 1L,
                images = listOf("", ""),
                timestamp = "Tue 19 Aug, 20:49",
                nutrients = NutrientsUiModel(
                    calories = "1008kcal",
                    protein = "Protein 158g",
                    fat = "Fat 14g(12g)",
                    carbs = "Carbs 39g(29g/19g)",
                    salt = "Salt 1.2g",
                    fibre = "Fibre 14g",
                ),
                showLoadingIndicator = false,
            ),
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            rowMenu = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 300)
@Composable
private fun OverviewListItemPreviewMissing() {
    ViewPreviewContext {
        ListItemRecord(
            record = ListUiModelRecord(
                recordId = 1L,
                title = "Title",
                templateId = 1L,
                images = listOf("", ""),
                timestamp = "Tue 19 Aug, 20:49",
                nutrients = NutrientsUiModel(
                    calories = "1008kcal",
                    protein = "",
                    fat = "Fat 14g(12g)",
                    carbs = "",
                    salt = "Salt 1.2g",
                    fibre = "",
                ),
                showLoadingIndicator = false,
            ),
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            rowMenu = {}
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListItemPreviewLoading() {
    ViewPreviewContext {
        ListItemRecord(
            record = ListUiModelRecord(
                recordId = 1L,
                title = "Title",
                templateId = 1L,
                images = listOf("", ""),
                timestamp = "Tue 19 Aug, 20:49",
                nutrients = NutrientsUiModel(
                    calories = "1008kcal",
                    protein = "Protein 58g",
                    fat = "Fat 14g(12g)",
                    carbs = "Carbs 39g(29g/19g)",
                    salt = "Salt 1.2g",
                    fibre = "Fibre 14g",
                ),
                showLoadingIndicator = true,
            ),
            onRecordImageTapped = {},
            onRecordBodyTapped = {},
            rowMenu = {}
        )
    }
}
