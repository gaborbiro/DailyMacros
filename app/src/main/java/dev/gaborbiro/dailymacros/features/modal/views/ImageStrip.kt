package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingHalf
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.design.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.common.views.LocalImage
import dev.gaborbiro.dailymacros.features.common.views.PreviewImageStoreProvider


@Composable
fun ImageStrip(
    images: List<String>,
    showAddPhotoButtons: Boolean,
    onImageTapped: (String) -> Unit,
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 64.dp,
    horizontalPadding: Dp = PaddingDefault,
    itemSpacing: Dp = PaddingHalf,
    onInfoButtonTapped: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = PaddingQuarter),
    ) {
        items(items = images, key = { it }) { name ->
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .clip(shape)
                    .border(1.dp, Color.Black.copy(alpha = 0.06f), shape)
                    .clickable { onImageTapped(name) }
            ) {
                LocalImage(
                    name = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    contentDescription = ""
                )
            }
        }
        if (showAddPhotoButtons) {
            item("add_camera") {
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .clip(shape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f), shape)
                        .clickable(onClick = onAddImageViaCameraTapped)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), shape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_photo),
                        contentDescription = "Take photo",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item("add_picker") {
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .clip(shape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f), shape)
                        .clickable(onClick = onAddImageViaPickerTapped)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), shape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_picture),
                        contentDescription = "Image picker",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                IconButton(onClick = onInfoButtonTapped) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ImageStripPreview() {
    ViewPreviewContext {
        PreviewImageStoreProvider {
            ImageStrip(
                images = listOf("1", "2"),
                showAddPhotoButtons = true,
                onImageTapped = {},
                onAddImageViaCameraTapped = {},
                onAddImageViaPickerTapped = {},
                onInfoButtonTapped = {},
            )
        }
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ImageStripPreviewViewOnly() {
    ViewPreviewContext {
        PreviewImageStoreProvider {
            ImageStrip(
                images = listOf("1", "2"),
                showAddPhotoButtons = false,
                onImageTapped = {},
                onAddImageViaCameraTapped = {},
                onAddImageViaPickerTapped = {},
                onInfoButtonTapped = {},
            )
        }
    }
}
