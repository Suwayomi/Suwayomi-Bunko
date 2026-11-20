/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.filter.model

import dev.bunko.domain.source.model.sourcefilters.SourceFilter
import dev.bunko.domain.source.model.sourcefilters.SourceFilter.TriState.TriStateValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

fun SourceFiltersView<*, *>.toSourceFilter(): SourceFilter =
    when (this) {
        is SourceFiltersView.CheckBox -> filter.copy(value = state.value)
        is SourceFiltersView.Group -> filter.copy(value = state.value.map { it.toSourceFilter() })
        is SourceFiltersView.Header -> filter
        is SourceFiltersView.Select -> filter.copy(value = state.value)
        is SourceFiltersView.Separator -> filter
        is SourceFiltersView.Sort -> filter.copy(value = state.value)
        is SourceFiltersView.Text -> filter.copy(value = state.value)
        is SourceFiltersView.TriState -> filter.copy(value = state.value)
    }

fun SourceFilter.activeCount(): Int =
    when (this) {
        is SourceFilter.Checkbox -> (value != default).toInt()
        is SourceFilter.Group -> value.sumOf { it.activeCount() }
        is SourceFilter.Header, is SourceFilter.Separator -> 0
        is SourceFilter.Select -> (value != default).toInt()
        is SourceFilter.Sort -> (value != default).toInt()
        is SourceFilter.Text -> (value != default).toInt()
        is SourceFilter.TriState -> (value != default).toInt()
    }

fun List<SourceFiltersView<*, *>>.activeCountFlow(): Flow<Int> {
    val leaves = leaves()
    if (leaves.isEmpty()) return flowOf(0)
    return combine(leaves.map { it.state }) { leaves.sumOf { it.toSourceFilter().activeCount() } }
}

fun TriStateValue.next(): TriStateValue =
    when (this) {
        TriStateValue.IGNORE -> TriStateValue.INCLUDE
        TriStateValue.INCLUDE -> TriStateValue.EXCLUDE
        TriStateValue.EXCLUDE -> TriStateValue.IGNORE
    }

private fun List<SourceFiltersView<*, *>>.leaves(): List<SourceFiltersView<*, *>> =
    flatMap {
        when (it) {
            is SourceFiltersView.Group -> it.state.value.leaves()
            is SourceFiltersView.Header, is SourceFiltersView.Separator -> emptyList()
            else -> listOf(it)
        }
    }

private fun Boolean.toInt() = if (this) 1 else 0
