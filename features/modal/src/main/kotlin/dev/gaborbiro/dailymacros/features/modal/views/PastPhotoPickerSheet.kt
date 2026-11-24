package dev.gaborbiro.dailymacros.features.modal.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.common.views.LocalImage
import dev.gaborbiro.dailymacros.features.common.views.ViewPreviewContext
import dev.gaborbiro.dailymacros.features.modal.R
import dev.gaborbiro.dailymacros.features.modal.model.DialogHandle
import dev.gaborbiro.dailymacros.features.modal.model.PastPhotoUiModel

/** Search among photos of earlier meals and pick some to add to the meal being edited. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PastPhotoPickerSheet(
    dialogHandle: DialogHandle.PastPhotoPicker,
    onQueryChanged: (TextFieldValue) -> Unit,
    onPhotoToggled: (String) -> Unit,
    onAddTapped: () -> Unit,
    onDismissRequested: () -> Unit,
) {
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequested,
    ) {
        PastPhotoPickerContent(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .imePadding(),
            dialogHandle = dialogHandle,
            onQueryChanged = onQueryChanged,
            onPhotoToggled = onPhotoToggled,
            onAddTapped = onAddTapped,
            onCloseTapped = onDismissRequested,
        )
    }
}

@Composable
private fun PastPhotoPickerContent(
    dialogHandle: DialogHandle.PastPhotoPicker,
    onQueryChanged: (TextFieldValue) -> Unit,
    onPhotoToggled: (String) -> Unit,
    onAddTapped: () -> Unit,
    onCloseTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCloseTapped) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.past_photos_close_cd),
                )
            }
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.past_photos_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            val selectedCount = dialogHandle.selectedFilenames.size
            Button(
                enabled = selectedCount > 0,
                onClick = onAddTapped,
            ) {
                Text(
                    if (selectedCount > 0) {
                        stringResource(R.string.past_photos_add_count, selectedCount)
                    } else {
                        stringResource(R.string.past_photos_add)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = dialogHandle.query,
            onValueChange = onQueryChanged,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.past_photos_search_hint)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (dialogHandle.query.text.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged(TextFieldValue()) }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.past_photos_clear_search_cd),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                dialogHandle.isLoading -> CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp),
                )

                !dialogHandle.hasPastPhotos -> EmptyMessage(stringResource(R.string.past_photos_empty))

                dialogHandle.photos.isEmpty() -> EmptyMessage(
                    stringResource(R.string.past_photos_no_matches, dialogHandle.query.text.trim())
                )

                else -> LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(dialogHandle.photos, key = { it.filename }) { photo ->
                        PastPhotoTile(
                            photo = photo,
                            selectionNumber = dialogHandle.selectedFilenames.indexOf(photo.filename) + 1,
                            onTapped = { onPhotoToggled(photo.filename) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * @param selectionNumber the photo's position among the picked ones (they are added in that order),
 * or 0 when it isn't picked
 */
@Composable
private fun PastPhotoTile(
    photo: PastPhotoUiModel,
    selectionNumber: Int,
    onTapped: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val selected = selectionNumber > 0
    Column(modifier = Modifier.clickable(onClick = onTapped)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.06f),
                    shape = shape,
                ),
        ) {
            LocalImage(
                name = photo.filename,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                contentDescription = photo.caption,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = selectionNumber.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (photo.caption.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = photo.caption,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PastPhotoPickerPreview() {
    ViewPreviewContext {
        PastPhotoPickerContent(
            modifier = Modifier.height(480.dp),
            dialogHandle = DialogHandle.PastPhotoPicker(
                query = TextFieldValue("soy"),
                photos = listOf(
                    PastPhotoUiModel("1", "Granola with soy milk"),
                    PastPhotoUiModel("2", "Porridge with soy milk and banana"),
                    PastPhotoUiModel("3", "Soy latte"),
                ),
                isLoading = false,
                hasPastPhotos = true,
                selectedFilenames = listOf("2"),
                maxSelectable = 4,
            ),
            onQueryChanged = {},
            onPhotoToggled = {},
            onAddTapped = {},
            onCloseTapped = {},
        )
    }
}

@Preview
@Composable
private fun PastPhotoPickerNoMatchesPreview() {
    ViewPreviewContext {
        PastPhotoPickerContent(
            modifier = Modifier.height(240.dp),
            dialogHandle = DialogHandle.PastPhotoPicker(
                query = TextFieldValue("kombucha"),
                isLoading = false,
                hasPastPhotos = true,
                maxSelectable = 5,
            ),
            onQueryChanged = {},
            onPhotoToggled = {},
            onAddTapped = {},
            onCloseTapped = {},
        )
    }
}
