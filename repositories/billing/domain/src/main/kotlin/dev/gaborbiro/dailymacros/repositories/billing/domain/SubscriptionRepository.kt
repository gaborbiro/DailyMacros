package dev.gaborbiro.dailymacros.repositories.billing.domain

import android.app.Activity
import dev.gaborbiro.dailymacros.repositories.billing.domain.model.SubscriptionState
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {

    fun observeState(): Flow<SubscriptionState>

    /** Synchronous snapshot of the last known state, for one-shot gate checks. */
    fun currentState(): SubscriptionState

    /** Re-queries BillingClient and re-submits any unverified purchase for server verification. */
    fun refresh()

    fun launchPurchaseFlow(activity: Activity)
}
