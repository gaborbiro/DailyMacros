package dev.gaborbiro.dailymacros.repositories.billing.domain.model

/**
 * [Unknown] is distinct from [NotSubscribed] so UI doesn't flash "please
 * subscribe" before the BillingClient connection has resolved a real answer.
 */
sealed class SubscriptionState {
    data object Active : SubscriptionState()
    data object NotSubscribed : SubscriptionState()
    data object Unknown : SubscriptionState()
}
