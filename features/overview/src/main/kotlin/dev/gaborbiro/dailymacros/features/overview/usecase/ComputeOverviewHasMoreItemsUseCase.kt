package dev.gaborbiro.dailymacros.features.overview.usecase

import javax.inject.Inject

/** Whether overview should still offer “load more” for the non-search diary list. */
class ComputeOverviewHasMoreItemsUseCase @Inject constructor() {

    fun execute(isSearchActive: Boolean, previousItemCount: Int, currentItemCount: Int): Boolean {
        if (isSearchActive) return false
        if (previousItemCount >= 0 && currentItemCount <= previousItemCount) return false
        return true
    }
}
