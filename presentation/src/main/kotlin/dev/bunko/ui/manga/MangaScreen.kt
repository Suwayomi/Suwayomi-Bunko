/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import dev.bunko.ui.manga.components.ChapterSettingsActions
import dev.bunko.ui.manga.components.MangaScreenContent
import dev.bunko.ui.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey

class MangaScreen(
    private val mangaId: Long,
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel {
            mangaViewModel(MangaScreenViewModel.Params(mangaId))
        }

        val manga = vm.manga.collectAsState().value
        CoverAccentTheme(mangaId, manga, vm.coverAccents) { onCoverBitmap ->
            MangaScreenContent(
                onCoverBitmap = onCoverBitmap,
                isLoading = vm.isLoading.collectAsState().value,
                isRefreshing = vm.isRefreshing.collectAsState().value,
                manga = manga,
                chapters = vm.chapters.collectAsState().value,
                displayChapters = vm.displayChapters.collectAsState().value,
                chapterOptions = vm.chapterOptions.collectAsState().value,
                chapterOptionsCustomised = vm.chapterOptionsCustomised.collectAsState().value,
                scanlators = vm.scanlators.collectAsState().value,
                missingChapterCount = vm.missingChapterCount.collectAsState().value,
                resumeChapter = vm.resumeChapter.collectAsState().value,
                chapterSettingsActions = remember(vm) {
                    ChapterSettingsActions(
                        setUnread = vm::setUnreadFilter,
                        setDownloaded = vm::setDownloadedFilter,
                        setBookmarked = vm::setBookmarkedFilter,
                        setScanlatorShown = vm::setScanlatorShown,
                        showAllScanlators = vm::showAllScanlators,
                        setSort = vm::setSort,
                        setDisplay = vm::setDisplay,
                        setAsDefault = vm::setOptionsAsDefault,
                        reset = vm::resetOptions,
                    )
                },
                dateTimeFormatter = vm.dateTimeFormatter.collectAsState().value,
                categoriesExist = vm.categoriesExist.collectAsState().value,
                chooseCategoriesFlowHolder = vm.chooseCategoriesFlowHolder,
                availableCategories = vm.categories.collectAsState().value,
                mangaCategories = vm.mangaCategories.collectAsState().value,
                inActionMode = vm.inActionMode.collectAsState().value,
                selectedItems = vm.selectedItems.collectAsState().value,
                addFavorite = vm::addFavorite,
                setCategories = vm::setCategories,
                toggleFavorite = vm::toggleFavorite,
                refreshManga = vm::refreshManga,
                downloadNext = vm::downloadNext,
                downloadUnread = vm::downloadUnread,
                downloadAll = vm::downloadAll,
                markRead = vm::markRead,
                markUnread = vm::markUnread,
                bookmarkChapter = vm::bookmarkChapter,
                unBookmarkChapter = vm::unBookmarkChapter,
                markPreviousRead = vm::markPreviousRead,
                downloadChapter = vm::downloadChapter,
                deleteDownload = vm::deleteDownload,
                stopDownloadingChapter = vm::stopDownloadingChapter,
                onSelectChapter = vm::selectChapter,
                onLongClickChapter = vm::longPressChapter,
                onUnselectChapter = vm::unselectChapter,
                selectAll = vm::selectAll,
                invertSelection = vm::invertSelection,
                clearSelection = vm::clearSelection,
                downloadChapters = vm::downloadChapters,
                offlineStates = vm.offlineStates.collectAsState().value,
                saveOffline = vm::saveOffline,
                removeOffline = vm::removeOffline,
                saveUnreadOffline = vm::saveUnreadOffline,
                loadChapters = vm::loadChapters,
                loadManga = vm::loadManga,
                tracking = vm.tracking,
            )
        }
    }
}
