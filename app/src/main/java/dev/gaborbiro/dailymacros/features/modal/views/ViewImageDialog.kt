package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.features.common.views.LocalImageStore
import dev.gaborbiro.dailymacros.features.common.views.PreviewContext
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import kotlin.math.roundToInt

@Composable
fun ImageDialog(
    dialogHandle: DialogHandle.ViewImageDialog,
    onDismissRequested: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequested,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val images = dialogHandle.images
        if (images.size == 1) {
            LazyZoomableImage(
                imageName = images[0],
                contentDescription = "Image: ${dialogHandle.title}",
            )
        } else {
            val pagerState = rememberPagerState(
                initialPage = dialogHandle.initialPage,
                pageCount = { images.size },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    LazyZoomableImage(
                        imageName = images[page],
                        contentDescription = "Image ${page + 1} of ${images.size}: ${dialogHandle.title}",
                    )
                }
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd),
                    onClick = onDismissRequested,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(64.dp),
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        tint = Color.White,
                        contentDescription = "Close",
                    )
                }
                PagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun PagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pagerState.pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            Box(
                modifier = Modifier
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun LazyZoomableImage(
    imageName: String,
    contentDescription: String,
) {
    val store = LocalImageStore.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = imageName) {
        value = try {
            store.read(imageName, thumbnail = false)
        } catch (_: Throwable) {
            null
        }
    }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    if (imageBitmap != null) {
        ZoomableImage(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
        )
    } else {
        Surface(
            modifier = Modifier
                .padding(PaddingDefault),
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    bitmap: ImageBitmap,
    contentDescription: String,
) {
    var scale by remember { mutableFloatStateOf(1f) }
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
            .clip(RoundedCornerShape(4.dp))
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
        IntSize(
            width = container.width,
            height = (container.width / imageRatio).roundToInt()
        )
    } else {
        IntSize(
            width = (container.height * imageRatio).roundToInt(),
            height = container.height
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ViewImageDialogPreview() {
    PreviewContext {
        ImageDialog(
            dialogHandle = DialogHandle.ViewImageDialog(
                title = "",
                images = listOf("1", "2", "3", "4"),
            ),
            onDismissRequested = {},
        )
    }
}