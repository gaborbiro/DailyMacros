package dev.gaborbiro.dailymacros

import android.graphics.Bitmap
import java.time.LocalDateTime

const val DefaultFoodPicExt = "jpg"
val DefaultFoodPicFormat = Bitmap.CompressFormat.JPEG
const val FoodPicMaxSize = 2560
const val DefaultFoodPicQuality = 86 // 100 max. PNG ignores this setting.

val generateFoodPicFilename: () -> String = {
    "${LocalDateTime.now()}.$DefaultFoodPicExt"
}
