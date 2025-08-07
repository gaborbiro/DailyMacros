package dev.gaborbiro.nutri.features.overview.views

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.gaborbiro.nutri.R
import dev.gaborbiro.nutri.design.PaddingDefault
import dev.gaborbiro.nutri.design.PaddingQuarter
import dev.gaborbiro.nutri.features.common.model.RecordViewState


@Composable
fun OverviewListItem(
    modifier: Modifier = Modifier,
    record: RecordViewState,
    onDuplicateRecord: (RecordViewState) -> Unit,
    onUpdateImage: (RecordViewState) -> Unit,
    onDeleteImage: (RecordViewState) -> Unit,
    onEditRecord: (RecordViewState) -> Unit,
    onDeleteRecord: (RecordViewState) -> Unit,
    onImageTapped: (RecordViewState) -> Unit,
    onNutrientsRequested: (RecordViewState) -> Unit,
) {
    Row(
        modifier = modifier
            .padding(start = PaddingDefault, end = PaddingDefault)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordImage(record.bitmap, Modifier.size(64.dp)) {
            onImageTapped(record)
        }
        Spacer(modifier = Modifier.size(PaddingDefault))
        TitleAndSubtitle(
            modifier = Modifier
                .wrapContentHeight()
                .padding(end = PaddingDefault)
                .weight(1f),
            record = record
        )
        PopupMenu(
            onDuplicateRecord = { onDuplicateRecord(record) },
            onUpdateImage = { onUpdateImage(record) },
            onDeleteImage = { onDeleteImage(record) },
            onEditRecord = { onEditRecord(record) },
            onDeleteRecord = { onDeleteRecord(record) },
            onNutrientsRequested = { onNutrientsRequested(record) },
        )
    }
}

@Composable
private fun RecordImage(bitmap: Bitmap?, modifier: Modifier, onTap: () -> Unit) {
    bitmap?.let {
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentScale = ContentScale.Crop,
            contentDescription = "note image",
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onTap)
        )
    } ?: run {
        Spacer(modifier)
    }
}

@Composable
private fun TitleAndSubtitle(modifier: Modifier, record: RecordViewState) {
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = record.title,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = record.timestamp,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(top = PaddingQuarter)
        )
    }
}

@Composable
private fun PopupMenu(
    onDuplicateRecord: () -> Unit,
    onUpdateImage: () -> Unit,
    onDeleteImage: () -> Unit,
    onEditRecord: () -> Unit,
    onDeleteRecord: () -> Unit,
    onNutrientsRequested: () -> Unit,
) {
    PopUpMenuButton(
        options = listOf(
            PopUpMenuItem(
                icon = Icons.Outlined.FileCopy,
                label = "Repeat note",
                onMenuItemSelected = onDuplicateRecord,
                hasBottomDivider = true,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Image,
                label = "Change image",
                onMenuItemSelected = onUpdateImage,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.HideImage,
                label = "Delete image",
                onMenuItemSelected = onDeleteImage,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Edit,
                label = "Edit",
                onMenuItemSelected = onEditRecord,
                hasBottomDivider = true,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Delete,
                label = "Delete",
                onMenuItemSelected = onDeleteRecord,
                hasBottomDivider = true,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Cake,
                label = "Nutrients",
                onMenuItemSelected = onNutrientsRequested,
            ),
        ),
    )
}

@Composable
private fun PopUpMenuButton(
    options: List<PopUpMenuItem>,
) {

    val expanded = remember { mutableStateOf(false) }

    Column {
        Box(modifier = Modifier.size(36.dp)) {
            IconButton(onClick = {
                expanded.value = expanded.value.not()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_vert),
                    contentDescription = "overflow menu",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Box {
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                options.forEachIndexed { _, item ->
                    DropdownMenuItem(
                        onClick = {
                            expanded.value = false
                            item.onMenuItemSelected()
                        },
                        text = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    )
                    if (item.hasBottomDivider) {
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

data class PopUpMenuItem(
    val label: String,
    val icon: ImageVector,
    val hasBottomDivider: Boolean = false,
    val onMenuItemSelected: () -> Unit,
)
