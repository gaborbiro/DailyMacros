package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultHorizontal
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetDefaultVertical
import dev.gaborbiro.dailymacros.features.widget.PaddingWidgetHalfVertical
import dev.gaborbiro.dailymacros.features.widget.util.WidgetPreview

@Composable
fun ListItemTop10(
    modifier: GlanceModifier = GlanceModifier,
    showQuickAddTooltip: Boolean,
    dismissAction: Action,
) {
    Column(
        modifier = modifier
            .background(sectionTitleBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = GlanceModifier
                .padding(vertical = PaddingWidgetDefaultVertical)
                .fillMaxWidth(),
            text = "Top 10",
            style = sectionTitleTextStyle,
        )
        println("showQuickAddTooltip: $showQuickAddTooltip")
        if (showQuickAddTooltip) {
            Row(
                modifier = GlanceModifier
                    .padding(horizontal = PaddingWidgetDefaultHorizontal)
                    .padding(bottom = PaddingWidgetDefaultVertical)
                    .background(sectionTitleBackground),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier
                        .background(Color.LightGray)
                        .cornerRadius(ListItemImageCornerRadius)
                        .padding(vertical = PaddingWidgetHalfVertical),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(GlanceModifier.width(PaddingWidgetDefaultHorizontal))

                    Image(
                        modifier = GlanceModifier
                            .size(18.dp),
                        provider = ImageProvider(R.drawable.ic_info),
                        contentDescription = "Info",
                        colorFilter = ColorFilter.tint(
                            ColorProvider(
                                day = Color.Blue,
                                night = Color.Blue,
                            )
                        ),
                    )

                    Spacer(GlanceModifier.width(PaddingWidgetDefaultHorizontal))

                    Text(
                        modifier = GlanceModifier
                            .defaultWeight(),
                        text = "Tap on a Top 10 entry to quickly add it again",
                        style = TextStyle(
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start,
                            color = ColorProvider(
                                day = Color.Black,
                                night = Color.Black,
                            ),
                        ),
                    )

                    Spacer(GlanceModifier.width(PaddingWidgetDefaultHorizontal))

                    Image(
                        modifier = GlanceModifier
                            .size(30.dp)
                            .padding(horizontal = 4.dp)
                            .clickable(dismissAction),
                        provider = ImageProvider(R.drawable.ic_close),
                        contentDescription = "Dismiss",
                        colorFilter = ColorFilter.tint(
                            ColorProvider(
                                day = Color.Black,
                                night = Color.Black,
                            )
                        )
                    )
                }
            }
        }
    }
}


@Preview(widthDp = 256)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun SectionTitlePreview() {
    WidgetPreview {
        ListItemTop10(
            showQuickAddTooltip = true,
            dismissAction = action {},
        )
    }
}

@Preview(widthDp = 256)
@Composable
@OptIn(ExperimentalGlancePreviewApi::class)
private fun SectionTitlePreview2() {
    WidgetPreview {
        ListItemTop10(
            showQuickAddTooltip = false,
            dismissAction = action {},
        )
    }
}
