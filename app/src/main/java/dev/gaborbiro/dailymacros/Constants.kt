package dev.gaborbiro.dailymacros

import android.graphics.Bitmap
import java.time.LocalDateTime

const val FoodPicExt = "jpg"
val FoodPicFormat = Bitmap.CompressFormat.JPEG
const val FoodPicMaxSize = 2560
const val FoodPicQuality = 86 // 100 max. PNG ignores this setting.

val generateFoodPicFilename: () -> String = {
    "${LocalDateTime.now()}.$FoodPicExt"
}
