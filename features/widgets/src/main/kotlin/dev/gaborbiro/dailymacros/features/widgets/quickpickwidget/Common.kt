package dev.gaborbiro.dailymacros.features.widgets.quickpickwidget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle


val OverlayTitleTextStyle: TextStyle
    get() = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        textAlign = TextAlign.Start,
        color = ColorProvider(day = Color.White, night = Color.White),
    )
