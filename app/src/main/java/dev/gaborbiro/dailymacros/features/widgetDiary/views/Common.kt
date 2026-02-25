package dev.gaborbiro.dailymacros.features.widgetDiary.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

val WidgetImageSize = 64.dp

val ListItemImageCornerRadius = 6.dp

val QuickPickBackground
    @Composable get() = GlanceTheme.colors.primaryContainer

val RecordBackground
    @Composable get() = GlanceTheme.colors.widgetBackground

val SectionTitleTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        color = GlanceTheme.colors.onSecondaryContainer
    )

val TitleTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        textAlign = TextAlign.Start,
        color = GlanceTheme.colors.onBackground,
    )

val DescriptionTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        textAlign = TextAlign.Start,
        color = GlanceTheme.colors.onBackground,
    )

val DateTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        textAlign = TextAlign.Start,
        color = GlanceTheme.colors.onBackground,
    )

val LoadingTextStyle: TextStyle
    @Composable get() = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        textAlign = TextAlign.Start,
        color = ColorProvider(day = Color(0xFFFF5722), night = Color(0xFFCDE9FF)),
    )
