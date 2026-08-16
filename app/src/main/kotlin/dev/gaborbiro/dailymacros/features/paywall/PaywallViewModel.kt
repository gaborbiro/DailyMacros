package dev.gaborbiro.dailymacros.features.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.repositories.billing.domain.SubscriptionRepository
import dev.gaborbiro.dailymacros.repositories.billing.domain.model.SubscriptionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * A standalone "subscribe" screen reachable from anywhere (Overview's proactive banner, a
 * failed-AI-analysis notification) independent of the Settings screen, which stays hidden
 * until the user has their first record - see Settings' own gating in OverviewViewModel.
 */
@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    val subscriptionState: StateFlow<SubscriptionState> = subscriptionRepository.observeState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), subscriptionRepository.currentState())

    fun onSubscribeTapped(activity: Activity) {
        subscriptionRepository.launchPurchaseFlow(activity)
    }
}
