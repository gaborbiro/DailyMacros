package dev.gaborbiro.dailymacros.features.overview.views

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R
import dev.gaborbiro.dailymacros.design.PaddingDefault
import dev.gaborbiro.dailymacros.design.PaddingQuarter
import dev.gaborbiro.dailymacros.features.common.model.RecordViewState
import dev.gaborbiro.dailymacros.util.dummyBitmap


@Composable
fun OverviewListItem(
    modifier: Modifier = Modifier,
    record: RecordViewState,
    onRepeatMenuItemTapped: (RecordViewState) -> Unit,
    onChangeImageMenuItemTapped: (RecordViewState) -> Unit,
    onDeleteImageMenuItemTapped: (RecordViewState) -> Unit,
    onEditRecordMenuItemTapped: (RecordViewState) -> Unit,
    onDeleteRecordMenuItemTapped: (RecordViewState) -> Unit,
    onNutrientsMenuItemTapped: (RecordViewState) -> Unit,
    onRecordImageTapped: (RecordViewState) -> Unit,
    onRecordBodyTapped: (RecordViewState) -> Unit,
) {
    Row(
        modifier = modifier
            .padding(start = PaddingDefault, end = PaddingDefault)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordImage(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = { onRecordImageTapped(record) }),
            bitmap = record.bitmap,
        )
        Spacer(
            modifier = Modifier
                .size(PaddingDefault)
        )
        TitleAndSubtitle(
            modifier = Modifier
                .wrapContentHeight()
                .clickable(onClick = { onRecordBodyTapped(record) })
                .padding(end = PaddingDefault)
                .weight(1f),
            record = record
        )
        PopupMenu(
            onRepeatMenuItemTapped = { onRepeatMenuItemTapped(record) },
            onChangeImageMenuItemTapped = { onChangeImageMenuItemTapped(record) },
            onDeleteImageMenuItemTapped = { onDeleteImageMenuItemTapped(record) },
            onEditRecordMenuItemTapped = { onEditRecordMenuItemTapped(record) },
            onDeleteRecordMenuItemTapped = { onDeleteRecordMenuItemTapped(record) },
            onNutrientsMenuItemTapped = { onNutrientsMenuItemTapped(record) },
        )
    }
}

@Composable
private fun RecordImage(
    modifier: Modifier,
    bitmap: Bitmap?,
) {
    bitmap?.let {
        Image(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp)),
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentScale = ContentScale.Crop,
            contentDescription = "note image",
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
    onRepeatMenuItemTapped: () -> Unit,
    onChangeImageMenuItemTapped: () -> Unit,
    onDeleteImageMenuItemTapped: () -> Unit,
    onEditRecordMenuItemTapped: () -> Unit,
    onDeleteRecordMenuItemTapped: () -> Unit,
    onNutrientsMenuItemTapped: () -> Unit,
) {
    PopUpMenuButton(
        options = listOf(
            PopUpMenuItem(
                icon = Icons.Outlined.FileCopy,
                label = "Repeat",
                onMenuItemSelected = onRepeatMenuItemTapped,
                hasBottomDivider = true,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Image,
                label = "Change image",
                onMenuItemSelected = onChangeImageMenuItemTapped,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.HideImage,
                label = "Delete image",
                onMenuItemSelected = onDeleteImageMenuItemTapped,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Edit,
                label = "Details",
                onMenuItemSelected = onEditRecordMenuItemTapped,
                hasBottomDivider = true,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Delete,
                label = "Delete",
                onMenuItemSelected = onDeleteRecordMenuItemTapped,
                hasBottomDivider = true,
            ),
            PopUpMenuItem(
                icon = Icons.Outlined.Cake,
                label = "Nutrients",
                onMenuItemSelected = onNutrientsMenuItemTapped,
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

private data class PopUpMenuItem(
    val label: String,
    val icon: ImageVector,
    val hasBottomDivider: Boolean = false,
    val onMenuItemSelected: () -> Unit,
)

@Preview
@Composable
private fun OverviewListItemPReview() {
    OverviewListItem(
        record = RecordViewState(
            recordId = 1L,
            title = "Title",
            templateId = 1L,
            bitmap = dummyBitmap(),
            timestamp = "2022-01-01 00:00:00"
        ),
        onRepeatMenuItemTapped = {},
        onChangeImageMenuItemTapped = {},
        onDeleteImageMenuItemTapped = {},
        onEditRecordMenuItemTapped = {},
        onDeleteRecordMenuItemTapped = {},
        onRecordImageTapped = {},
        onRecordBodyTapped = {},
        onNutrientsMenuItemTapped = {},
    )
}
