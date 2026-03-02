package dev.gaborbiro.dailymacros.features.modal.model

sealed class ModalUiUpdates {
    data class Error(val message: String) : ModalUiUpdates()
    data object Close : ModalUiUpdates()
}
