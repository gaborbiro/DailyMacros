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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
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

/**
 * Provenance for the nutrient estimate: what the AI read from the photo/title/description.
 * Collapsed, it previews the detected components (falling back to the notes) in at most two faded lines.
 */
@Composable
internal fun AiInferenceSection(
    modifier: Modifier = Modifier,
    nutrientBreakdown: NutrientBreakdownUiModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val fadeSpec = tween<Float>(durationMillis = 400)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (expanded) {
                    it
                } else {
                    it.clickable { onExpandedChange(true) }
                }
            },
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = .45f)),
    ) {
        Column(
            modifier = Modifier
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
                    AiInferencePreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingHalf),
                        text = nutrientBreakdown.componentsSummary ?: nutrientBreakdown.notes.orEmpty(),
                    )
                }
            }

            ExpandToggleRow(
                expanded = expanded,
                onCollapseTapped = { onExpandedChange(false) },
            )
        }
    }
}

@Composable
private fun AiInferencePreview(
    modifier: Modifier,
    text: String,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        modifier = modifier.fadeTruncatedLastLine { layout },
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Clip,
        onTextLayout = { layout = it },
    )
}

/**
 * When the text is cut off, fades out the end of its last visible line instead of showing an ellipsis.
 * The layout is read at draw time, so a new layout only triggers a redraw.
 */
private fun Modifier.fadeTruncatedLastLine(layout: () -> TextLayoutResult?): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val textLayout = layout()?.takeIf { it.hasVisualOverflow && it.lineCount > 0 } ?: return@drawWithContent
        val lastLineTop = textLayout.getLineTop(textLayout.lineCount - 1)
        val fadeWidth = size.width * .4f
        val ltr = layoutDirection == LayoutDirection.Ltr
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = if (ltr) size.width - fadeWidth else fadeWidth,
                endX = if (ltr) size.width else 0f,
            ),
            topLeft = Offset(0f, lastLineTop),
            size = Size(size.width, size.height - lastLineTop),
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
                text = stringResource(R.string.modal_content_ai_notes),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.Underline,
                ),
            )
            Spacer(modifier = Modifier.height(PaddingQuarter))
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
                stringResource(R.string.meal_details_expand)
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
private fun AiInferenceSectionExpandedPreview() {
    ViewPreviewContext {
        AiInferenceSection(
            nutrientBreakdown = previewAiNutrientBreakdown(),
            expanded = true,
            onExpandedChange = {},
        )
    }
}
