package dev.gaborbiro.nutri.features.common

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaborbiro.nutri.features.common.model.ErrorViewState
import dev.gaborbiro.nutri.features.common.views.ErrorDialog

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
