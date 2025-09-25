package dev.gaborbiro.dailymacros.features.widgetCamera

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.WidgetColorScheme

class CameraWidgetScreen : GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme(colors = WidgetColorScheme.colors(context)) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_add_photo),
                        contentDescription = "Add photo",
                    )
                }
            }
        }
    }
}
