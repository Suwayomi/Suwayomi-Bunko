/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.statistics

import androidx.compose.runtime.Immutable
import dev.bunko.domain.library.interactor.GetLibraryStatistics
import dev.bunko.domain.library.model.LibraryStatistics
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Immutable
sealed interface StatisticsState {
    data object Loading : StatisticsState

    data class Failed(
        val e: Throwable,
    ) : StatisticsState

    data class Loaded(
        val statistics: LibraryStatistics,
    ) : StatisticsState
}

@Inject
class StatisticsScreenViewModel(
    private val getLibraryStatistics: GetLibraryStatistics,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    private val _state = MutableStateFlow<StatisticsState>(StatisticsState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = StatisticsState.Loading
        scope.launch {
            var error: Throwable? = null
            val statistics = getLibraryStatistics.await(onError = { error = it })
            _state.value = when {
                statistics != null -> StatisticsState.Loaded(statistics)
                else -> StatisticsState.Failed(error ?: IllegalStateException())
            }
        }
    }
}
