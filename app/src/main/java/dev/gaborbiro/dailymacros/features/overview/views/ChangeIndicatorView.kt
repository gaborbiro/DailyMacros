package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.common.model.ChangeDirection
import dev.gaborbiro.dailymacros.features.common.model.ChangeIndicator

@Composable
internal fun ChangeIndicatorView(
    changeIndicator: ChangeIndicator,
    showColor: Boolean,
    textStyle: TextStyle,
) {
    val (icon, baseColor) = when (changeIndicator.direction) {
        ChangeDirection.UP -> Icons.Default.ArrowUpward to Color.Green
        ChangeDirection.DOWN -> Icons.Default.ArrowDownward to Color.Red
        ChangeDirection.NEUTRAL -> null to null
    }

    val tintColor = if (showColor && baseColor != null) baseColor else LocalContentColor.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(
                modifier = Modifier.size(14.dp),
                imageVector = it,
                contentDescription = null,
                tint = tintColor
            )
        }
        Text(
            text = changeIndicator.value,
            style = textStyle,
        )
    }
}
