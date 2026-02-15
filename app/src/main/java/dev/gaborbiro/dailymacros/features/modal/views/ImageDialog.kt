package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.modal.model.DialogState
import kotlin.math.roundToInt

@Composable
fun ImageDialog(
    dialogState: DialogState.ViewImageDialog,
    onDismissRequested: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequested,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        ZoomableImage(
            bitmap = dialogState.bitmap.asImageBitmap(),
            contentDescription = "Image: ${dialogState.title}",
        )
    }
}

@Composable
private fun ZoomableImage(
    bitmap: ImageBitmap,
    contentDescription: String,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val imageSize = remember(bitmap) {
        IntSize(bitmap.width, bitmap.height)
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)

        val fitted = fittedImageSize(imageSize, containerSize)

        val scaledWidth = fitted.width * newScale
        val scaledHeight = fitted.height * newScale

        val maxX = ((scaledWidth - containerSize.width) / 2f).coerceAtLeast(0f)
        val maxY = ((scaledHeight - containerSize.height) / 2f).coerceAtLeast(0f)

        val adjustedPan = panChange * newScale

        val newOffset =
            if (maxX == 0f && maxY == 0f) {
                Offset.Zero
            } else {
                Offset(
                    x = (offset.x + adjustedPan.x).coerceIn(-maxX, maxX),
                    y = (offset.y + adjustedPan.y).coerceIn(-maxY, maxY),
                )
            }

        scale = newScale
        offset = newOffset
    }

    Surface(
        modifier = Modifier
            .padding(PaddingDefault)
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { containerSize = it },
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(transformableState)
        )
    }
}

private fun fittedImageSize(
    image: IntSize,
    container: IntSize,
): IntSize {
    val imageRatio = image.width.toFloat() / image.height
    val containerRatio = container.width.toFloat() / container.height

    return if (imageRatio > containerRatio) {
        // constrained by width
        IntSize(
            width = container.width,
            height = (container.width / imageRatio).roundToInt()
        )
    } else {
        // constrained by height
        IntSize(
            width = (container.height * imageRatio).roundToInt(),
            height = container.height
        )
    }
}
