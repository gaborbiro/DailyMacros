package dev.gaborbiro.dailymacros.features.overview.views

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import dev.gaborbiro.dailymacros.design.PaddingDefault

@Composable
internal fun SearchFAB(
    onSearch: (String?) -> Unit,
    onSearchCleared: () -> Unit,
) {
    var fabExpanded by remember { mutableStateOf(false) }
    var text by remember(fabExpanded) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    if (fabExpanded) {
        BackHandler {
            onSearch(null)
            onSearchCleared()
            fabExpanded = false
        }
    }

    AnimatedContent(
        targetState = fabExpanded,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "SearchFAB",
    ) { expanded ->
        if (!expanded) {
            FloatingActionButton(
                onClick = { fabExpanded = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    modifier = Modifier.padding(PaddingDefault),
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(280.dp),
                ) {
                    TextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onGloballyPositioned { focusRequester.requestFocus() },
                        colors = TextFieldDefaults.colors().copy(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = {
                            Text(
                                text = "Search",
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        value = text,
                        singleLine = true,
                        onValueChange = {
                            text = it
                            onSearch(it)
                        },
                    )
                    IconButton(
                        onClick = {
                            onSearch(null)
                            onSearchCleared()
                            fabExpanded = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }
    }
}
