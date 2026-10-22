package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
 * How much of the last preview line (from its bottom up) fades out. Text sits in the upper ~80% of its line box,
 * so anything much below 1 only fades the descenders and is barely noticeable.
 */
private const val PreviewLastLineFadeFraction = 1f

/**
 * Provenance for the nutrient estimate: what the AI read from the photo/title/description.
 * Collapsed, it previews the detected components (falling back to the notes) in at most two lines, fading out
 * the bottom of the last line when there is more. It only offers to expand (tap the preview; a chevron in the
 * caption hints at it and turns over once expanded) when the preview hides something. No card or border: it reads as a caption to the macros.
 *
 * [correctionInput], when given, closes the card: the place to tell the AI what it got wrong, right after
 * reading what it inferred. It replaces the footnote that otherwise points the user at the description.
 */
@Composable
internal fun AiInferenceSection(
    modifier: Modifier = Modifier,
    nutrientBreakdown: NutrientBreakdownUiModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    correctionInput: (@Composable () -> Unit)? = null,
) {
    val showFootnote = correctionInput == null
    val fadeSpec = tween<Float>(durationMillis = 400)
    val previewText = nutrientBreakdown.componentsSummary ?: nutrientBreakdown.notes.orEmpty()
    val previewStyle = MaterialTheme.typography.bodyMedium
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Measured up front (rather than via onTextLayout) so the expand affordance is right on the first frame.
        // Mirrors the preview's horizontal padding, rounded the same way the padding modifier rounds it.
        val previewWidthPx = (constraints.maxWidth - 2 * with(LocalDensity.current) { PaddingDefault.roundToPx() })
            .coerceAtLeast(0)
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

        Column {
            Column(
                modifier = Modifier
                    .let {
                        if (!expanded && expandable) {
                            it.clickable { onExpandedChange(true) }
                        } else {
                            it
                        }
                    }
                    .animateContentSize(animationSpec = tween(durationMillis = 280)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let {
                            if (expanded) it.clickable { onExpandedChange(false) } else it
                        }
                        .padding(horizontal = PaddingDefault),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.ic_ai_borg),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = PaddingQuarter)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.record_details_ai_inferred_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (expanded || expandable) {
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            animationSpec = tween(durationMillis = 280),
                            label = "mealDetailsAiInferenceChevron",
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) {
                                stringResource(R.string.meal_details_collapse)
                            } else {
                                stringResource(R.string.record_details_ai_inferred_show_all)
                            },
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation),
                            tint = ExpandAffordanceColor,
                        )
                    }
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
                                .padding(horizontal = PaddingDefault, vertical = PaddingHalf),
                            nutrientBreakdown = nutrientBreakdown,
                            showFootnote = showFootnote,
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaddingDefault, vertical = PaddingHalf),
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .let {
                                        if (previewLayout.hasVisualOverflow) it.fadeLastLineBottom(previewLayout) else it
                                    },
                                text = previewText,
                                style = previewStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = PreviewMaxLines,
                                overflow = TextOverflow.Clip,
                            )
                            // Nothing more to expand into, so the footnote belongs here.
                            if (!expandable && showFootnote) {
                                Spacer(modifier = Modifier.height(PaddingDefault))
                                MealDetailsFootnote(text = stringResource(R.string.record_details_ai_inferred_footnote))
                            }
                        }
                    }
                }

                if (expanded) {
                    ExpandToggleRow(
                        expanded = true,
                        onCollapseTapped = { onExpandedChange(false) },
                    )
                }
            }
            // Outside the collapsed preview's tap target, so typing here doesn't expand it.
            correctionInput?.invoke()
        }
    }
}

/**
 * Fades out the last visible line towards its bottom (see [PreviewLastLineFadeFraction]), hinting that the text
 * continues.
 */
private fun Modifier.fadeLastLineBottom(layout: TextLayoutResult): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val lastLine = layout.lineCount - 1
        if (lastLine < 0) return@drawWithContent
        val lineTop = layout.getLineTop(lastLine)
        val lineBottom = layout.getLineBottom(lastLine)
        val fadeTop = lineBottom - (lineBottom - lineTop) * PreviewLastLineFadeFraction
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
    showFootnote: Boolean,
) {
    Column(modifier = modifier) {
        // The caption above already says what this is, so the list goes without a heading.
        nutrientBreakdown.components.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        nutrientBreakdown.notes?.takeIf { it.isNotBlank() }?.let {
            if (nutrientBreakdown.components.isNotEmpty()) {
                Spacer(modifier = Modifier.height(PaddingDefault))
            }
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showFootnote) {
            Spacer(modifier = Modifier.height(PaddingDefault))
            MealDetailsFootnote(text = stringResource(R.string.record_details_ai_inferred_footnote))
        }
    }
}

/**
 * Small italic note closing a meal details card.
 */
@Composable
internal fun MealDetailsFootnote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
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
            color = ExpandAffordanceColor,
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
            tint = ExpandAffordanceColor,
        )
    }
}

/**
 * Expand/collapse controls are secondary to what they reveal, so they share one muted colour (and stay out of the
 * primary colour, which is kept for the actions that matter: correcting the AI and Done).
 */
private val ExpandAffordanceColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private fun previewAiNutrientBreakdown() = NutrientBreakdownUiModel(
    nutrients = previewNutrientRows(),
    notes = "Assumed white bread with a thin spread of butter; marmalade amount estimated from the photo.",
    components = listOf("- 1 slice white bread", "- ~5g butter (likely)", "- ~1 tbsp orange marmalade (guess)"),
    componentsSummary = "1 slice white bread · ~5g butter (likely) · ~1 tbsp orange marmalade (guess)",
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
                components = listOf("- 1 slice white bread", "- ~1 tbsp orange marmalade (likely)"),
                componentsSummary = "1 slice white bread · ~1 tbsp orange marmalade (likely)",
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
