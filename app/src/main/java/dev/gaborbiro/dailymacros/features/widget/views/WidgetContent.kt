package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.widget.NotesWidgetNavigator
import dev.gaborbiro.dailymacros.features.widget.NotesWidgetNavigatorImpl
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetHalfVertical
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview
import dev.gaborbiro.dailymacros.util.randomBitmap

@Composable
internal fun WidgetContent(
    modifier: GlanceModifier,
    showTopTemplates: Boolean,
    onTemplatesExpandButtonTapped: () -> Unit,
    navigator: NotesWidgetNavigator,
    recentRecords: List<RecordUIModel>,
    topTemplates: List<TemplateUIModel>,
) {
    Column(
        modifier = modifier
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            WidgetList(
                recentRecords = recentRecords,
                topTemplates = topTemplates,
                showTemplates = showTopTemplates,
                recordImageTapActionProvider = { recordId -> navigator.getRecordImageTappedAction(recordId) },
                recordBodyTapActionProvider = { recordId -> navigator.getRecordBodyTappedAction(recordId) },
                templateImageTapActionProvider = { templateId -> navigator.getTemplateImageTappedAction(templateId) },
                templateBodyTapActionProvider = { templateId -> navigator.getTemplateBodyTappedAction(templateId) },
                onTemplatesExpandButtonTapped = onTemplatesExpandButtonTapped,
            )
            Box(
                modifier = GlanceModifier
                    .size(56.dp)
                    .padding(PaddingWidgetHalfVertical),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = GlanceModifier
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .padding(12.dp)
                        .clickable(navigator.getOpenAppAction())
                        .fillMaxSize(),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                    provider = ImageProvider(R.drawable.ic_open_in_new),
                    contentDescription = "open app",
                )
            }
        }
        ButtonLayout(
            modifier = GlanceModifier
                .fillMaxWidth()
                .wrapContentHeight(),
            launchNoteViaCameraAction = { navigator.getLaunchNewNoteViaCameraAction() },
            launchNewNoteViaImagePickerActionProvider = { navigator.getLaunchNewNoteViaImagePickerAction() },
            launchNewNoteViaTextOnlyActionProvider = { navigator.getLaunchNewNoteViaTextOnlyAction() },
            reloadActionProvider = { navigator.getReloadAction() },
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun WidgetContentPreviewExpanded() {
    WidgetPreview {
        WidgetContent(
            modifier = GlanceModifier
                .fillMaxSize(),
            navigator = NotesWidgetNavigatorImpl(),
            showTopTemplates = true,
            onTemplatesExpandButtonTapped = {},
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
            topTemplates = listOf(
                TemplateUIModel(
                    templateId = 1,
                    title = "Breakfast",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    bitmap = randomBitmap(),
                ),
                TemplateUIModel(
                    templateId = 2,
                    title = "Lunch",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    bitmap = randomBitmap(),
                ),
                TemplateUIModel(
                    templateId = 3,
                    title = "Dinner",
                    description = "8cal, Prot 8, Carb 9, Suga 9, Fat 4, Sat 2, Sal: 0",
                    bitmap = null,
                ),
            ),
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun WidgetContentPreviewCollapsed() {
    WidgetPreview {
        WidgetContent(
            modifier = GlanceModifier
                .fillMaxSize(),
            navigator = NotesWidgetNavigatorImpl(),
            showTopTemplates = false,
            onTemplatesExpandButtonTapped = {},
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
        )
    }
}
