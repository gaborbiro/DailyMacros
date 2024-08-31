package dev.gaborbiro.nutrition.core.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class UIUpdateBus<U> {

    private val _uiUpdates: MutableStateFlow<List<UIUpdate<U>>> = MutableStateFlow(emptyList())
    val uiUpdates: StateFlow<List<UIUpdate<U>>> = _uiUpdates

    fun send(uiUpdate: U) {
        _uiUpdates.tryEmit(uiUpdates.value + UIUpdateImpl(uiUpdate))
    }

    private inner class UIUpdateImpl(private val data: U) : UIUpdate<U> {

        override fun get(): U? = synchronized(this@UIUpdateBus) {
            if (_uiUpdates.value.contains(this)) {
                _uiUpdates.value = uiUpdates.value - this
                data
            } else {
                null
            }
        }

        override fun peek(): U {
            return data
        }
    }
}

fun <T> Any.takeIfType(builderFunction: T.() -> T): T? {
    return (this as? T)?.builderFunction()
}