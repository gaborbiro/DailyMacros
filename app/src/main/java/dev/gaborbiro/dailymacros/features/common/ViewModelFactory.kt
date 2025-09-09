package dev.gaborbiro.dailymacros.features.common


import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

class ViewModelFactory<T : ViewModel>(
    private val creator: () -> T,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}

@Composable
inline fun <reified T : ViewModel> viewModelFactory(
    noinline creator: () -> T,
): T {
    return viewModel(factory = ViewModelFactory(creator))
}
