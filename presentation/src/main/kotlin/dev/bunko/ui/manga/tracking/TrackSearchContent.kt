/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.bunko.domain.track.model.TrackSearch
import dev.bunko.i18n.MR
import dev.bunko.ui.base.image.ExternalImage
import dev.bunko.uicore.components.EmptyScreen
import dev.bunko.uicore.image.ImageLoaderImage
import dev.bunko.uicore.resources.stringResource
import dev.icerock.moko.resources.StringResource
import io.ktor.http.Url

@Composable
internal fun TrackSearchContent(
    search: TrackSearchUi,
    onSearch: (String) -> Unit,
    onClose: () -> Unit,
    onBind: (TrackSearch) -> Unit,
) {
    var query by remember(search.tracker.id) { mutableStateOf(search.query) }
    var selected by remember(search.results) { mutableStateOf<TrackSearch?>(null) }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(MR.strings.track_search_hint, search.tracker.name)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = null) } }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (val results = search.results) {
                SearchResults.Searching -> CircularProgressIndicator()

                is SearchResults.Failed -> EmptyScreen(
                    message = stringResource(results.message),
                    icon = Icons.Outlined.ErrorOutline,
                    actionLabel = stringResource(MR.strings.action_retry),
                    onAction = { onSearch(query) },
                )

                is SearchResults.Loaded -> if (results.results.isEmpty()) {
                    EmptyScreen(stringResource(MR.strings.track_search_empty), icon = Icons.Outlined.SearchOff)
                } else {
                    LazyColumn(
                        Modifier.fillMaxHeight(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(results.results, key = { it.remoteId }) { result ->
                            SearchResultItem(result, result == selected) { selected = result }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = selected != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Button(
                onClick = { selected?.let(onBind) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(stringResource(MR.strings.track_select))
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: TrackSearch,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultCover(result.coverUrl, selected)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Detail(MR.strings.track_search_type, result.publishingType)
                Detail(MR.strings.track_search_status, result.publishingStatus)
                Detail(MR.strings.track_search_started, result.startDate)
                Detail(MR.strings.track_chapters, result.totalChapters.takeIf { it > 0 }?.toString().orEmpty())
                if (result.summary.isNotBlank()) {
                    Text(
                        result.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCover(
    coverUrl: String,
    selected: Boolean,
) {
    val cover = remember(coverUrl) { coverUrl.takeIf { it.isNotBlank() }?.let { runCatching { ExternalImage(Url(it)) }.getOrNull() } }
    Box(
        Modifier.size(width = 64.dp, height = 92.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            ImageLoaderImage(
                data = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                onLoading = null,
            )
        } else {
            Icon(Icons.Outlined.ImageNotSupported, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)))
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun Detail(
    label: StringResource,
    value: String,
) {
    if (value.isBlank()) return
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(stringResource(label)) }
            append(": ")
            append(value)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
