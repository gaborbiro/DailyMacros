package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview
import dev.gaborbiro.dailymacros.util.randomBitmap

@Composable
internal fun RecordsList(
    modifier: GlanceModifier,
    recentRecords: List<RecordUIModel>,
    topTemplates: List<TemplateUIModel>,
    showTemplates: Boolean,
    recordImageTapActionProvider: @Composable (recordId: Long) -> Action,
    recordBodyTapActionProvider: @Composable (recordId: Long) -> Action,
    templateTapActionProvider: @Composable (templateId: Long) -> Action,
    onTemplatesExpandButtonTapped: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(
            count = recentRecords.size + topTemplates.size + 1,
            itemId = {
                val index = mapListIndex(recentRecords.size, it)
                when {
                    it < recentRecords.size -> {
                        -recentRecords[index].recordId
                    }

                    it == recentRecords.size -> {
                        0
                    }

                    else -> {
                        topTemplates[index].templateId
                    }
                }
            }
        ) { index ->
            val mappedIndex = mapListIndex(recentRecords.size, index)
            when {
                index < recentRecords.size -> {
                    val record = recentRecords[mappedIndex]
                    RecordListItem(
                        record = record,
                        imageTappedActionProvider = recordImageTapActionProvider(record.recordId),
                        bodyTappedActionProvider = recordBodyTapActionProvider(record.recordId),
                    )
                }

                index == recentRecords.size -> {
                    SectionTitle(
                        title = "Top Meals",
                        trailingImage = if (showTemplates) R.drawable.keyboard_arrow_down else R.drawable.keyboard_arrow_right,
                        onClick = onTemplatesExpandButtonTapped,
                    )
                }

                else -> {
                    if (showTemplates) {
                        val template = topTemplates[mappedIndex]
                        TemplateListItem(
                            template = template,
                            tapActionProvider = templateTapActionProvider(template.templateId),
                        )
                    } else {
                        Spacer(modifier = GlanceModifier.size(5.dp))
                    }
                }
            }
        }
    }
}

private fun mapListIndex(recentRecordsSize: Int, index: Int) = when {
    index < recentRecordsSize -> index
    index == recentRecordsSize -> recentRecordsSize
    else -> index - 1 - recentRecordsSize
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun RecordListPreviewExpanded() {
    WidgetPreview {
        RecordsList(
            modifier = GlanceModifier
                .fillMaxWidth(),
            recentRecords = listOf(
                RecordUIModel(
                    recordId = 1,
                    templateId = 1L,
                    title = "Breakfast",
                    description = "8cal, Prot 8, Carb 9, Sug 9, Fat 4, Sat 2, Sal: 0",
                    timestamp = "Yesterday",
                    bitmap = randomBitmap(),
                ),
                RecordUIModel(
                    recordId = 2L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Lunch",
                    description = "8cal, Prot 8, Carb 9, Sug 9, Fat 4, Sat 2, Sal: 0",
                    bitmap = null,
                ),
                RecordUIModel(
                    recordId = 3L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Dinner",
                    description = "8cal, Prot 8, Carb 9, Sug 9, Fat 4, Sat 2, Sal: 0",
                    bitmap = randomBitmap(),
                ),
            ),
            topTemplates = listOf(
                TemplateUIModel(
                    templateId = 1,
                    title = "Breakfast",
                    bitmap = randomBitmap(),
                ),
                TemplateUIModel(
                    templateId = 2,
                    title = "Lunch",
                    bitmap = randomBitmap(),
                ),
                TemplateUIModel(
                    templateId = 3,
                    title = "Dinner",
                    bitmap = null,
                ),
            ),
            showTemplates = true,
            recordImageTapActionProvider = { action {} },
            recordBodyTapActionProvider = { action {} },
            templateTapActionProvider = { action {} },
            onTemplatesExpandButtonTapped = { },
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun RecordListPreviewCollapsed() {
    WidgetPreview {
        RecordsList(
            modifier = GlanceModifier
                .fillMaxWidth(),
            recentRecords = listOf(
                RecordUIModel(
                    recordId = 1,
                    templateId = 1L,
                    title = "Breakfast",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    timestamp = "Yesterday",
                    bitmap = null,
                ),
                RecordUIModel(
                    recordId = 2L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Lunch",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    bitmap = randomBitmap(),
                ),
                RecordUIModel(
                    recordId = 3L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Dinner",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    bitmap = randomBitmap(),
                ),
            ),
            topTemplates = emptyList(),
            showTemplates = false,
            recordImageTapActionProvider = { action {} },
            recordBodyTapActionProvider = { action {} },
            templateTapActionProvider = { action {} },
            onTemplatesExpandButtonTapped = { },
        )
    }
}
