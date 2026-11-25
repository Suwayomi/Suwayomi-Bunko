/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.migration

import dev.bunko.domain.manga.interactor.GetManga
import dev.bunko.domain.manga.interactor.MigrateManga
import dev.bunko.domain.manga.interactor.MigrationOptions
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.source.service.CatalogPreferences
import dev.bunko.i18n.MR
import dev.bunko.ui.base.userMessage
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject
class MigrationViewModel(
    private val getManga: GetManga,
    private val migrateManga: MigrateManga,
    catalogPreferences: CatalogPreferences,
    contextWrapper: ContextWrapper,
    @Assisted private val params: Params,
) : ViewModel(contextWrapper) {
    private val _from = MutableStateFlow<Manga?>(null)
    val from = _from.asStateFlow()

    val chapters = catalogPreferences.migrateChapters().asStateFlow()
    val categories = catalogPreferences.migrateCategories().asStateFlow()
    val tracking = catalogPreferences.migrateTracking().asStateFlow()
    val deleteDownloads = catalogPreferences.migrateDeleteDownloads().asStateFlow()

    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    private val _finished = MutableSharedFlow<Manga>()
    val finished = _finished.asSharedFlow()

    init {
        scope.launch { _from.value = getManga.await(params.mangaId, onError = { toast(it.userMessage.toPlatformString()) }) }
    }

    fun migrate(
        target: Manga,
        replace: Boolean,
    ) {
        if (target.id == params.mangaId) {
            toast(MR.strings.migrate_same_entry.toPlatformString())
            return
        }
        if (_running.value) return
        _running.value = true
        scope.launch {
            val from = _from.value ?: getManga.await(params.mangaId)
            if (from == null) {
                toast(MR.strings.migrate_failed.toPlatformString())
                _running.value = false
                return@launch
            }
            val options = MigrationOptions(
                chapters = chapters.value,
                categories = categories.value,
                tracking = tracking.value,
                deleteDownloads = deleteDownloads.value,
            )
            migrateManga.await(from, target, options, replace)
                .onSuccess {
                    toast((if (replace) MR.strings.migrate_done else MR.strings.migrate_copied).toPlatformString(target.title))
                    _finished.emit(target)
                }
                .onFailure { toast(MR.strings.migrate_failed_reason.toPlatformString(it.userMessage.toPlatformString())) }
            _running.value = false
        }
    }

    data class Params(
        val mangaId: Long,
    )
}
