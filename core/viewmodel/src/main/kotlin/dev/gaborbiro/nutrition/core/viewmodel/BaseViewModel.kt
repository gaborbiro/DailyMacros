package dev.gaborbiro.nutrition.core.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow


abstract class BaseViewModel<VS, U>(
    application: Application,
    initialViewState: VS,
) : AndroidViewModel(application) {

    protected val appContext: Context
        get() = this.getApplication<Application>().applicationContext

    private val viewStateBus = ViewStateBus(initialViewState, viewModelScope)
    private val uiUpdateBus = UIUpdateBus<U>()

    val viewState: StateFlow<VS> = viewStateBus.viewState

    val uiUpdates: StateFlow<List<UIUpdate<U>>> = uiUpdateBus.uiUpdates

    protected fun state(viewState: VS) {
        viewStateBus.state(viewState)
    }

    protected fun state(builderFunction: VS.() -> VS) {
        state(required = true, builderFunction)
    }

    protected fun <T : VS> state(required: Boolean = true, builderFunction: T.() -> T) {
        viewStateBus.state(required, builderFunction)
    }

    protected fun trigger(uiUpdate: U) {
        uiUpdateBus.send(uiUpdate)
    }
}

