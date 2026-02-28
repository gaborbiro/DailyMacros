package dev.gaborbiro.dailymacros.features.modal.model

sealed class ModalUIUpdates {
    data class Error(val message: String) : ModalUIUpdates()
}
