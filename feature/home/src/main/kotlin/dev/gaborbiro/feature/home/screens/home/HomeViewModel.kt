package dev.gaborbiro.feature.home.screens.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.feature.home.screens.home.model.HomeUIUpdates
import dev.gaborbiro.feature.home.screens.home.model.HomeViewState
import dev.gaborbiro.nutrition.app_prefs.domain.AppPrefs
import dev.gaborbiro.nutrition.core.clause.Clause
import dev.gaborbiro.nutrition.core.clause.asText
import dev.gaborbiro.nutrition.core.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val appPrefs: AppPrefs,
) : BaseViewModel<HomeViewState, HomeUIUpdates>(application, HomeViewState()) {

    init {
        viewModelScope.launch {
            appPrefs.text.get().collect { text: String? ->
                state {
                    copy(text = text?.asText() ?: Clause.empty)
                }
            }
        }
    }

    fun onButtonTapped() {
        trigger(
            HomeUIUpdates.Toast(
                Clause.Date.Timestamp.Skeleton(
                    System.currentTimeMillis(),
                    "YMMMd"
                )
            )
        )
    }

    fun onTextChanged(text: String) {
        viewModelScope.launch {
            appPrefs.text.set(text)
        }
    }
}