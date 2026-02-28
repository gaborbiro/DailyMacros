package dev.gaborbiro.dailymacros.features.settings.util

import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.ui.unit.Dp

internal fun Dp.px(context: Context): Float {
    return context.dpToPixel(this.value)
}

private fun Context.dpToPixel(dp: Float): Float {
    val metrics: DisplayMetrics = this.resources.displayMetrics
    return dp * (metrics.densityDpi / 160f)
}
