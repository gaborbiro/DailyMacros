package dev.gaborbiro.dailymacros.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders


object DailyMacrosColors {
    val BackgroundWhite = Color(0xFFE6E4F1)
    val BackgroundDark = Color(0xFF464646)

    val CardColorLight = Color(0xFFEEEEF0)
    val SurfaceDark = Color(0xFF212222)

    val DailyMacros80 = Color(0xFFBDF6FF)
    val DailyMacrosGrey80 = Color(0xFFC1D8DB) // 188 deg
    val DailyMacrosHi80 = Color(0xFFB8EFDF) //163 deg

    val DailyMacros40 = Color(0xFF5098A3)
    val NDailyMacrosGrey40 = Color(0xFF5B6D70)
    val DailyMacrosHi40 = Color(0xFF527D71)

    val surfaceVariantLight = Color(0xFFFFFFFF)
    val surfaceVariantDark = Color(0xFF1C1C1E)
}


private val DarkColorScheme = darkColorScheme(
    primary = DailyMacrosColors.DailyMacros80,
    secondary = DailyMacrosColors.DailyMacrosGrey80,
    tertiary = DailyMacrosColors.DailyMacrosHi80,
    surfaceVariant = DailyMacrosColors.surfaceVariantDark,

    background = DailyMacrosColors.BackgroundDark,

    surface = DailyMacrosColors.SurfaceDark,

    onSecondaryContainer = Color.White, // tab bar icon
    secondaryContainer = DailyMacrosColors.BackgroundDark, // tab bar button container
)


private val LightColorScheme = lightColorScheme(
    primary = DailyMacrosColors.DailyMacros40,
    secondary = DailyMacrosColors.NDailyMacrosGrey40,
    tertiary = DailyMacrosColors.DailyMacrosHi40,

    surfaceVariant = DailyMacrosColors.surfaceVariantLight,

    background = DailyMacrosColors.BackgroundWhite,

    surface = DailyMacrosColors.CardColorLight,

    onSecondaryContainer = Color.White, // tab bar icon
    secondaryContainer = DailyMacrosColors.BackgroundWhite, // tab bar button container
)

@Composable
fun DailyMacrosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

object DailyMacrosGlanceColorScheme {

    val colors: ColorProviders
        @Composable get() {
            return ColorProviders(
                light = LightColorScheme,
                dark = DarkColorScheme,
            )
        }
}
