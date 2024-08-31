package dev.gaborbiro.feature.home.screens.home.model

import dev.gaborbiro.nutrition.core.clause.Clause

sealed class HomeUIUpdates {
    data class Toast(val message: Clause) : HomeUIUpdates()
}