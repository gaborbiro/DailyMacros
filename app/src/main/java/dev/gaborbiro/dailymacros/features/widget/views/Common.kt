package dev.gaborbiro.dailymacros.features.widget.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

val WidgetImageSize = 64.dp

val WidgetTemplateImageSize = 48.dp

val ListItemImageCornerRadius = 6.dp

val sectionTitleBackground
    @Composable get() = GlanceTheme.colors.secondaryContainer

val sectionTitleTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        color = GlanceTheme.colors.onSecondaryContainer
    )

val titleTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        textAlign = TextAlign.Start,
        color = GlanceTheme.colors.onBackground,
    )

val descriptionTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        textAlign = TextAlign.Start,
        color = GlanceTheme.colors.onBackground,
    )

val dateTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        textAlign = TextAlign.Start,
        color = GlanceTheme.colors.onBackground,
    )
