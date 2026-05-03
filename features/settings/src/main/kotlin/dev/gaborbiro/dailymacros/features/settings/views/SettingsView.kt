package dev.gaborbiro.dailymacros.features.settings.views

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.settings.model.SettingsUiState
import dev.gaborbiro.dailymacros.features.settings.util.verticalScrollWithBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsView(
    viewState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBackNavigateRequested: () -> Unit,
    onTargetsSettingTapped: () -> Unit,
    onExportSettingTapped: () -> Unit,
    onExportDbTapped: () -> Unit,
    onImportDbTapped: () -> Unit,
    onVariabilityMiningPreviewTapped: () -> Unit,
    onClearVariabilityProfileTapped: () -> Unit,
    onCopyVariabilityRequestJson: () -> Unit,
    onCopyVariabilityResponseJson: () -> Unit,
    onVariabilityMiningRequestJsonExpansionBitsChange: (String) -> Unit,
    onVariabilityMiningResponseJsonExpansionBitsChange: (String) -> Unit,
    onVariabilityMiningRequestJsonSectionExpandedChange: (Boolean) -> Unit,
    onVariabilityMiningResponseJsonSectionExpandedChange: (Boolean) -> Unit,
) {

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackNavigateRequested) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back Button"
                        )
                    }
                },
            )
        },
        bottomBar = {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .padding(WindowInsets.navigationBars.asPaddingValues()),
                    text = viewState.bottomLabel,
                    textAlign = TextAlign.Center,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            SettingRow(title = "Daily targets", onTapped = onTargetsSettingTapped)
            SettingRow(title = "Export summary JSON", onTapped = onExportSettingTapped)
            val backupIdle =
                !viewState.exportDataInProgress && !viewState.importDataInProgress
            SettingRow(
                title = "Export data",
                onTapped = onExportDbTapped,
                enabled = backupIdle,
                trailing = {
                    if (viewState.exportDataInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
            SettingRow(
                title = "Import data (irreversible)",
                onTapped = onImportDbTapped,
                enabled = backupIdle,
                trailing = {
                    if (viewState.importDataInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = onVariabilityMiningPreviewTapped,
                enabled = !viewState.variabilityMiningLoading,
            ) {
                Text("Preview meal variability (AI) — ${viewState.templateCountForVariabilityButton} templates")
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onClearVariabilityProfileTapped,
                enabled = !viewState.variabilityMiningLoading,
            ) {
                Text("Clear meal variability profile")
            }
            if (viewState.variabilityMiningLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp)
                        .height(32.dp),
                )
            }
            viewState.variabilityMiningError?.let { err ->
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollWithBar()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (
                    viewState.variabilityMiningRequestJson != null ||
                    viewState.variabilityMiningResponseJson != null
                ) {
                    val hasRequestJson = viewState.variabilityMiningRequestJson != null
                    val hasResponseJson = viewState.variabilityMiningResponseJson != null

                    viewState.variabilityMiningGeneratedAt?.let { line ->
                        Text(
                            modifier = Modifier.padding(bottom = 8.dp),
                            text = line,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    CollapsibleJsonRow(
                        title = "Request JSON",
                        json = viewState.variabilityMiningRequestJson,
                        onCopyAll = onCopyVariabilityRequestJson,
                        jsonExpansionBits = viewState.variabilityMiningRequestJsonExpansionBits,
                        onJsonExpansionBitsChange = onVariabilityMiningRequestJsonExpansionBitsChange,
                        sectionExpanded = viewState.variabilityMiningRequestJsonSectionExpanded,
                        onSectionExpandedChange = onVariabilityMiningRequestJsonSectionExpandedChange,
                    )
                    if (hasRequestJson && hasResponseJson) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    CollapsibleJsonRow(
                        title = "Response JSON",
                        json = viewState.variabilityMiningResponseJson,
                        onCopyAll = onCopyVariabilityResponseJson,
                        jsonExpansionBits = viewState.variabilityMiningResponseJsonExpansionBits,
                        onJsonExpansionBitsChange = onVariabilityMiningResponseJsonExpansionBitsChange,
                        sectionExpanded = viewState.variabilityMiningResponseJsonSectionExpanded,
                        onSectionExpandedChange = onVariabilityMiningResponseJsonSectionExpandedChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleJsonRow(
    title: String,
    json: String?,
    onCopyAll: () -> Unit,
    jsonExpansionBits: String,
    onJsonExpansionBitsChange: (String) -> Unit,
    sectionExpanded: Boolean,
    onSectionExpandedChange: (Boolean) -> Unit,
) {
    if (json == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .clickable { onSectionExpandedChange(!sectionExpanded) },
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        JsonToggleButton(
            expanded = sectionExpanded,
            onClick = { onSectionExpandedChange(!sectionExpanded) },
        )
    }
    if (sectionExpanded) {
        if (json.isNotBlank()) {
            InteractiveJsonViewer(
                json = json,
                onCopyAll = onCopyAll,
                expandedBits = jsonExpansionBits,
                onExpandedBitsChange = onJsonExpansionBitsChange,
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onTapped: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onTapped,
            )
            .alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            text = title,
        )
        trailing?.invoke()
        Spacer(modifier = Modifier.padding(end = 16.dp))
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SettingsViewPreview() {
    SettingsPreviewContext {
        SettingsView(
            viewState = SettingsUiState(
                showTargetsSettings = true,
                bottomLabel = "Bottom Label",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackNavigateRequested = {},
            onTargetsSettingTapped = {},
            onExportSettingTapped = {},
            onExportDbTapped = {},
            onImportDbTapped = {},
            onVariabilityMiningPreviewTapped = {},
            onClearVariabilityProfileTapped = {},
            onCopyVariabilityRequestJson = {},
            onCopyVariabilityResponseJson = {},
            onVariabilityMiningRequestJsonExpansionBitsChange = {},
            onVariabilityMiningResponseJsonExpansionBitsChange = {},
            onVariabilityMiningRequestJsonSectionExpandedChange = {},
            onVariabilityMiningResponseJsonSectionExpandedChange = {},
        )
    }
}
