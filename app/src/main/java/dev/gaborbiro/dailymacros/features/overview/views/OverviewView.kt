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
import androidx.compose.material3.MaterialTheme
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
import dev.gaborbiro.dailymacros.design.DailyMacrosColors
import dev.gaborbiro.dailymacros.design.DailyMacrosTheme
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressItem
import dev.gaborbiro.dailymacros.features.common.model.MacroProgressUIModel
import dev.gaborbiro.dailymacros.features.common.model.MacrosUIModel
import dev.gaborbiro.dailymacros.features.common.model.RecordUIModel
import dev.gaborbiro.dailymacros.features.common.view.PreviewImageStoreProvider
import dev.gaborbiro.dailymacros.features.overview.model.OverviewViewState
import dev.gaborbiro.dailymacros.features.widget.NotesWidgetReceiver
import java.time.LocalDate

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
        containerColor = MaterialTheme.colorScheme.background,
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
            )
        } else {
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
    DailyMacrosTheme {
        PreviewImageStoreProvider {
            OverviewView(
                viewState = OverviewViewState(
                    items = listOf(
                        MacroProgressUIModel(
                            date = LocalDate.now(),
                            macros = listOf(
                                MacroProgressItem(
                                    title = "Calories",
                                    progress = .15f,
                                    progressLabel = "1005 cal",
                                    range = Range(.84f, .88f),
                                    rangeLabel = "2.1-2.2kcal",
                                    color = DailyMacrosColors.calorieColor,
                                ),
                                MacroProgressItem(
                                    title = "Protein",
                                    progress = .0809f,
                                    progressLabel = "110g",
                                    range = Range(.8095f, .9047f),
                                    rangeLabel = "170-190g",
                                    color = DailyMacrosColors.proteinColor,
                                ),
                                MacroProgressItem(
                                    title = "Fat",
                                    progress = .2121f,
                                    progressLabel = "30g",
                                    range = Range(.6818f, .9091f),
                                    rangeLabel = "45-60g",
                                    color = DailyMacrosColors.fatColor,
                                ),
                                MacroProgressItem(
                                    title = "Carbs",
                                    progress = .1818f,
                                    progressLabel = "105g",
                                    range = Range(.6818f, .9091f),
                                    rangeLabel = "150-200g",
                                    color = DailyMacrosColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Sugar",
                                    progress = .2955f,
                                    progressLabel = "35g",
                                    range = Range(.9091f, .9091f),
                                    rangeLabel = "<40g ttl., <25g",
                                    color = DailyMacrosColors.carbsColor,
                                ),
                                MacroProgressItem(
                                    title = "Salt",
                                    progress = .0f,
                                    progressLabel = "0g",
                                    range = Range(.9091f, .9091f),
                                    rangeLabel = "<5g (≈2g Na)",
                                    color = DailyMacrosColors.saltColor,
                                ),
                                MacroProgressItem(
                                    title = "Fibre",
                                    progress = .0f,
                                    progressLabel = "0g",
                                    range = Range(.9091f, .9091f),
                                    rangeLabel = "30-38g",
                                    color = DailyMacrosColors.fibreColor,
                                ),
                            )
                        ),
                        RecordUIModel(
                            recordId = 1L,
                            title = "Title",
                            templateId = 1L,
                            images = listOf("", ""),
                            timestamp = "2022-01-01 00:00:00",
                            macros = MacrosUIModel(
                                calories = "8cal",
                                protein = "prot 8",
                                fat = "fat 4(2)",
                                carbs = "carb 9(9)",
                                salt = "sal 2",
                                fibre = "fib 4",
                            ),
                        ),
                        RecordUIModel(
                            recordId = 2L,
                            title = "Title 2",
                            templateId = 1L,
                            images = listOf("", ""),
                            timestamp = "2022-05-01 00:00:00",
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
            )
        }
    }
}
