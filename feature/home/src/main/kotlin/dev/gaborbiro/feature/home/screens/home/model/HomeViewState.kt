package dev.gaborbiro.feature.home.screens.home.model

import com.google.android.gms.auth.api.identity.AuthorizationRequest

data class HomeViewState(
    val question: String? = null,
    val answer: String? = null,
    val authorizationRequest: AuthorizationRequest? = null,
    val showFetchKeepNotesProgress: Boolean = false,
    val showQueryProgress: Boolean = false,
)