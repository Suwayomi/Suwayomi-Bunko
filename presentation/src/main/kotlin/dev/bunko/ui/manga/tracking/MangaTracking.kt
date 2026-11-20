/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.tracking

import androidx.compose.runtime.Immutable
import dev.bunko.domain.track.interactor.Tracking
import dev.bunko.domain.track.model.TrackRecord
import dev.bunko.domain.track.model.TrackSearch
import dev.bunko.domain.track.model.TrackUpdate
import dev.bunko.domain.track.model.Tracker
import dev.bunko.ui.base.userMessage
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class TrackerItem(
    val tracker: Tracker,
    val iconUrl: Any?,
    val record: TrackRecord?,
)

sealed interface TrackingState {
    data object Loading : TrackingState

    data class Failed(val message: StringResource) : TrackingState

    data class Loaded(val items: ImmutableList<TrackerItem>) : TrackingState
}

sealed interface SearchResults {
    data object Searching : SearchResults

    data class Failed(val message: StringResource) : SearchResults

    data class Loaded(val results: ImmutableList<TrackSearch>) : SearchResults
}

@Immutable
data class TrackSearchUi(
    val tracker: Tracker,
    val query: String,
    val results: SearchResults,
)

class MangaTracking(
    private val scope: CoroutineScope,
    private val tracking: Tracking,
    private val mangaId: Long,
    private val serverUrl: () -> String,
    private val onError: (Throwable) -> Unit,
) {
    private val _state = MutableStateFlow<TrackingState>(TrackingState.Loading)
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    val activeCount: StateFlow<Int> = state
        .map { state -> (state as? TrackingState.Loaded)?.items?.count { it.record != null } ?: 0 }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    private val _search = MutableStateFlow<TrackSearchUi?>(null)
    val search: StateFlow<TrackSearchUi?> = _search.asStateFlow()
    private var searchJob: Job? = null

    fun load() {
        scope.launch {
            if (_state.value !is TrackingState.Loaded) _state.value = TrackingState.Loading
            val trackers = tracking.trackers().getOrElse { return@launch fail(it) }
                .filter { it.isLoggedIn }
            val records = tracking.records(mangaId).getOrElse { return@launch fail(it) }
            _state.value = TrackingState.Loaded(
                trackers.map { tracker ->
                    TrackerItem(tracker, trackerIconUrl(serverUrl(), tracker.icon), records.find { it.trackerId == tracker.id })
                }.toImmutableList(),
            )
        }
    }

    fun update(record: TrackRecord, update: TrackUpdate) {
        val optimistic = record.copy(
            status = update.status ?: record.status,
            displayScore = update.score ?: record.displayScore,
            lastChapterRead = update.lastChapterRead ?: record.lastChapterRead,
        )
        setRecord(record.trackerId, optimistic)
        scope.launch {
            tracking.update(record.id, update)
                .onSuccess { setRecord(record.trackerId, it ?: optimistic) }
                .onFailure {
                    setRecord(record.trackerId, record)
                    onError(it)
                }
        }
    }

    fun unbind(record: TrackRecord, deleteRemote: Boolean) {
        setRecord(record.trackerId, null)
        scope.launch {
            tracking.unbind(record.id, deleteRemote).onFailure {
                setRecord(record.trackerId, record)
                onError(it)
            }
        }
    }

    fun openSearch(tracker: Tracker, query: String) {
        _search.value = TrackSearchUi(tracker, query, SearchResults.Searching)
        search(query)
    }

    fun search(query: String) {
        val current = _search.value ?: return
        if (query.isBlank()) return
        _search.value = current.copy(query = query, results = SearchResults.Searching)
        searchJob?.cancel()
        searchJob = scope.launch {
            val results = tracking.search(current.tracker.id, query).fold(
                onSuccess = { SearchResults.Loaded(it.toImmutableList()) },
                onFailure = { SearchResults.Failed(it.userMessage) },
            )
            _search.update { it?.copy(results = results) }
        }
    }

    fun closeSearch() {
        searchJob?.cancel()
        _search.value = null
    }

    fun bind(result: TrackSearch) {
        val tracker = _search.value?.tracker ?: return
        closeSearch()
        scope.launch {
            tracking.bind(mangaId, tracker.id, result.remoteId)
                .onSuccess { setRecord(tracker.id, it) }
                .onFailure(onError)
        }
    }

    private fun setRecord(trackerId: Int, record: TrackRecord?) {
        _state.update { state ->
            if (state !is TrackingState.Loaded) return@update state
            TrackingState.Loaded(
                state.items.map { if (it.tracker.id == trackerId) it.copy(record = record) else it }.toImmutableList(),
            )
        }
    }

    private fun fail(error: Throwable) {
        _state.value = TrackingState.Failed(error.userMessage)
    }
}
