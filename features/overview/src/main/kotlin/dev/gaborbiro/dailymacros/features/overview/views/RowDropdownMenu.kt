package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.common.R as CommonR
import dev.gaborbiro.dailymacros.features.overview.R
import dev.gaborbiro.dailymacros.design.PaddingHalf

@Composable
fun RowDropdownMenu(
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    icons: MenuIcons,
    onRepeatTapped: (() -> Unit)?,
    onAnalyseMacrosTapped: (() -> Unit)?,
    onDeleteTapped: (() -> Unit)?,
) {

    Box {
        Icon(
            modifier = Modifier
                .size(36.dp)
                .padding(end = PaddingHalf)
                .clickable(
                    onClick = onOpen,
                ),
            painter = painterResource(CommonR.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.overview_content_menu_more_cd),
        )

        if (expanded) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = onDismiss,
                offset = DpOffset(0.dp, 0.dp)
            ) {
                onRepeatTapped?.let {
                    DropdownMenuItem(
                        leadingIcon = { Icon(icons.repeat, null) },
                        text = { Text(stringResource(R.string.overview_content_menu_log_again)) },
                        onClick = { onDismiss(); onRepeatTapped() }
                    )
                }
                onAnalyseMacrosTapped?.let {
                    DropdownMenuItem(
                        leadingIcon = { Icon(icons.macros, null) },
                        text = { Text(stringResource(R.string.overview_content_menu_reanalyse)) },
                        onClick = { onDismiss(); onAnalyseMacrosTapped() }
                    )
                }
                onDeleteTapped?.let {
                    DropdownMenuItem(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer),
                        leadingIcon = { Icon(icons.delete, null) },
                        text = { Text(stringResource(R.string.overview_content_menu_delete)) },
                        onClick = { onDismiss(); onDeleteTapped() },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onErrorContainer,
                            leadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    )
                }
            }
        }
    }
}

@Stable
data class MenuIcons(
    val repeat: Painter,
    val macros: Painter,
    val details: Painter,
    val star: Painter,
    val delete: Painter,
)
