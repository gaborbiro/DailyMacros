package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
    onDetailsMenuItemTapped: () -> Unit,
    onDeleteRecordMenuItemTapped: () -> Unit,
    onMacrosMenuItemTapped: () -> Unit,
) {
    PopUpMenuButton(
        options = listOf(
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_exposure_plus_1),
                label = "Repeat",
                onMenuItemSelected = onRepeatMenuItemTapped,
                isDestructiveAction = false,
            ),
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_nutrition),
                label = "Macros (AI)",
                onMenuItemSelected = onMacrosMenuItemTapped,
                isDestructiveAction = false,
            ),
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_topic),
                label = "Details",
                onMenuItemSelected = onDetailsMenuItemTapped,
                isDestructiveAction = false,
            ),
            PopUpMenuItem(
                icon = painterResource(R.drawable.ic_delete),
                label = "Delete",
                onMenuItemSelected = onDeleteRecordMenuItemTapped,
                isDestructiveAction = true,
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
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
            ) {
                options.forEachIndexed { _, item ->
                    val background =
                        if (item.isDestructiveAction) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    val tint =
                        if (item.isDestructiveAction) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(background),
                        colors = MenuDefaults.itemColors(
                            leadingIconColor = tint,
                            textColor = tint,
                        ),
                        onClick = {
                            expanded.value = false
                            item.onMenuItemSelected()
                        },
                        text = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                overflow = TextOverflow.Companion.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = item.icon,
                                contentDescription = item.label,
                            )
                        }
                    )
                }
            }
        }
    }
}

private data class PopUpMenuItem(
    val label: String,
    val icon: Painter,
    val onMenuItemSelected: () -> Unit,
    val isDestructiveAction: Boolean,
)
