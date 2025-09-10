package dev.gaborbiro.dailymacros.features.overview.views

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.Configuration
import android.util.Range
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.gaborbiro.dailymacros.design.ExtraColors
import dev.gaborbiro.dailymacros.design.AppTheme
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelMacroProgress
import dev.gaborbiro.dailymacros.features.common.model.ListUIModelRecord
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.common.view.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.features.widget.NotesWidgetReceiver

@Composable
internal fun OverviewView(
    viewState: OverviewViewState,
    onRepeatMenuItemTapped: (id: Long) -> Unit,
    onDetailsMenuItemTapped: (id: Long) -> Unit,
    onDeleteRecordMenuItemTapped: (id: Long) -> Unit,
    onMacrosMenuItemTapped: (id: Long) -> Unit,
    onRecordImageTapped: (id: Long) -> Unit,
    onRecordBodyTapped: (id: Long) -> Unit,
    onUndoDeleteTapped: () -> Unit,
    onUndoDeleteDismissed: () -> Unit,
    onUndoDeleteSnackbarShown: () -> Unit,
    onSearchTermChanged: (String?) -> Unit,
    onSettingsButtonTapped: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewState.showUndoDeleteSnackbar) {
        if (viewState.showUndoDeleteSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Note deleted",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onUndoDeleteTapped()
                SnackbarResult.Dismissed -> onUndoDeleteDismissed()
            }
            onUndoDeleteSnackbarShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        floatingActionButton = {
            if (viewState.items.isNotEmpty()) {
                SearchFAB {
                    onSearchTermChanged(it)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { paddingValues ->
        if (viewState.items.isNotEmpty()) {
            OverviewList(
                viewState = viewState,
                paddingValues = paddingValues,
                onRepeatMenuItemTapped = onRepeatMenuItemTapped,
                onDetailsMenuItemTapped = onDetailsMenuItemTapped,
                onDeleteRecordMenuItemTapped = onDeleteRecordMenuItemTapped,
                onMacrosMenuItemTapped = onMacrosMenuItemTapped,
                onRecordImageTapped = onRecordImageTapped,
                onRecordBodyTapped = onRecordBodyTapped,
                onSettingsButtonTapped = onSettingsButtonTapped,
            )
        } else if (viewState.showAddWidgetButton) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                AddWidgetButton()
            }
        }
    }
}

@Composable
fun AddWidgetButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            val widgetProvider = ComponentName(context, NotesWidgetReceiver::class.java)

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
            } else {
                Toast.makeText(context, "Pinning widgets is not supported on this launcher", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Tap here to add Widget to Home Screen")
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NotesListPreview() {
    AppTheme {
        PreviewImageStoreProvider {
            OverviewView(
                viewState = OverviewViewState(
                    items = listOf(
                        ListUIModelMacroProgress(
                            listItemId = 1L,
                            dayTitle = "Yesterday",
                            progress = listOf(
                                MacroProgressItem(
                                    title = "Calories",
                                    progress0to1 = .15f,
                                    progressLabel = "1005kcal",
                                    targetRange0to1 = Range(.84f, .88f),
                                    rangeLabel = "2.1-2.2k",
                                    color = ExtraColors.calorieColor,
                                ),
                                MacroProgressItem(
                                    title = "Protein",
                                    progress0to1 = .0809f,
                                    progressLabel = "110g",
                                    targetRange0to1 = Range(.8095f, .9047f),
                                    rangeLabel = "170-190g",
                                    color = ExtraColors.proteinColor,
                                ),
                                MacroProgressItem(
                                    title = "Fat",
                                    progress0to1 = .2121f,
                                    progressLabel = "30g",
                                    targetRange0to1 = Range(.6818f, .9091f),
                                    rangeLabel = "45-60g",
                                    color = ExtraColors.fatColor,
                                ),
                                MacroProgressItem(
                                    title = "Carbs",
                                    progress0to1 = .1818f,
                                    progressLabel = "105g",
                                    targetRange0to1 = Range(.6818f, .9091f),
                                    rangeLabel = "150-200g",
                                    color = ExtraColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Sugar",
                                    progress0to1 = .2955f,
                                    progressLabel = "35g",
                                    targetRange0to1 = Range(.9091f, .9091f),
                                    rangeLabel = "<40g/<25g added",
                                    color = ExtraColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Salt",
                                    progress0to1 = .0f,
                                    progressLabel = "0g",
                                    targetRange0to1 = Range(.9091f, .9091f),
                                    rangeLabel = "<5g (≈2g Na)",
                                    color = ExtraColors.saltColor,
                                ),
                                MacroProgressItem(
                                    title = "Fibre",
                                    progress0to1 = .0f,
                                    progressLabel = "0g",
                                    targetRange0to1 = Range(.9091f, .9091f),
                                    rangeLabel = "30-38g",
                                    color = ExtraColors.fibreColor,
                                ),
                            )
                        ),
                        ListUIModelRecord(
                            recordId = 2L,
                            title = "Title",
                            templateId = 2L,
                            images = listOf("", ""),
                            timestamp = "17:00",
                            macros = MacrosUIModel(
                                calories = "8cal",
                                protein = "prot 8",
                                fat = "fat 4(2)",
                                carbs = "carb 9(9)",
                                salt = "sal 2",
                                fibre = "fib 4",
                            ),
                        ),
                        ListUIModelRecord(
                            recordId = 3L,
                            title = "Title 2",
                            templateId = 4L,
                            images = listOf("", ""),
                            timestamp = "15:38",
                            macros = MacrosUIModel(
                                calories = "8cal",
                                protein = "prot 8",
                                fat = "fat 4(2)",
                                carbs = "carb 9(9)",
                                salt = "sal 2",
                                fibre = "fib 4",
                            ),
                        )
                    ),
                ),
                onRepeatMenuItemTapped = {},
                onDetailsMenuItemTapped = {},
                onDeleteRecordMenuItemTapped = {},
                onRecordImageTapped = {},
                onRecordBodyTapped = {},
                onUndoDeleteTapped = {},
                onUndoDeleteDismissed = {},
                onUndoDeleteSnackbarShown = {},
                onSearchTermChanged = {},
                onMacrosMenuItemTapped = {},
                onSettingsButtonTapped = {},
            )
        }
    }
}
