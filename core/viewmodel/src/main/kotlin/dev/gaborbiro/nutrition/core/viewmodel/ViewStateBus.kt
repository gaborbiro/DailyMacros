package dev.gaborbiro.nutrition.core.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn


class ViewStateBus<VS>(initialViewState: VS, scope: CoroutineScope) {

    private val _viewState: MutableStateFlow<VS> = MutableStateFlow(initialViewState)
    val viewState: StateFlow<VS> = _viewState
        .debounce(300)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = initialViewState
        )

    fun state(viewState: VS) {
        _viewState.tryEmit(viewState)
    }

    fun state(builderFunction: VS.() -> VS) {
        state(required = true, builderFunction)
    }

    fun <T : VS> state(required: Boolean = true, builderFunction: T.() -> T) {
        try {
            val state = _viewState.value as T
            _viewState.tryEmit(state.builderFunction())
        } catch (e: ClassCastException) {
            if (required) {
                throw e
            }
        }
    }
}