/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.schedule

import com.diamondedge.logging.logging
import dev.bunko.domain.category.interactor.GetCategories
import dev.bunko.domain.category.model.Category
import dev.bunko.domain.schedule.GetReleaseSchedule
import dev.bunko.domain.schedule.ScheduledSeries
import dev.bunko.ui.base.today
import dev.bunko.ui.base.userMessage
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock

@Inject
class ScheduleViewModel(
    private val getReleaseSchedule: GetReleaseSchedule,
    private val getCategories: GetCategories,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    sealed interface State {
        data object Loading : State

        data class Failed(val message: String) : State

        data class Loaded(
            val sections: List<ScheduleSection>,
            val categories: List<CategoryChip>,
            val selectedCategory: Long?,
            val week: List<WeekDay>,
            val selectedDay: LocalDate?,
            val expanded: Set<ScheduleGroup>,
        ) : State
    }

    private sealed interface Source {
        data object Loading : Source

        data class Failed(val message: String) : Source

        data class Loaded(val series: List<ScheduledSeries>, val categories: List<Category>) : Source
    }

    private val source = MutableStateFlow<Source>(Source.Loading)
    private val selectedCategory = MutableStateFlow<Long?>(null)
    private val selectedDay = MutableStateFlow<LocalDate?>(null)
    private val expanded = MutableStateFlow(setOf(ScheduleGroup.DueSoon, ScheduleGroup.NextWeek, ScheduleGroup.Later))

    val state: StateFlow<State> = combine(source, selectedCategory, selectedDay, expanded, ::present)
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, State.Loading)

    init {
        load()
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun load() {
        source.value = Source.Loading
        scope.launch { fetch() }
    }

    /** Reloads while keeping the current list on screen. */
    fun refresh() {
        if (_isRefreshing.value) return
        scope.launch {
            _isRefreshing.value = true
            fetch()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetch() =
        coroutineScope {
            val categories = async { getCategories.await().orEmpty() }
            getReleaseSchedule.await()
                .onSuccess { source.value = Source.Loaded(it, categories.await()) }
                .onFailure {
                    log.warn(it) { "Release schedule failed to load" }
                    source.value = Source.Failed(it.userMessage.toPlatformString())
                }
        }

    fun selectCategory(id: Long?) {
        selectedCategory.value = id
    }

    fun selectDay(day: LocalDate) {
        selectedDay.value = day.takeUnless { it == selectedDay.value }
    }

    fun toggle(group: ScheduleGroup) {
        expanded.value = if (group in expanded.value) expanded.value - group else expanded.value + group
    }

    private fun present(
        source: Source,
        category: Long?,
        day: LocalDate?,
        expanded: Set<ScheduleGroup>,
    ): State =
        when (source) {
            Source.Loading -> State.Loading
            is Source.Failed -> State.Failed(source.message)
            is Source.Loaded -> {
                val now = Clock.System.now().toEpochMilliseconds()
                val inCategory = source.series.inCategory(category)
                val shown = if (day == null) inCategory else inCategory.filter { it.expectedOn() == day }
                State.Loaded(
                    sections = shown.toSections(now),
                    categories = categoryChips(source.series, source.categories),
                    selectedCategory = category,
                    week = inCategory.weekStrip(today()),
                    selectedDay = day,
                    expanded = expanded,
                )
            }
        }

    private companion object {
        val log = logging()
    }
}
