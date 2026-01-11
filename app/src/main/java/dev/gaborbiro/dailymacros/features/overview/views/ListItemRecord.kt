package dev.gaborbiro.dailymacros.features.overview.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.design.ViewPreviewContext
import dev.gaborbiro.dailymacros.design.darkExtraColorScheme
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.MacrosAmountsUIModel
import dev.gaborbiro.dailymacros.features.common.views.LocalImage
import dev.gaborbiro.dailymacros.features.common.views.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.widgetDiary.views.ListItemImageCornerRadius


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
                .clip(RoundedCornerShape(ListItemImageCornerRadius)),
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
        if (record.showLoadingIndicator) {
            Text(
                text = "Analyzing…",
                maxLines = 1,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            record.macrosAmounts?.let {
                MacroRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MacroPill(text = it.calories ?: "", bg = darkExtraColorScheme.calorieColor, protectEndOfText = false)
                    MacroPill(text = it.protein ?: "", bg = darkExtraColorScheme.proteinColor)
                    MacroPill(text = it.fat ?: "", bg = darkExtraColorScheme.fatColor)
                }
                Spacer(Modifier.height(4.dp))
                MacroRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MacroPill(text = it.carbs ?: "", bg = darkExtraColorScheme.carbsColor)
                    MacroPill(text = it.salt ?: "", bg = darkExtraColorScheme.saltColor)
                    MacroPill(text = it.fibre ?: "", bg = darkExtraColorScheme.fibreColor)
                }
            }
        }
    }
}

@Composable
private fun MacroPill(
    text: String,
    bg: Color,
    color: Color = Color.Black,
    protectEndOfText: Boolean = true,
) {
    if (text.isBlank()) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
        )
        return
    }

    Card(
        modifier = Modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = PaddingQuarter, vertical = 2.dp),
            text = text,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = if (protectEndOfText) TextOverflow.StartEllipsis else TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MacroRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    val spacingPx = with(LocalDensity.current) { spacing.roundToPx() }

    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->

        val n = measurables.size
        val maxWidth = constraints.maxWidth
        if (n == 0) return@Layout layout(maxWidth, 0) {}

        val totalSpacing = spacingPx * (n - 1)
        val available = (maxWidth - totalSpacing).coerceAtLeast(0)
        val equal = available / n

        val required = measurables.map {
            it.maxIntrinsicWidth(constraints.maxHeight)
        }

        // Step 1: start with equal columns
        val widths = IntArray(n) { equal }

        // Step 2: find how much extra is needed by oversized pills
        var deficit = 0
        for (i in 0 until n) {
            val need = required[i] - equal
            if (need > 0) deficit += need
        }

        // Step 3: see how much can be stolen from smaller pills
        var pool = 0
        for (i in 0 until n) {
            if (required[i] < equal) {
                pool += equal - required[i]
            }
        }

        val transferable = minOf(deficit, pool)

        // Step 4: shrink small pills proportionally
        if (transferable > 0) {
            val shrinkables = (0 until n).filter { required[it] < equal }

            var remaining = transferable
            for (i in shrinkables) {
                val maxShrink = equal - required[i]
                val take = minOf(maxShrink, remaining)
                widths[i] -= take
                remaining -= take
                if (remaining == 0) break
            }

            // Step 5: grow big pills with what we took
            val growables = (0 until n).filter { required[it] > equal }
            var give = transferable
            for (i in growables) {
                val need = required[i] - equal
                val add = minOf(need, give)
                widths[i] += add
                give -= add
                if (give == 0) break
            }
        }

        val placeables = measurables.mapIndexed { i, m ->
            m.measure(Constraints.fixedWidth(widths[i]))
        }

        val height = placeables.maxOf { it.height }

        layout(maxWidth, height) {
            var x = 0
            for (i in 0 until n) {
                val p = placeables[i]
                p.placeRelative(x, 0)
                x += p.width + if (i != n - 1) spacingPx else 0
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 260)
private fun OverviewListItemPreview() {
    ViewPreviewContext {
        PreviewImageStoreProvider {
            ListItemRecord(
                record = ListUIModelRecord(
                    recordId = 1L,
                    title = "Title",
                    templateId = 1L,
                    images = listOf("", ""),
                    timestamp = "Tue 19 Aug, 20:49",
                    macrosAmounts = MacrosAmountsUIModel(
                        calories = "1008kcal",
                        protein = "Protein 158g",
                        fat = "Fat 14g(12g)",
                        carbs = "Carbs 39g(29g/19g)",
                        salt = "Salt 1.2g",
                        fibre = "Fibre 14g",
                    ),
                ),
                onRecordImageTapped = {},
                onRecordBodyTapped = {},
                rowMenu = {}
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 300)
@Composable
private fun OverviewListItemPreviewMissing() {
    ViewPreviewContext {
        PreviewImageStoreProvider {
            ListItemRecord(
                record = ListUIModelRecord(
                    recordId = 1L,
                    title = "Title",
                    templateId = 1L,
                    images = listOf("", ""),
                    timestamp = "Tue 19 Aug, 20:49",
                    macrosAmounts = MacrosAmountsUIModel(
                        calories = "1008kcal",
                        protein = null,
                        fat = "Fat 14g(12g)",
                        carbs = null,
                        salt = "Salt 1.2g",
                        fibre = null,
                    ),
                ),
                onRecordImageTapped = {},
                onRecordBodyTapped = {},
                rowMenu = {}
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OverviewListItemPreviewLoading() {
    ViewPreviewContext {
        PreviewImageStoreProvider {
            ListItemRecord(
                record = ListUIModelRecord(
                    recordId = 1L,
                    title = "Title",
                    templateId = 1L,
                    images = listOf("", ""),
                    timestamp = "Tue 19 Aug, 20:49",
                    macrosAmounts = MacrosAmountsUIModel(
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
}
