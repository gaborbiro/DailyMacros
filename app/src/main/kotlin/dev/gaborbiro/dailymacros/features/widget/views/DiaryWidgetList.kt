package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.gaborbiro.dailymacros.design.PaddingWidgetDefault
import dev.gaborbiro.dailymacros.design.PaddingWidgetDouble
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.widget.model.ListUiModelQuickPick
import dev.gaborbiro.dailymacros.features.widget.model.ListUiModelQuickPickFooter
import dev.gaborbiro.dailymacros.features.widget.model.ListUiModelQuickPickHeader
import dev.gaborbiro.dailymacros.features.common.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.features.common.model.NutrientsUiModel
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreviewContext

@Composable
internal fun DiaryWidgetList(
    items: List<ListUiModelBase>,
    recordImageTapActionProvider: @Composable (recordId: Long) -> Action,
    recordBodyTapActionProvider: @Composable (recordId: Long) -> Action,
    quickPickImageTapActionProvider: @Composable (templateId: Long) -> Action,
    quickPickBodyTapActionProvider: @Composable (templateId: Long) -> Action,
) {
    LazyColumn(
        modifier = GlanceModifier
            .fillMaxSize(),
    ) {
        item { Spacer(modifier = GlanceModifier.height(PaddingWidgetDefault)) }
        itemsIndexed(
            items = items,
            itemId = { _, item -> item.listItemId },
        ) { _, item ->
            when (item) {
                is ListUiModelRecord -> ListItemRecord(
                    modifier = GlanceModifier
                        .padding(start = PaddingWidgetDouble)
                        .padding(vertical = PaddingWidgetDefault),
                    record = item,
                    imageTappedActionProvider = recordImageTapActionProvider(item.listItemId),
                    bodyTappedActionProvider = recordBodyTapActionProvider(item.listItemId),
                )

                is ListUiModelQuickPickHeader -> ListItemQuickPickHeader(
                    modifier = GlanceModifier
                        .padding(top = PaddingWidgetDefault)
                )

                is ListUiModelQuickPick -> ListItemQuickPick(
                    modifier = GlanceModifier
                        .padding(horizontal = PaddingWidgetDouble, vertical = PaddingWidgetDefault),
                    quickPickEntry = item,
                    imageTapActionProvider = quickPickImageTapActionProvider(item.templateId),
                    bodyTapActionProvider = quickPickBodyTapActionProvider(item.templateId),
                )

                is ListUiModelQuickPickFooter -> Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingWidgetDefault)
                ) {
                    Spacer(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(QuickPickBackground)
                            .height(PaddingWidgetDefault)
                    )
                }
            }
        }
        item {
            Spacer(
                modifier = GlanceModifier
                    .height(58.dp)
            )
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun WidgetListPreview() {
    WidgetPreviewContext {
        DiaryWidgetList(
            items = listOf(
                ListUiModelRecord(
                    recordId = 1,
                    templateId = 1L,
                    title = "Breakfast",
                    timestamp = "Yesterday",
                    images = listOf("", ""),
                    nutrients = NutrientsUiModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                    showLoadingIndicator = false,
                    showAddToQuickPicksMenuItem = true,
                ),
                ListUiModelQuickPickHeader,
                ListUiModelQuickPick(
                    templateId = 1,
                    title = "Breakfast",
                    images = listOf("", ""),
                    nutrients = NutrientsUiModel(
                        calories = "1008cal",
                        protein = "protein 8",
                        fat = "fat 4(2)",
                        carbs = "carbs 9(9)",
                        salt = "salt 2",
                        fibre = "fibre 4",
                    ),
                ),
                ListUiModelQuickPick(
                    templateId = 2,
                    title = "Lunch",
                    images = listOf("", ""),
                    nutrients = NutrientsUiModel(
                        calories = "1008cal",
                        protein = "protein 8",
                        fat = "fat 4(2)",
                        carbs = "carbs 9(9)",
                        salt = "salt 2",
                        fibre = "fibre 4",
                    ),
                ),
                ListUiModelQuickPick(
                    templateId = 3,
                    title = "Dinner",
                    images = listOf("", ""),
                    nutrients = NutrientsUiModel(
                        calories = "1008cal",
                        protein = "protein 8",
                        fat = "fat 4(2)",
                        carbs = "carbs 9(9)",
                        salt = "salt 2",
                        fibre = "fibre 4",
                    ),
                ),
                ListUiModelQuickPickFooter,
                ListUiModelRecord(
                    recordId = 2L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Lunch",
                    images = listOf("", ""),
                    nutrients = NutrientsUiModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                    showLoadingIndicator = false,
                    showAddToQuickPicksMenuItem = true,
                ),
                ListUiModelRecord(
                    recordId = 3L,
                    templateId = 1L,
                    timestamp = "Yesterday",
                    title = "Dinner",
                    images = listOf("", ""),
                    nutrients = NutrientsUiModel(
                        calories = "8cal",
                        protein = "prot 8",
                        fat = "fat 4(2)",
                        carbs = "carb 9(9)",
                        salt = "sal 2",
                        fibre = "fib 4",
                    ),
                    showLoadingIndicator = false,
                    showAddToQuickPicksMenuItem = true,
                ),
            ),
            recordImageTapActionProvider = { action {} },
            recordBodyTapActionProvider = { action {} },
            quickPickImageTapActionProvider = { action {} },
            quickPickBodyTapActionProvider = { action {} },
        )
    }
}
