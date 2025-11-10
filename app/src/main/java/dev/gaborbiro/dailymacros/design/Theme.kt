package dev.gaborbiro.dailymacros.design

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import dev.gaborbiro.dailymacros.features.common.StatusBarOverlay


val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB74D),    // Softer orange (resonates with your calorieColor)
    secondary = Color(0xFFCE93D8),  // Muted purple for dark mode
    tertiary = Color(0xFF4DD0E1),   // Brighter cyan
    background = Color(0xFF121212), // Standard dark background
    surface = Color(0xFF2E2E2E),    // Slightly lighter surface
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
)

val LightColors = lightColorScheme(
    primary = Color(0xFFFB8C00),    // Vibrant orange → energetic, active
    secondary = Color(0xFF7B1FA2),  // Purple → contrast, playful accent
    tertiary = Color(0xFF00ACC1),   // Cyan → lively highlight
    background = Color(0xFFFFFFFF), // Crisp white
    surface = Color(0xFFF0F0F0),    // Very light neutral
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
)


@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    val dayNightExtraColorScheme = if (darkTheme) {
        darkExtraColorScheme
    } else {
        lightExtraColorScheme
    }

    CompositionLocalProvider(LocalExtraColorScheme provides dayNightExtraColorScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                content()
                StatusBarOverlay()
            },
        )
    }
}

object WidgetColorScheme {

    @Composable
    fun colors(
        context: Context? = null,
        dynamicColor: Boolean = false,
    ): ColorProviders {
        val (light, dark) = when {
            dynamicColor -> {
                context
                    ?.let {
                        dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
                    }
                    ?: run {
                        LightColors to DarkColors
                    }
            }

            else -> LightColors to DarkColors
        }

        return ColorProviders(
            light = light,
            dark = dark,
        )
    }
}
