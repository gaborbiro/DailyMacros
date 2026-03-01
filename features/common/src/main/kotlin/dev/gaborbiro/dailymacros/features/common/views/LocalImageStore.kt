package dev.gaborbiro.dailymacros.features.common.views

import androidx.compose.runtime.staticCompositionLocalOf
import dev.gaborbiro.dailymacros.data.image.domain.ImageStore

val LocalImageStore = staticCompositionLocalOf<ImageStore> {
    error("LocalImageStore not provided")
}
