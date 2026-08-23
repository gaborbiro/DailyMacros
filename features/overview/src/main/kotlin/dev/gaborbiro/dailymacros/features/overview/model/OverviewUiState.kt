package dev.gaborbiro.dailymacros.features.overview.model

import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record


data class OverviewUiState(
    val items: List<ListUiModelBase> = emptyList(),
    val isLoadingMore: Boolean = false,
    val hasMoreData: Boolean = true,
    val showAddWidgetButton: Boolean = false,
    val showUndoDeleteSnackbar: Boolean = false,
    val recordToUndelete: Record? = null,
    val showSettingsButton: Boolean = false,
    val showSubscribeBanner: Boolean = false,
    /** One-shot: set when a tap on a Trends chart point resolved to a day card already present
     *  in [items], asking the view to scroll to it - see OverviewViewModel.onScrollToDateRequested.
     *  Cleared via onScrollHandled() once the view has acted on it. */
    val scrollToListItemId: Long? = null,
    /** Non-null from the moment a Trends chart tap requests a scroll (see
     *  OverviewViewModel.onScrollToDateRequested) until [scrollToListItemId] fires - covers the
     *  page-widen + reload that can be needed when the target date isn't loaded yet, which is
     *  the "takes a while for the scrolling to start" gap this label's spinner is shown for. */
    val pendingScrollDateLabel: String? = null,
)
