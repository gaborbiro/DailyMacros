package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.R

@Composable
fun RowMenu(
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    icons: MenuIcons,
    onRepeat: () -> Unit,
    onMacros: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }

    Box {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert), // or your 3-dots painter hoisted too
            contentDescription = "More Menu",
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onOpen,
                )
        )

        if (expanded) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = onDismiss
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(icons.repeat, null) },
                    text = { Text("Repeat") },
                    onClick = { onDismiss(); onRepeat() }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(icons.macros, null) },
                    text = { Text("Macros (AI)") },
                    onClick = { onDismiss(); onMacros() }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(icons.details, null) },
                    text = { Text("Details") },
                    onClick = { onDismiss(); onDetails() }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(icons.delete, null) },
                    text = { Text("Delete") },
                    onClick = { onDismiss(); onDelete() },
                )
            }
        }
    }
}

@Stable
data class MenuIcons(
    val repeat: Painter, val macros: Painter, val details: Painter, val delete: Painter,
)
