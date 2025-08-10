package dev.gaborbiro.dailymacros.features.common.error

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.dailymacros.features.common.error.model.ErrorViewState
import dev.gaborbiro.dailymacros.features.common.error.views.ErrorDialog

abstract class BaseErrorDialogActivity : AppCompatActivity() {

    protected abstract fun baseViewModel(): ErrorViewModel

    @Composable
    protected fun HandleErrors() {
        val error: State<ErrorViewState?> = baseViewModel().errorState.collectAsStateWithLifecycle()

        error.value?.let {
            ErrorDialog(viewModel = baseViewModel(), error = it)
        }
    }
}
