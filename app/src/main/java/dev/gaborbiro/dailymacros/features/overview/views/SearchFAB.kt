package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
internal fun SearchFAB(onSearch: (String?) -> Unit) {
    var fabExpanded by remember { mutableStateOf(false) }
    var text by remember {
        mutableStateOf("")
    }
    val focusRequester = remember { FocusRequester() }

    FloatingActionButton(
        onClick = {
            fabExpanded = fabExpanded.not()
            if (fabExpanded.not()) {
                onSearch(null)
            }
        },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            AnimatedVisibility(visible = fabExpanded) {
                TextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onGloballyPositioned {
                            focusRequester.requestFocus() // IMPORTANT
                        },
                    colors = TextFieldDefaults.colors().copy(
                        unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        focusedIndicatorColor = Color.Companion.Transparent,
                        unfocusedIndicatorColor = Color.Companion.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = {
                        Text(
                            text = "Search",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
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
            }
            Icon(
                modifier = Modifier
                    .padding(PaddingDefault),
                imageVector = if (!fabExpanded) Icons.Filled.Search else Icons.Filled.Close,
                contentDescription = "search",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
