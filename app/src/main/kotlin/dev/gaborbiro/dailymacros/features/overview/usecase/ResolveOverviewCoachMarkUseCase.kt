package dev.gaborbiro.dailymacros.features.overview.usecase

import dev.gaborbiro.dailymacros.features.overview.OverviewPrefs
import javax.inject.Inject

/**
 * Whether the one-time coach mark should run for this emission.
 * When true, the pref is cleared so it does not trigger again.
 */
class ResolveOverviewCoachMarkUseCase @Inject constructor(
    private val overviewPrefs: OverviewPrefs,
) {

    fun execute(mappedItemCount: Int): Boolean {
        if (mappedItemCount == 2 && overviewPrefs.showCoachMark) {
            overviewPrefs.showCoachMark = false
            return true
        }
        return false
    }
}
