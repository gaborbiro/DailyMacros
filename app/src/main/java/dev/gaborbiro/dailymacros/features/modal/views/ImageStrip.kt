package dev.gaborbiro.dailymacros.features.modal.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.features.common.view.LocalImage


@Composable
fun ImageStrip(
    images: List<String>,
    onImageTapped: (String) -> Unit,
    onAddImageTapped: () -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 64.dp,
    horizontalPadding: Dp = 16.dp,
    itemSpacing: Dp = 8.dp,
) {
    val shape = RoundedCornerShape(12.dp)

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 4.dp)
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

        // Trailing "Add" tile
        item("add") {
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .clip(shape)
                    .border(1.dp, Color.Blue.copy(alpha = 0.35f), shape)
                    .clickable(onClick = onAddImageTapped)
                    .background(Color.Blue.copy(alpha = 0.06f), shape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, contentDescription = "Add image", tint = Color.Blue)
                    Text("Add", style = MaterialTheme.typography.labelSmall, color = Color.Blue)
                }
            }
        }
    }
}
