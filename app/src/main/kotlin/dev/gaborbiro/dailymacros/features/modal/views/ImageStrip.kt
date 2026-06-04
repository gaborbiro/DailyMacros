package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.common.utils.horizontalScrollWithBar
import dev.gaborbiro.dailymacros.features.common.views.LocalImage


@Composable
fun ImageStrip(
    images: List<String>,
    showAddPhotoButtons: Boolean,
    showImageDeleteButton: Boolean = true,
    showImageReorderButtons: Boolean = false,
    showInfoButton: Boolean = false,
    onImageTapped: (String) -> Unit,
    onImageDeleteTapped: (String) -> Unit,
    onImageMoveLeftTapped: (String) -> Unit = {},
    onImageMoveRightTapped: (String) -> Unit = {},
    onAddImageViaCameraTapped: () -> Unit,
    onAddImageViaPickerTapped: () -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 64.dp,
    onInfoButtonTapped: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val imageControlColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = Color.Gray.copy(alpha = .8f),
        contentColor = Color.White,
        disabledContainerColor = Color.Gray.copy(alpha = .4f),
        disabledContentColor = Color.White.copy(alpha = .5f),
    )
    val imageDeleteColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = Color.Gray.copy(alpha = .8f),
        contentColor = Color.Red,
    )

    Row(
        modifier = modifier
            .horizontalScrollWithBar(
                startPadding = PaddingDefault,
                autoFade = false,
            )
            .padding(start = PaddingDefault),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        images.forEachIndexed { index, name ->
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .clip(shape)
                    .border(1.dp, Color.Black.copy(alpha = 0.06f), shape)
                    .clickable { onImageTapped(name) },
            ) {
                LocalImage(
                    name = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    contentDescription = "",
                )
                if (showImageDeleteButton) {
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp),
                        onClick = { onImageDeleteTapped(name) },
                        colors = imageDeleteColors,
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Delete Button",
                        )
                    }
                }
                if (showImageReorderButtons && images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Gray.copy(alpha = .8f)),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            enabled = index > 0,
                            onClick = { onImageMoveLeftTapped(name) },
                            colors = imageControlColors,
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.meal_details_image_move_left_cd),
                            )
                        }
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            enabled = index < images.lastIndex,
                            onClick = { onImageMoveRightTapped(name) },
                            colors = imageControlColors,
                        ) {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.meal_details_image_move_right_cd),
                            )
                        }
                    }
                }
            }
        }
        if (showAddPhotoButtons) {
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
            if (showInfoButton) {
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
        ImageStrip(
            images = listOf("1", "2"),
            showAddPhotoButtons = true,
            showImageReorderButtons = true,
            showInfoButton = true,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onImageMoveLeftTapped = {},
            onImageMoveRightTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onInfoButtonTapped = {},
        )
    }
}

@Preview
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ImageStripPreviewViewOnly() {
    ViewPreviewContext {
        ImageStrip(
            images = listOf("1", "2"),
            showAddPhotoButtons = false,
            showImageDeleteButton = false,
            onImageTapped = {},
            onImageDeleteTapped = {},
            onAddImageViaCameraTapped = {},
            onAddImageViaPickerTapped = {},
            onInfoButtonTapped = {},
        )
    }
}
