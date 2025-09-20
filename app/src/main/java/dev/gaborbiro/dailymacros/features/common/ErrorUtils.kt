package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.repo.chatgpt.model.DomainError


internal fun DomainError.message() = when (this) {
    is DomainError.DisplayMessageToUser.CheckInternetConnection -> "Internet connectivity error"
    is DomainError.DisplayMessageToUser.ContactSupport -> "Oops. Something went wrong. The issue has been logged and our engineers are looking into it."
    is DomainError.DisplayMessageToUser.Message -> this.message
    is DomainError.DisplayMessageToUser.TryAgain -> "Oops. Something went wrong. Please try again later."
}
