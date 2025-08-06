package dev.gaborbiro.nutri.features.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaborbiro.nutri.features.common.model.ErrorViewState
import ellipsize
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class ErrorViewModel : ViewModel() {

    private val _errorState: MutableStateFlow<ErrorViewState?> = MutableStateFlow(null)
    val errorState: StateFlow<ErrorViewState?> = _errorState.asStateFlow()


    protected fun runSafely(task: suspend () -> Unit): Job {
        return viewModelScope.launch(errorHandler) {
            task()
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        _errorState.update {
            ErrorViewState(
                "Oops. Something went wrong ${
                    exception.message?.let {
                        "\n\n(${
                            it.ellipsize(
                                300
                            )
                        })"
                    } ?: ""
                }")
        }
        Log.w("BaseViewModel", "Uncaught exception", exception)
    }

    fun onErrorDialogDismissRequested() {
        _errorState.update {
            null
        }
    }
}
