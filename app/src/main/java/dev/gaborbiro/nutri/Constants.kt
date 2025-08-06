package dev.gaborbiro.nutri

import android.graphics.Bitmap
import java.time.LocalDateTime

const val ImageFilenameExt = "png"
val ImageFileFormat = Bitmap.CompressFormat.PNG

val generateImageFilename: () -> String = {
    "${LocalDateTime.now()}.$ImageFilenameExt"
}
