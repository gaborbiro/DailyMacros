package dev.gaborbiro.feature.home.screens.home

import android.app.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.feature.home.screens.home.model.HomeUIUpdates
import dev.gaborbiro.feature.home.screens.home.model.HomeViewState
import dev.gaborbiro.nutrition.core.clause.Clause
import dev.gaborbiro.nutrition.core.viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(application: Application) :
    BaseViewModel<HomeViewState, HomeUIUpdates>(application, HomeViewState()) {

    fun onButtonTapped() {
        state {
            copy(text = Clause.Text.Plain("Clicked. Now what?"))
        }
        trigger(
            HomeUIUpdates.Toast(
                Clause.Date.Timestamp.Skeleton(
                    System.currentTimeMillis(),
                    "YMMMd"
                )
            )
        )
    }
}