package dev.gaborbiro.nutri.design

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


object NotesColors {
    val NotesPunchy = Color(0xFF288B95)

    val BackgroundWhite = Color(0xFFE6E4F1)
    val BackgroundDark = Color(0xFF464646)

    val CardColorLight = Color(0xFFEEEEF0)
    val SurfaceDark = Color(0xFF212222)

    val Notes80 = Color(0xFFBDF6FF)
    val NotesGrey80 = Color(0xFFC1D8DB) // 188 deg
    val NotesHi80 = Color(0xFFB8EFDF) //163 deg

    val Notes40 = Color(0xFF5098A3)
    val NotesGrey40 = Color(0xFF5B6D70)
    val NotesHi40 = Color(0xFF527D71)

    val surfaceVariantLight = Color(0xFFFFFFFF)
    val surfaceVariantDark = Color(0xFF1C1C1E)
}


private val DarkColorScheme = darkColorScheme(
    primary = NotesColors.Notes80,
    secondary = NotesColors.NotesGrey80,
    tertiary = NotesColors.NotesHi80,
    surfaceVariant = NotesColors.surfaceVariantDark,

    background = NotesColors.BackgroundDark,

    surface = NotesColors.SurfaceDark,

    onSecondaryContainer = Color.White, // tab bar icon
    secondaryContainer = NotesColors.BackgroundDark, // tab bar button container
)


private val LightColorScheme = lightColorScheme(
    primary = NotesColors.Notes40,
    secondary = NotesColors.NotesGrey40,
    tertiary = NotesColors.NotesHi40,

    surfaceVariant = NotesColors.surfaceVariantLight,

    background = NotesColors.BackgroundWhite,

    surface = NotesColors.CardColorLight,

    onSecondaryContainer = Color.White, // tab bar icon
    secondaryContainer = NotesColors.BackgroundWhite, // tab bar button container
)

@Composable
fun NotesTheme(
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

object NotesGlanceColorScheme {

    val colors: ColorProviders
        @Composable get() {
            return ColorProviders(
                light = LightColorScheme,
                dark = DarkColorScheme,
            )
        }
}
