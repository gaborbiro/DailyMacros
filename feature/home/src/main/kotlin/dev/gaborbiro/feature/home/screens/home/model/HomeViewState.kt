package dev.gaborbiro.feature.home.screens.home.model

data class HomeViewState(
    val question: String? = null,
    val answer: String? = null,
    val showProgress: Boolean = false,
)