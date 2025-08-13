package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.wrapContentHeight
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.widget.model.TemplateUIModel
import dev.gaborbiro.dailymacros.features.widget.NotesWidgetNavigator

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
        RecordsList(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            recentRecords = recentRecords,
            topTemplates = topTemplates,
            showTemplates = showTopTemplates,
            recordImageTapActionProvider = { recordId -> navigator.getRecordImageTappedAction(recordId) },
            recordBodyTapActionProvider = { recordId -> navigator.getRecordBodyTappedAction(recordId) },
            templateTapActionProvider = { templateId -> navigator.getApplyTemplateAction(templateId) },
            onTemplatesExpandButtonTapped = onTemplatesExpandButtonTapped,
        )
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
