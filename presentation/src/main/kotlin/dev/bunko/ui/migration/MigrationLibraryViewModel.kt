/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.migration

import androidx.compose.runtime.Immutable
import dev.bunko.domain.ServerListeners
import dev.bunko.domain.library.interactor.GetLibraryManga
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.source.interactor.GetSourceList
import dev.bunko.domain.source.model.Source
import dev.bunko.ui.base.userMessage
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Immutable
data class MigrationSource(
    val sourceId: Long,
    val source: Source?,
    val manga: ImmutableList<Manga>,
) {
    val name: String get() = source?.displayName ?: sourceId.toString()
}

sealed interface MigrationLibraryState {
    data object Loading : MigrationLibraryState

    data class Failed(val error: Throwable) : MigrationLibraryState

    data class Loaded(val sources: ImmutableList<MigrationSource>) : MigrationLibraryState
}

@OptIn(FlowPreview::class)
@Inject
class MigrationLibraryViewModel(
    private val getLibraryManga: GetLibraryManga,
    private val getSourceList: GetSourceList,
    serverListeners: ServerListeners,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    private val _state = MutableStateFlow<MigrationLibraryState>(MigrationLibraryState.Loading)
    val state = _state.asStateFlow()

    private val listeners = merge(serverListeners.libraryUpdated, serverListeners.mangaListener)
    private var started = false
    private var loadJob: Job? = null

    fun start() {
        if (started) return
        started = true
        load()
        listeners
            .debounce(RELOAD_DEBOUNCE_MS)
            .onEach { load(quiet = true) }
            .launchIn(scope)
    }

    fun load(quiet: Boolean = false) {
        if (!quiet) _state.value = MigrationLibraryState.Loading
        loadJob?.cancel()
        loadJob = scope.launch {
            var error: Throwable? = null
            val library = getLibraryManga.await(onError = { error = it })
            if (library == null) {
                if (!quiet || _state.value !is MigrationLibraryState.Loaded) {
                    _state.value = MigrationLibraryState.Failed(error ?: IllegalStateException())
                } else {
                    error?.let { toast(it.userMessage.toPlatformString()) }
                }
                return@launch
            }
            val sources = getSourceList.await().orEmpty().associateBy { it.id }
            _state.value = MigrationLibraryState.Loaded(
                library.groupBy { it.sourceId }
                    .map { (id, manga) ->
                        MigrationSource(id, sources[id], manga.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }).toImmutableList())
                    }
                    .sortedWith(compareBy<MigrationSource> { it.source == null }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                    .toImmutableList(),
            )
        }
    }

    private companion object {
        private const val RELOAD_DEBOUNCE_MS = 1_000L
    }
}
