package dev.gaborbiro.nutri.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.DisplayMetrics
import android.view.Surface
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.createBitmap
import dev.gaborbiro.nutri.App
import kotlin.math.pow
import kotlin.random.Random


fun correctBitmap(
    currentScreenRotation: Int,
    bitmap: Bitmap,
    correctRotation: Boolean,
    scaleDown: Boolean,
): Bitmap {
    val rotateAngle = if (correctRotation) {
        when (currentScreenRotation) {
            Surface.ROTATION_0 -> 0f
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> 90f
            else -> 0f
        }
    } else {
        0f
    }
    return modifyImage(
        bitmap,
        rotateAngle,
        if (scaleDown) 640 else null
    )
}

fun modifyImage(source: Bitmap, rotateAngle: Float, maxWidthPx: Int?): Bitmap {
    val matrix = Matrix()
    if (rotateAngle != 0f) {
        matrix.postRotate(rotateAngle)
    }
    if (maxWidthPx != null) {
        val scale = maxWidthPx / source.width.toFloat()
        if (scale < 1f) {
            matrix.postScale(scale, scale)
        }
    }
    return Bitmap.createBitmap(
        source,
        0,
        0,
        source.width,
        source.height,
        matrix,
        true
    )
}

fun Dp.px(): Float {
    return App.appContext.dpToPixel(this.value)
}

fun Context.dpToPixel(dp: Float): Float {
    val metrics: DisplayMetrics = this.resources.displayMetrics
    return dp * (metrics.densityDpi / 160f)
}

fun createDummyBitmap(
    color: Int = android.graphics.Color.rgb(
        Random.nextInt(256),
        Random.nextInt(256),
        Random.nextInt(256)
    ),
): Bitmap {
    return createBitmap(200, 200).apply {
        val canvas = Canvas(this)
        val paint = Paint().apply {
            this.isAntiAlias = true
            this.color = color
            this.style = Paint.Style.FILL
        }
        // draw a background circle and some text
        canvas.drawCircle(100f, 100f, 90f, paint)
        paint.color = bestContrastingTextColor(color)
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Fud", 100f, 115f, paint)
    }
}

/**
 * Returns either black or white, whichever has higher contrast against [backgroundColor].
 * Optionally enforces a minimum contrast ratio (default 4.5 for normal text). If neither
 * black nor white meets the threshold, returns the one with the higher ratio anyway.
 */
fun bestContrastingTextColor(
    backgroundColor: Int,
    minContrastRatio: Double = 4.5
): Int {
    val white = Color.WHITE
    val black = Color.BLACK

    val contrastWithWhite = contrastRatio(backgroundColor, white)
    val contrastWithBlack = contrastRatio(backgroundColor, black)

    return when {
        contrastWithWhite >= minContrastRatio || contrastWithBlack >= minContrastRatio -> {
            if (contrastWithWhite >= contrastWithBlack) white else black
        }
        else -> {
            // Neither meets the threshold: pick the better one anyway.
            if (contrastWithWhite >= contrastWithBlack) white else black
        }
    }
}

/**
 * Calculates the contrast ratio between two colors per WCAG 2.1:
 * (L1 + 0.05) / (L2 + 0.05) where L1 is the lighter relative luminance.
 */
fun contrastRatio(color1: Int, color2: Int): Double {
    val lum1 = relativeLuminance(color1)
    val lum2 = relativeLuminance(color2)
    val lighter = maxOf(lum1, lum2)
    val darker = minOf(lum1, lum2)
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Computes relative luminance of a color in sRGB space per WCAG definition.
 */
fun relativeLuminance(color: Int): Double {
    fun channel(c: Int): Double {
        val srgb = c / 255.0
        return if (srgb <= 0.03928) {
            srgb / 12.92
        } else {
            ((srgb + 0.055) / 1.055).pow(2.4)
        }
    }
    val r = channel(Color.red(color))
    val g = channel(Color.green(color))
    val b = channel(Color.blue(color))
    // coefficients from WCAG
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
