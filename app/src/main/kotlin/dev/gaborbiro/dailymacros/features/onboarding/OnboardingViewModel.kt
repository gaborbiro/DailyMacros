package dev.gaborbiro.dailymacros.features.onboarding

import android.app.Activity
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.AppPrefs
import dev.gaborbiro.dailymacros.repositories.billing.domain.SubscriptionRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPrefs: AppPrefs,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    fun onStartTrialTapped(activity: Activity) {
        subscriptionRepository.launchPurchaseFlow(activity)
        appPrefs.hasCompletedOnboarding = true
    }

    fun onSkipTapped() {
        appPrefs.hasCompletedOnboarding = true
    }
}
