package dev.gaborbiro.feature.home.screens.home.model

import dev.gaborbiro.nutrition.core.clause.Clause

data class HomeViewState(
    val text: Clause = Clause.empty,
)