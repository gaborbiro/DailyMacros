package dev.gaborbiro.nutrition.core.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.gaborbiro.nutrition.core.compose.theme.NutriTheme

@Composable
fun PreviewContext(content: @Composable (modifier: Modifier) -> Unit) {
    NutriTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}