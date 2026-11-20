/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.sources.browse.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bunko.i18n.MR
import dev.bunko.ui.sources.browse.filter.model.SourceFiltersView
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList

private const val CHIPS_PER_ITEM = 48
private const val SEARCHABLE_GROUP_SIZE = 12

@Composable
fun SourceFiltersSheet(
    filters: ImmutableList<SourceFiltersView<*, *>>,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    val sections = remember { FilterSections() }
    Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onReset) { Text(stringResource(MR.strings.reset_filters)) }
            Spacer(Modifier.weight(1f))
            Button(onApply) { Text(stringResource(MR.strings.action_filter)) }
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(vertical = 8.dp)) {
            filterItems(filters, "", sections)
        }
    }
}

@Stable
private class FilterSections {
    private val expanded = mutableStateMapOf<String, Boolean>()
    private val queries = mutableStateMapOf<String, String>()

    fun isExpanded(key: String) = expanded[key] == true

    fun toggle(key: String) {
        expanded[key] = !isExpanded(key)
    }

    fun query(key: String) = queries[key].orEmpty()

    fun setQuery(
        key: String,
        query: String,
    ) {
        queries[key] = query
    }
}

private fun LazyListScope.filterItems(
    views: List<SourceFiltersView<*, *>>,
    prefix: String,
    sections: FilterSections,
) {
    views.segments().forEach { segment ->
        val head = segment.first()
        val key = "$prefix/${head.index}"
        when (head) {
            is SourceFiltersView.CheckBox, is SourceFiltersView.TriState ->
                segment.chunked(CHIPS_PER_ITEM).forEachIndexed { chunk, chips ->
                    item(key = "$key/$chunk") { FilterChips(chips, Modifier.animateItem()) }
                }
            is SourceFiltersView.Group -> groupItems(head, key, sections)
            is SourceFiltersView.Sort -> sortItems(head, key, sections)
            is SourceFiltersView.Header -> item(key = key) { FilterHeading(head.name, Modifier.animateItem()) }
            is SourceFiltersView.Separator -> item(key = key) { FilterDivider(Modifier.animateItem()) }
            is SourceFiltersView.Select -> item(key = key) { SelectFilter(head, Modifier.animateItem()) }
            is SourceFiltersView.Text -> item(key = key) { TextFilter(head, Modifier.animateItem()) }
        }
    }
}

private fun LazyListScope.groupItems(
    group: SourceFiltersView.Group,
    key: String,
    sections: FilterSections,
) {
    val children = group.state.value
    val expanded = sections.isExpanded(key)
    item(key = key) {
        FilterSectionHeader(group.name, children, expanded, { sections.toggle(key) }, Modifier.animateItem())
    }
    if (!expanded) return
    if (children.size < SEARCHABLE_GROUP_SIZE) return filterItems(children, key, sections)
    val query = sections.query(key)
    item(key = "$key/search") {
        FilterSearchField(query, { sections.setQuery(key, it) }, Modifier.animateItem())
    }
    filterItems(children.filter { it.name.contains(query, ignoreCase = true) }, key, sections)
}

private fun LazyListScope.sortItems(
    sort: SourceFiltersView.Sort,
    key: String,
    sections: FilterSections,
) {
    val expanded = sections.isExpanded(key)
    item(key = key) {
        FilterSectionHeader(sort.name, listOf(sort), expanded, { sections.toggle(key) }, Modifier.animateItem())
    }
    if (!expanded) return
    itemsIndexed(sort.filter.values, key = { index, _ -> "$key/$index" }) { index, name ->
        SortOption(sort, index, name, Modifier.animateItem())
    }
}

private fun SourceFiltersView<*, *>.isChip() = this is SourceFiltersView.CheckBox || this is SourceFiltersView.TriState

private fun List<SourceFiltersView<*, *>>.segments(): List<List<SourceFiltersView<*, *>>> =
    fold(mutableListOf<MutableList<SourceFiltersView<*, *>>>()) { segments, view ->
        val last = segments.lastOrNull()
        if (view.isChip() && last != null && last.first().isChip()) last += view else segments += mutableListOf(view)
        segments
    }
