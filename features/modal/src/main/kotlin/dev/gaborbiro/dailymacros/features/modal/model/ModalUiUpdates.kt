package dev.gaborbiro.dailymacros.features.modal.model

import android.net.Uri

sealed class ModalUiUpdates {
    data class Error(val message: String) : ModalUiUpdates()
    data class ShowToast(val message: String) : ModalUiUpdates()
    data class ShareImage(val uri: Uri) : ModalUiUpdates()
}
