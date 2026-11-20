/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base

import dev.bunko.ui.settings.tracking.TrackerOAuth
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import dev.bunko.core.di.AppScope
import dev.bunko.domain.chapter.interactor.GetChapterPages
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.ui.ViewModelComponent
import dev.bunko.ui.base.offline.OfflineChapterStore
import dev.bunko.ui.base.offline.OfflineChapters
import dev.bunko.ui.base.offline.offlineChaptersRoot
import dev.bunko.ui.base.image.ImageLoaderProvider
import dev.bunko.ui.base.image.configure
import dev.bunko.uicore.vm.ContextWrapper
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.cache.disk.DiskCache
import me.tatarka.inject.annotations.Provides
import okio.FileSystem

typealias ImageCache = DiskCache

typealias ChapterCache = DiskCache

interface UiComponent {
    val imageLoader: ImageLoader

    val contextWrapper: ContextWrapper

    val hooks: Array<ProvidedValue<out Any?>>

    val imageCache: ImageCache

    val chapterCache: ChapterCache

    val offlineChapters: OfflineChapters

    val trackerOAuth: TrackerOAuth

    @AppScope
    @Provides
    fun imageLoaderFactory(
        imageLoaderProvider: ImageLoaderProvider,
        imageCache: ImageCache,
    ): ImageLoader = imageLoaderProvider.get(imageCache)

    @AppScope
    @Provides
    fun imageCacheFactory(): ImageCache =
        DiskCache(FileSystem.SYSTEM) {
            configure(contextWrapper, "image_cache")
        }

    @AppScope
    @Provides
    fun chapterCacheFactory(): ChapterCache =
        DiskCache(FileSystem.SYSTEM) {
            configure(contextWrapper, "chapter_cache")
        }

    @AppScope
    @Provides
    fun offlineChaptersFactory(
        getChapterPages: GetChapterPages,
        http: Http,
        serverPreferences: ServerPreferences,
    ): OfflineChapters =
        OfflineChapters(
            OfflineChapterStore(offlineChaptersRoot(contextWrapper), FileSystem.SYSTEM),
            getChapterPages,
            http,
            serverPreferences,
            contextWrapper,
        )

    @Provides
    fun getHooks(viewModelComponent: ViewModelComponent) =
        arrayOf(
            LocalViewModels provides viewModelComponent,
            LocalImageLoader provides imageLoader,
        )
}

val LocalViewModels =
    compositionLocalOf<ViewModelComponent> { throw IllegalArgumentException("ViewModelComponent not found") }
