package dev.gaborbiro.dailymacros.features.settings.util

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.verticalScrollWithBar(): Modifier = verticalScroll(rememberScrollState())
