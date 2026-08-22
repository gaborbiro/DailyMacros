package dev.gaborbiro.dailymacros.features.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
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
    }

    private var sinceEpochMillis: Long = System.currentTimeMillis() - PAGE_SIZE.inWholeMilliseconds
    private var currentSearch: String? = null
    private var collectionJob: Job? = null
    private var previousRecordCount: Int = -1
    private var autoCatchUpWidens: Int = 0

    fun onSearchTermChanged(search: String?) {
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
                    Triple(enrichRecordRowsWithOtherVariantsIcon(items), showSubscribeBanner, targets.hasAnyEnabled())
                }
                .collect { (records: List<ListUiModelBase>, showSubscribeBanner: Boolean, hasTargets: Boolean) ->
                    val hasMore = computeHasMoreItems.execute(
                        isSearchActive = !searchBlank,
                        previousItemCount = previousRecordCount,
                        currentItemCount = records.size,
                    )
                    previousRecordCount = records.size

                    val notSearching = searchBlank
                    _viewState.update {
                        if (records.isNotEmpty()) {
                            it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                showSettingsButton = notSearching,
                                showSetTargetsCta = notSearching && !hasTargets,
                                showSubscribeBanner = notSearching && showSubscribeBanner,
                            )
                        } else {
                            it.copy(
                                items = records,
                                isLoadingMore = false,
                                hasMoreData = hasMore,
                                showAddWidgetButton = notSearching,
                                showSettingsButton = false,
                                showSetTargetsCta = false,
                                showSubscribeBanner = false,
                            )
                        }
                    }

                    // See MAX_CATCH_UP_WIDENS above. Deliberately NOT gated on
                    // `hasMore`: computeHasMoreItems already flips that false
                    // after just one more empty page (it's tuned to stop
                    // normal pagination, not to keep searching back through
                    // however many empty weeks/months of inactivity it takes
                    // to find the first real record) - autoCatchUpWidens is
                    // this loop's own, more patient bound instead. Launched
                    // as a separate coroutine rather than calling resubscribe
                    // directly, since this callback is itself running inside
                    // collectionJob - cancelling it from within its own body
                    // would be reentrant.
                    if (records.isEmpty() && notSearching && autoCatchUpWidens < MAX_CATCH_UP_WIDENS) {
                        autoCatchUpWidens++
                        sinceEpochMillis -= PAGE_SIZE.inWholeMilliseconds
                        viewModelScope.launch { resubscribe(currentSearch) }
                    } else if (records.isNotEmpty()) {
                        autoCatchUpWidens = 0
                    }
                }
        }
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

    fun onTrendsButtonTapped() {
        viewModelScope.launch {
            _uiUpdates.emit(OverviewUiUpdates.OpenTrendsScreen)
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
