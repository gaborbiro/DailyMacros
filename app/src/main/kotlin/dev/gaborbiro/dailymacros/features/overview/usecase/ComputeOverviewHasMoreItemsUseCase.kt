package dev.gaborbiro.dailymacros.features.overview.usecase

/** Whether overview should still offer “load more” for the non-search diary list. */
internal class ComputeOverviewHasMoreItemsUseCase {

    fun execute(isSearchActive: Boolean, previousItemCount: Int, currentItemCount: Int): Boolean {
        if (isSearchActive) return false
        if (previousItemCount >= 0 && currentItemCount <= previousItemCount) return false
        return true
    }
}
