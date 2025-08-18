package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R

@Composable
internal fun PopupMenu(
    onRepeatMenuItemTapped: () -> Unit,
//    onChangeImageMenuItemTapped: () -> Unit,
//    onDeleteImageMenuItemTapped: () -> Unit,
    onEditRecordMenuItemTapped: () -> Unit,
    onDeleteRecordMenuItemTapped: () -> Unit,
    onMacrosMenuItemTapped: () -> Unit,
) {
    PopUpMenuButton(
        options = listOf(
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_dinner_dining),
                label = "Repeat",
                onMenuItemSelected = onRepeatMenuItemTapped,
            ),
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_nutrition),
                label = "Macros (AI)",
                onMenuItemSelected = onMacrosMenuItemTapped,
            ),
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_topic),
                label = "Details",
                onMenuItemSelected = onEditRecordMenuItemTapped,
                hasBottomDivider = true,
            ),
//            PopUpMenuItem(
//                icon = painterResource(R.drawable.ic_add_picture),
//                label = "Change image",
//                onMenuItemSelected = onChangeImageMenuItemTapped,
//            ),
//            PopUpMenuItem(
//                icon = painterResource(R.drawable.ic_hide_image),
//                label = "Delete image",
//                onMenuItemSelected = onDeleteImageMenuItemTapped,
//            ),
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_delete),
                label = "Delete",
                onMenuItemSelected = onDeleteRecordMenuItemTapped,
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
        Box(
            modifier = Modifier
                .size(36.dp)
        ) {
            IconButton(onClick = {
                expanded.value = expanded.value.not()
            }) {
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                    painter = painterResource(id = R.drawable.ic_more_vert),
                    contentDescription = "overflow menu",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Box {
            DropdownMenu(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer),
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
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
                                overflow = TextOverflow.Companion.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = item.icon,
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
    val icon: Painter,
    val hasBottomDivider: Boolean = false,
    val onMenuItemSelected: () -> Unit,
)
