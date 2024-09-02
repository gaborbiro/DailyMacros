package dev.gaborbiro.feature.home.screens.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.feature.home.screens.home.model.HomeUIUpdates
import dev.gaborbiro.feature.home.screens.home.model.HomeViewState
import dev.gaborbiro.nutrition.app_prefs.domain.AppPrefs
import dev.gaborbiro.nutrition.core.clause.asText
import dev.gaborbiro.nutrition.core.viewmodel.BaseViewModel
import dev.gaborbiro.nutrition.data.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.nutrition.data.chatgpt.domain.model.Request
import dev.gaborbiro.nutrition.data.common.model.DomainError
import dev.gaborbiro.nutrition.feature.common.errorMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val appPrefs: AppPrefs,
    private val chatGptRepository: ChatGPTRepository,
) : BaseViewModel<HomeViewState, HomeUIUpdates>(application, HomeViewState()) {

    init {
        viewModelScope.launch {
            val savedQuery = appPrefs.request.get().first()
            state {
                copy(question = savedQuery ?: "")
            }
        }
    }

    fun onButtonTapped() {
        viewModelScope.launch {
            state {
                copy(
                    showProgress = true
                )
            }
            val savedRequest = appPrefs.request.get().first()
            savedRequest?.let {
                try {
                    val answer = chatGptRepository.request(Request(it))
                    state {
                        copy(answer = answer.response)
                    }
                } catch (e: DomainError) {
                    handleDomainError(e)
                }
            }
            state {
                copy(
                    showProgress = false
                )
            }
        }
    }

    fun onTextChanged(text: String) {
        viewModelScope.launch {
            appPrefs.request.set(text)
        }
    }

    private fun handleDomainError(error: DomainError) {
        when (error) {
            is DomainError.DisplayMessageToUser -> {
                val message = error.errorMessage()
                trigger(HomeUIUpdates.Toast(message))
            }

            is DomainError.GoToSignInScreen -> {
                trigger(HomeUIUpdates.Toast("Auth error".asText()))
            }
        }
    }
}