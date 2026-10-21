package dev.gaborbiro.dailymacros.repositories.billing.domain

import android.app.Activity
import dev.gaborbiro.dailymacros.repositories.billing.domain.model.SubscriptionState
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {

    fun observeState(): Flow<SubscriptionState>

    /** Synchronous snapshot of the last known state, for one-shot gate checks. */
    fun currentState(): SubscriptionState

    /** Re-queries BillingClient and re-submits any unverified purchase for server verification. */
    fun refresh()

    /** Opens Play's purchase sheet. If it can't be opened, [observePurchaseFlowFailures] emits. */
    fun launchPurchaseFlow(activity: Activity)

    /**
     * Emits when [launchPurchaseFlow] couldn't open Play's purchase sheet. Failures inside the
     * sheet (declined card, etc.) are shown by Play itself and never arrive here.
     */
    fun observePurchaseFlowFailures(): Flow<DomainError>
}
