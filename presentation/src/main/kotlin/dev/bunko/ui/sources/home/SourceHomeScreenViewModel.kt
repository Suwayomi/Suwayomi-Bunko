/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.home

import dev.bunko.ui.base.userMessage
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.intl.Locale
import dev.bunko.core.lang.displayName
import dev.bunko.domain.ServerListeners
import dev.bunko.domain.source.interactor.GetSourceList
import dev.bunko.domain.source.model.Source
import dev.bunko.domain.source.service.CatalogPreferences
import dev.bunko.i18n.MR
import androidx.lifecycle.SavedStateHandle
import dev.bunko.ui.base.state.getStateFlow
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class SourceHomeScreenViewModel(
    private val getSourceList: GetSourceList,
    private val serverListeners: ServerListeners,
    catalogPreferences: CatalogPreferences,
    contextWrapper: ContextWrapper,
    @Assisted private val savedStateHandle: SavedStateHandle,
) : ViewModel(contextWrapper) {
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val installedSources = MutableStateFlow(emptyList<Source>())

    private val _languages = catalogPreferences.languages().asStateFlow()
    val languages = _languages.asStateFlow()
        .map { it.toImmutableSet() }
        .stateIn(scope, SharingStarted.Eagerly, persistentSetOf())

    private val _pinned = catalogPreferences.pinnedSources().asStateFlow()

    val sources = combine(installedSources, languages, _pinned) { installedSources, languages, pinnedIds ->
        val all = MR.strings.all.toPlatformString()
        val other = MR.strings.other.toPlatformString()
        val visible = installedSources
            .distinctBy { it.id }
            .filter {
                it.lang in languages || it.id == Source.LOCAL_SOURCE_ID
            }
        val (pinned, unpinned) = visible.partition { it.id.toString() in pinnedIds }
        val byName = compareBy<Source, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
        val pinnedItems = if (pinned.isEmpty()) {
            emptyList()
        } else {
            listOf(SourceUI.Header(MR.strings.pinned.toPlatformString())) +
                pinned.sortedWith(byName).map { SourceUI.SourceItem(it, isPinned = true) }
        }
        unpinned
            .groupBy(Source::displayLang)
            .mapValues {
                it.value.sortedWith(byName)
                    .map { source -> SourceUI.SourceItem(source) }
            }
            .mapKeys { (key) ->
                when (key) {
                    "all" -> all
                    "other" -> other
                    else -> Locale(key).displayName
                }
            }
            .toList()
            .sortedWith(
                compareBy<Pair<String, *>> { (key) ->
                    when (key) {
                        all -> 1
                        other -> 3
                        else -> 2
                    }
                }.thenBy(String.CASE_INSENSITIVE_ORDER, Pair<String, *>::first),
            )
            .flatMap { (key, value) ->
                listOf(SourceUI.Header(key)) + value
            }
            .let { pinnedItems + it }
            .toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    val sourceLanguages = installedSources.map { sources ->
        sources.map { it.lang }.distinct().minus(Source.LOCAL_SOURCE_LANG)
            .toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    private val _query by savedStateHandle.getStateFlow { "" }
    val query = _query.asStateFlow()

    init {
        getSources()
    }

    private fun getSources() {
        serverListeners.combineSources(
            getSourceList.asFlow()
                .onEach {
                    installedSources.value = it
                    _isLoading.value = false
                }
                .catch {
                    toast(it.userMessage.toPlatformString())
                    log.warn(it) { "Error getting sources" }
                    _isLoading.value = false
                },
        )
            .launchIn(scope)
    }

    fun setEnabledLanguages(langs: Set<String>) {
        log.info { langs }
        _languages.value = langs
    }

    fun togglePinned(source: Source) {
        val id = source.id.toString()
        _pinned.value = if (id in _pinned.value) _pinned.value - id else _pinned.value + id
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    private companion object {
        private val log = logging()
    }
}

@Stable
sealed class SourceUI {
    @Stable
    data class Header(
        val header: String,
    ) : SourceUI()

    @Stable
    data class SourceItem(
        val source: Source,
        val isPinned: Boolean = false,
    ) : SourceUI()
}
