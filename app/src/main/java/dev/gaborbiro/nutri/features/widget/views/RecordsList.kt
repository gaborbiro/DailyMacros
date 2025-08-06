package dev.gaborbiro.nutri.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.size
import dev.gaborbiro.nutri.R
import dev.gaborbiro.nutri.design.PaddingWidgetHalf
import dev.gaborbiro.nutri.features.common.model.RecordViewState
import dev.gaborbiro.nutri.features.common.model.TemplateUIModel

@Composable
fun RecordsList(
    modifier: GlanceModifier,
    recentRecords: List<RecordViewState>,
    topTemplates: List<TemplateUIModel>,
    showTemplates: Boolean,
    recordTapActionProvider: (recordId: Long) -> Action,
    templateTapActionProvider: (templateId: Long) -> Action,
    onTemplatesExpandButtonTapped: () -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .padding(vertical = PaddingWidgetHalf),
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
        ) {
            val index = mapListIndex(recentRecords.size, it)
            when {
                it < recentRecords.size -> {
                    val record = recentRecords[index]
                    RecordListItem(
                        record = record,
                        tapActionProvider = recordTapActionProvider(record.recordId),
                    )
                }

                it == recentRecords.size -> {
                    SectionTitle(
                        title = "Top Templates",
                        trailingImage = if (showTemplates) R.drawable.keyboard_arrow_down else R.drawable.keyboard_arrow_right,
                        onClick = onTemplatesExpandButtonTapped
                    )
                }

                else -> {
                    if (showTemplates) {
                        val template = topTemplates[index]
                        WidgetTemplateListItem(
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
