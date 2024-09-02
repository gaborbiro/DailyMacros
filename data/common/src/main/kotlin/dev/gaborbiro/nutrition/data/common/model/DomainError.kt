package dev.gaborbiro.nutrition.data.common.model


sealed class DomainError(message: String?) : Exception(message) {

    sealed class DisplayMessageToUser(override val message: String? = null) : DomainError(message) {
        data class Message(override val message: String) : DisplayMessageToUser(message)
        data object TryAgain : DisplayMessageToUser()
        data object ContactSupport : DisplayMessageToUser()
        data object CheckInternetConnection : DisplayMessageToUser()
    }

    data class GoToSignInScreen(override val message: String? = null) : DomainError(message)
}