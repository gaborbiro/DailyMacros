package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.modal.model.NutrientBreakdownUiModel
import dev.gaborbiro.dailymacros.features.common.R as CommonR

internal val NutrientBreakdownUiModel.hasAiInference: Boolean
    get() = !notes.isNullOrBlank() || components.isNotEmpty()

private const val PreviewMaxLines = 2

/**
 * Provenance for the nutrient estimate: what the AI read from the photo/title/description.
 * Collapsed, it previews the detected components (falling back to the notes) in at most two lines, fading out
 * the bottom of the last line when there is more. It only offers to expand when the preview hides something.
 */
@Composable
internal fun AiInferenceSection(
    modifier: Modifier = Modifier,
    nutrientBreakdown: NutrientBreakdownUiModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val fadeSpec = tween<Float>(durationMillis = 400)
    val previewText = nutrientBreakdown.componentsSummary ?: nutrientBreakdown.notes.orEmpty()
    val previewStyle = MaterialTheme.typography.bodyMedium
    val textMeasurer = rememberTextMeasurer()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = .45f)),
    ) {
        BoxWithConstraints {
            // Measured up front (rather than via onTextLayout) so the expand affordance is right on the first frame.
            val previewWidthPx = with(LocalDensity.current) { (maxWidth - PaddingHalf * 2).roundToPx() }.coerceAtLeast(0)
            val previewLayout = remember(previewText, previewStyle, previewWidthPx) {
                textMeasurer.measure(
                    text = previewText,
                    style = previewStyle,
                    overflow = TextOverflow.Clip,
                    maxLines = PreviewMaxLines,
                    constraints = Constraints(maxWidth = previewWidthPx),
                )
            }
            // Notes aren't part of the preview while it shows the components, so they'd be unreachable otherwise.
            val notesHiddenByPreview = nutrientBreakdown.componentsSummary != null && !nutrientBreakdown.notes.isNullOrBlank()
            val expandable = previewLayout.hasVisualOverflow || notesHiddenByPreview

            Column(
                modifier = Modifier
                    .let {
                        if (!expanded && expandable) {
                            it.clickable { onExpandedChange(true) }
                        } else {
                            it
                        }
                    }
                    .padding(top = PaddingHalf)
                    .animateContentSize(animationSpec = tween(durationMillis = 280)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingHalf),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.ic_ai_borg),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = PaddingHalf)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.record_details_ai_inferred_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
                    },
                    label = "mealDetailsAiInferenceExpand",
                ) { isExpanded ->
                    if (isExpanded) {
                        AiInferenceDetails(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaddingHalf),
                            nutrientBreakdown = nutrientBreakdown,
                        )
                    } else {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaddingHalf)
                                .let {
                                    if (previewLayout.hasVisualOverflow) it.fadeLastLineBottom(previewLayout) else it
                                },
                            text = previewText,
                            style = previewStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = PreviewMaxLines,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }

                if (expanded || expandable) {
                    ExpandToggleRow(
                        expanded = expanded,
                        expandLabel = stringResource(R.string.record_details_ai_inferred_show_all),
                        onCollapseTapped = { onExpandedChange(false) },
                    )
                }
            }
        }
    }
}

/**
 * Fades out the bottom quarter of the last visible line, hinting that the text continues.
 */
private fun Modifier.fadeLastLineBottom(layout: TextLayoutResult): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val lastLine = layout.lineCount - 1
        if (lastLine < 0) return@drawWithContent
        val lineTop = layout.getLineTop(lastLine)
        val lineBottom = layout.getLineBottom(lastLine)
        val fadeTop = lineBottom - (lineBottom - lineTop) * .25f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = fadeTop,
                endY = lineBottom,
            ),
            topLeft = Offset(0f, fadeTop),
            size = Size(size.width, size.height - fadeTop),
            blendMode = BlendMode.DstOut,
        )
    }

@Composable
private fun AiInferenceDetails(
    modifier: Modifier,
    nutrientBreakdown: NutrientBreakdownUiModel,
) {
    Column(modifier = modifier) {
        if (nutrientBreakdown.components.isNotEmpty()) {
            Text(
                text = stringResource(R.string.modal_content_components),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.Underline,
                ),
            )
            Spacer(modifier = Modifier.height(PaddingQuarter))
            nutrientBreakdown.components.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        nutrientBreakdown.notes?.takeIf { it.isNotBlank() }?.let {
            if (nutrientBreakdown.components.isNotEmpty()) {
                Spacer(modifier = Modifier.height(PaddingDefault))
            }
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * "More details ⌄" / "Collapse ⌃" footer shared by the meal details cards. While collapsed, taps are
 * handled by the enclosing card, so this row only reacts once expanded.
 */
@Composable
internal fun ExpandToggleRow(
    expanded: Boolean,
    onCollapseTapped: () -> Unit,
    expandLabel: String = stringResource(R.string.meal_details_expand),
) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .fillMaxWidth()
            .let {
                if (expanded) {
                    it.clickable { onCollapseTapped() }
                } else {
                    it
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (expanded) {
                stringResource(R.string.meal_details_collapse)
            } else {
                expandLabel
            },
            style = MaterialTheme.typography.labelLarge,
        )

        Spacer(
            modifier = Modifier
                .size(6.dp)
        )

        Icon(
            imageVector = if (expanded) {
                Icons.Filled.KeyboardArrowUp
            } else {
                Icons.Filled.KeyboardArrowDown
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun previewAiNutrientBreakdown() = NutrientBreakdownUiModel(
    calories = "Calories: 170 kcal",
    protein = null,
    fat = null,
    ofWhichSaturated = null,
    carbs = null,
    ofWhichSugar = null,
    ofWhichAddedSugar = null,
    salt = null,
    fibre = null,
    notes = "Assumed white bread with a thin spread of butter; marmalade amount estimated from the photo.",
    components = listOf("- 1 slice white bread", "- ~5g butter (?)", "- ~1 tbsp orange marmalade (??)"),
    componentsSummary = "1 slice white bread · ~5g butter (?) · ~1 tbsp orange marmalade (??)",
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AiInferenceSectionCollapsedPreview() {
    ViewPreviewContext {
        AiInferenceSection(
            nutrientBreakdown = previewAiNutrientBreakdown(),
            expanded = false,
            onExpandedChange = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AiInferenceSectionFitsPreview() {
    ViewPreviewContext {
        AiInferenceSection(
            nutrientBreakdown = previewAiNutrientBreakdown().copy(
                notes = null,
                components = listOf("- 1 slice white bread", "- ~1 tbsp orange marmalade (?)"),
                componentsSummary = "1 slice white bread · ~1 tbsp orange marmalade (?)",
            ),
            expanded = false,
            onExpandedChange = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AiInferenceSectionExpandedPreview() {
    ViewPreviewContext {
        AiInferenceSection(
            nutrientBreakdown = previewAiNutrientBreakdown(),
            expanded = true,
            onExpandedChange = {},
        )
    }
}
