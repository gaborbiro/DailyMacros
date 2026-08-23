package dev.gaborbiro.dailymacros.features.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayStartTime
import dev.gaborbiro.dailymacros.features.common.utils.diaryDayWindowStart
import dev.gaborbiro.dailymacros.features.overview.model.ListUiModelDailySummary
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiState
import dev.gaborbiro.dailymacros.features.overview.model.OverviewUiUpdates
import dev.gaborbiro.dailymacros.features.overview.usecase.CancelMacrosAnalysisForRecordUseCase
import dev.gaborbiro.dailymacros.features.overview.usecase.ComputeOverviewHasMoreItemsUseCase
import dev.gaborbiro.dailymacros.features.overview.usecase.DeleteUnusedTemplateIfOrphanedUseCase
import dev.gaborbiro.dailymacros.features.overview.usecase.ResolveOverviewObserveSinceEpochMillisUseCase
import dev.gaborbiro.dailymacros.features.shared.CreateRecordFromTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.ListMealVariantsForTemplateUseCase
import dev.gaborbiro.dailymacros.features.shared.NutrientAnalysisWorker
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelBase
import dev.gaborbiro.dailymacros.features.shared.model.ListUiModelRecord
import dev.gaborbiro.dailymacros.repositories.billing.domain.SubscriptionRepository
import dev.gaborbiro.dailymacros.repositories.billing.domain.model.SubscriptionState
import dev.gaborbiro.dailymacros.repositories.records.domain.RecordsRepository
import dev.gaborbiro.dailymacros.repositories.records.domain.model.Record
import dev.gaborbiro.dailymacros.repositories.settings.domain.SettingsRepository
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.Targets
import dev.gaborbiro.dailymacros.repositories.settings.domain.model.hasAnyEnabled
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    application: Application,
    private val recordsRepository: RecordsRepository,
    private val settingsRepository: SettingsRepository,
    private val uiMapper: OverviewUiMapper,
    private val resolveObserveSinceEpochMillis: ResolveOverviewObserveSinceEpochMillisUseCase,
    private val computeHasMoreItems: ComputeOverviewHasMoreItemsUseCase,
    private val deleteUnusedTemplateIfOrphaned: DeleteUnusedTemplateIfOrphanedUseCase,
    private val cancelMacrosAnalysisForRecord: CancelMacrosAnalysisForRecordUseCase,
    private val listMealVariantsForTemplateUseCase: ListMealVariantsForTemplateUseCase,
    private val createRecordFromTemplateUseCase: CreateRecordFromTemplateUseCase,
    private val subscriptionRepository: SubscriptionRepository,
) : AndroidViewModel(application) {

    private val _viewState: MutableStateFlow<OverviewUiState> =
        MutableStateFlow(OverviewUiState())
    val viewState: StateFlow<OverviewUiState> = _viewState.asStateFlow()

    private val _uiUpdates = MutableSharedFlow<OverviewUiUpdates>()
    val uiUpdates: SharedFlow<OverviewUiUpdates> = _uiUpdates.asSharedFlow()

    private companion object {
        val PAGE_SIZE = 14.days

        // The Overview screen only ever calls onLoadMore() by scrolling near
        // the bottom of the rendered list, which never mounts while items is
        // empty (see OverviewView.kt: WelcomeView shows instead). Without a
        // separate, more patient retry here, anyone who hasn't opened the
        // app (or restored a backup) in more than PAGE_SIZE gets permanently
        // stuck on the empty state, even though older records exist just
        // outside the current window. This bound is how far back the
        // auto-widen below is willing to look before concluding there's
        // genuinely nothing - generous enough to cover months of inactivity
        // (20 * 14 days ≈ 9 months) while still bounded for a truly-empty diary.
        const val MAX_CATCH_UP_WIDENS = 20

        val SCROLL_TARGET_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    }

    private var sinceEpochMillis: Long = System.currentTimeMillis() - PAGE_SIZE.inWholeMilliseconds
    private var currentSearch: String? = null
    private var collectionJob: Job? = null
    private var previousRecordCount: Int = -1
    private var autoCatchUpWidens: Int = 0
    private var pendingScrollToEpochDay: Long? = null

    init {
        // The one-time initial load. Deliberately not triggered by OverviewScreen's
        // "clear any stale search" LaunchedEffect(Unit) call to onSearchTermChanged(null) below -
        // that runs on *every* re-entry to the Overview screen (e.g. returning from Trends via
        // back navigation), not just the first one, and used to unconditionally reset
        // sinceEpochMillis back to "now" each time, silently discarding any widened window (e.g.
        // from a Trends-triggered scroll-to-date) the moment the user navigated away and back.
        resubscribe(currentSearch)
    }

    fun onSearchTermChanged(search: String?) {
        if (search == currentSearch) return
        currentSearch = search
        // Reset paging window when the search term changes
        sinceEpochMillis = System.currentTimeMillis() - PAGE_SIZE.inWholeMilliseconds
        previousRecordCount = -1
        autoCatchUpWidens = 0
        _viewState.update { it.copy(hasMoreData = true) }
        resubscribe(search)
    }

    fun onLoadMore() {
        if (!_viewState.value.hasMoreData || _viewState.value.isLoadingMore) return
        _viewState.update { it.copy(isLoadingMore = true) }
        sinceEpochMillis -= PAGE_SIZE.inWholeMilliseconds
        resubscribe(currentSearch)
    }

    private fun resubscribe(search: String?) {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            val searchBlank = search.isNullOrBlank()
            val sinceMillis = resolveObserveSinceEpochMillis.execute(searchBlank, sinceEpochMillis)
            combine(
                recordsRepository.observeRecords(search, sinceEpochMillis = sinceMillis),
                settingsRepository.observeTargets(),
                subscriptionRepository.observeState(),
            ) { records: List<Record>, targets: Targets, subscriptionState: SubscriptionState ->
                Triple(records, targets, subscriptionState)
            }
                .mapLatest { (records: List<Record>, targets: Targets, subscriptionState: SubscriptionState) ->
                    val items = if (searchBlank) {
                        uiMapper.map(records, targets)
                    } else {
                        uiMapper.mapSearchResults(records)
                    }
                    val showSubscribeBanner = records.isNotEmpty() &&
                        subscriptionState == SubscriptionState.NotSubscribed &&
                        !settingsRepository.getSubscribeBannerDismissed()
                    enrichRecordRowsWithOtherVariantsIcon(items) to showSubscribeBanner
                }
                .collect { (records: List<ListUiModelBase>, showSubscribeBanner: Boolean) ->
                    val hasMore = computeHasMoreItems.execute(
                        isSearchActive = !searchBlank,
                        previousItemCount = previousRecordCount,
                        currentItemCount = records.size,
                    )
                    previousRecordCount = records.size

                    val notSearching = searchBlank
                    // See MAX_CATCH_UP_WIDENS above: an empty page here doesn't yet mean "no
                    // data" - the auto-widen loop below may still be about to look further
                    // back. Deciding showAddWidgetButton from this page alone would flash the
                    // "add a widget" empty state on every cold start whose first (most recent)
                    // page happens to be empty, only for it to be replaced moments later once
                    // an older page turns up real records - which reads as the app being
                    // broken on every launch after any inactivity.
                    val willAutoWiden = records.isEmpty() && notSearching && autoCatchUpWidens < MAX_CATCH_UP_WIDENS

                    _viewState.update {
                        when {
                            records.isNotEmpty() -> it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                // Reset explicitly - once records exist there's no "add a
                                // widget" empty state to show, but a prior empty page (e.g.
                                // from the auto-widen catch-up loop below finding nothing
                                // until it looks back far enough) could have left this true,
                                // and it would otherwise never get cleared since it isn't
                                // touched anywhere else once data starts flowing. That stuck
                                // "true" also permanently hid the Search FAB, which is gated
                                // on this same flag.
                                showAddWidgetButton = false,
                                showSettingsButton = notSearching,
                                showSubscribeBanner = notSearching && showSubscribeBanner,
                            )

                            willAutoWiden -> it.copy(
                                // Still probing further back - leave showAddWidgetButton (and
                                // the rest of the empty-state flags) as they were rather than
                                // flashing the empty state only to immediately replace it.
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                            )

                            else -> it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                showAddWidgetButton = notSearching,
                                showSettingsButton = false,
                                showSubscribeBanner = false,
                            )
                        }
                    }

                    // Launched as a separate coroutine rather than calling resubscribe
                    // directly, since this callback is itself running inside collectionJob -
                    // cancelling it from within its own body would be reentrant.
                    if (willAutoWiden) {
                        autoCatchUpWidens++
                        sinceEpochMillis -= PAGE_SIZE.inWholeMilliseconds
                        viewModelScope.launch { resubscribe(currentSearch) }
                    } else if (records.isNotEmpty()) {
                        autoCatchUpWidens = 0
                    }

                    attemptPendingScroll()
                }
        }
    }

    /**
     * Called when the user taps a Trends chart point (see TrendsScreen.kt /
     * OVERVIEW_SCROLL_TO_EPOCH_DAY_KEY): widens the paging window if [epochDay] predates it,
     * then asks the view to scroll to the closest matching day card once it's loaded (see
     * [attemptPendingScroll]).
     */
    fun onScrollToDateRequested(epochDay: Long) {
        pendingScrollToEpochDay = epochDay
        val targetDate = LocalDate.ofEpochDay(epochDay)
        _viewState.update {
            it.copy(pendingScrollDateLabel = targetDate.format(SCROLL_TARGET_DATE_FORMATTER))
        }
        val dayStart = diaryDayStartTime(settingsRepository.getDiaryDayStartHour())
        val targetWindowStartMillis = diaryDayWindowStart(targetDate, dayStart, ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (targetWindowStartMillis < sinceEpochMillis) {
            sinceEpochMillis = targetWindowStartMillis - PAGE_SIZE.inWholeMilliseconds
            resubscribe(currentSearch)
        } else {
            attemptPendingScroll()
        }
    }

    /** Best-effort match against whatever day cards are currently loaded - there's no
     *  guarantee the exact target day itself has a card (e.g. it wasn't logged), so this
     *  picks the closest one rather than requiring an exact hit. */
    private fun attemptPendingScroll() {
        val targetEpochDay = pendingScrollToEpochDay ?: return
        val match = _viewState.value.items
            .filterIsInstance<ListUiModelDailySummary>()
            .minByOrNull { abs(it.day.toEpochDay() - targetEpochDay) }
            ?: return
        pendingScrollToEpochDay = null
        _viewState.update { it.copy(scrollToListItemId = match.listItemId) }
    }

    fun onScrollHandled() {
        _viewState.update { it.copy(scrollToListItemId = null, pendingScrollDateLabel = null) }
    }

    private suspend fun enrichRecordRowsWithOtherVariantsIcon(items: List<ListUiModelBase>): List<ListUiModelBase> =
        items.map { item ->
            when (item) {
                is ListUiModelRecord -> {
                    val show = listMealVariantsForTemplateUseCase.hasOtherVariants(item.templateId)
                    item.copy(showOtherLoggedVariantsIcon = show)
                }

                else -> item
            }
        }

    fun onRepeatMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            val templateId = recordsRepository.get(recordId)?.template?.dbId ?: return@launch
            createRecordFromTemplateUseCase.execute(templateId, ZonedDateTime.now(ZoneId.systemDefault()))
        }
    }

    fun onAnalyseMacrosMenuItemTapped(recordId: Long) {
        NutrientAnalysisWorker.setWorkRequest(
            appContext = application,
            recordId = recordId,
            force = true,
            wifiOnly = false,
        )
    }

    fun onDeleteMenuItemTapped(recordId: Long) {
        viewModelScope.launch {
            val oldRecord = recordsRepository.deleteRecord(recordId = recordId)
            _viewState.update {
                it.copy(
                    showUndoDeleteSnackbar = true,
                    recordToUndelete = oldRecord,
                )
            }
            cancelMacrosAnalysisForRecord.execute(recordId)
        }
    }

    fun onRecordImageTapped(recordId: Long) {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.ViewImage(recordId))
        }
    }

    fun onRecordBodyTapped(recordId: Long) {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.EditRecord(recordId))
        }
    }

    fun onUndoDeleteTapped() {
        viewModelScope.launch {
            recordsRepository.updateRecord(_viewState.value.recordToUndelete!!)
        }
        _viewState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
    }

    fun onUndoDeleteDismissed() {
        deleteUnusedTemplateAfterUndo(_viewState.value.recordToUndelete!!.template.dbId)
        _viewState.update {
            it.copy(
                recordToUndelete = null,
            )
        }
    }

    fun onSettingsButtonTapped() {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.OpenSettingsScreen)
        }
    }

    /** Tapping a day card opens Trends in the Days view, pre-scrolled to that day. */
    fun onDailySummaryTapped(epochDay: Long) {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.OpenTrendsScreen(scrollToEpochDay = epochDay, timescale = "DAYS"))
        }
    }

    /** Tapping a week card opens Trends in the Weeks view, pre-scrolled to that week. */
    fun onWeeklySummaryTapped(epochDay: Long) {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.OpenTrendsScreen(scrollToEpochDay = epochDay, timescale = "WEEKS"))
        }
    }

    fun onSubscribeBannerTapped() {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.OpenPaywallScreen)
        }
    }

    fun onSubscribeBannerDismissed() {
        settingsRepository.setSubscribeBannerDismissed(true)
        _viewState.update { it.copy(showSubscribeBanner = false) }
    }

    private fun deleteUnusedTemplateAfterUndo(templateId: Long) {
        viewModelScope.launch {
            deleteUnusedTemplateIfOrphaned.execute(templateId)
        }
    }

    fun onUndoDeleteSnackbarShown() {
        _viewState.update {
            it.copy(
                showUndoDeleteSnackbar = false
            )
        }
    }

    fun finalizePendingUndos() {
        _viewState.value.recordToUndelete?.let {
            deleteUnusedTemplateAfterUndo(it.template.dbId)
        }
    }
}
