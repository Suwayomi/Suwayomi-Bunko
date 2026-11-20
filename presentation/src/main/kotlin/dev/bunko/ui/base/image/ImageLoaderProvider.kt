/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.image

import android.os.Build
import androidx.compose.ui.graphics.ImageBitmapConfig
import dev.bunko.domain.download.model.DownloadManga
import dev.bunko.domain.extension.model.Extension
import dev.bunko.domain.manga.model.Manga
import dev.bunko.domain.server.Http
import dev.bunko.domain.server.HttpNoAuth
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.domain.source.model.Source
import dev.bunko.ui.base.ImageCache
import dev.bunko.uicore.vm.ContextWrapper
import com.seiko.imageloader.Bitmap
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.disk.DiskCacheBuilder
import com.seiko.imageloader.cache.memory.MemoryCacheBuilder
import com.seiko.imageloader.cache.memory.MemoryKey
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.ComponentRegistryBuilder
import com.seiko.imageloader.component.fetcher.MokoResourceFetcher
import com.seiko.imageloader.component.keyer.Keyer
import com.seiko.imageloader.component.mapper.Mapper
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.intercept.bitmapMemoryCacheConfig
import com.seiko.imageloader.option.Options
import com.seiko.imageloader.option.OptionsBuilder
import com.seiko.imageloader.option.androidContext
import io.ktor.http.Url
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import me.tatarka.inject.annotations.Inject
import okio.Path.Companion.toOkioPath

fun mangaCoverKey(manga: Manga): String = "${manga.id}-${manga.thumbnailUrl}-${manga.thumbnailUrlLastFetched}"

fun mangaCoverUrl(
    serverUrl: Url,
    manga: Manga,
): Url? = manga.thumbnailUrl?.takeUnless { it.isBlank() }?.let { Url(serverUrl.toString() + it) }

@Inject
class ImageLoaderProvider(
    private val http: Http,
    private val httpNoAuth: HttpNoAuth,
    serverPreferences: ServerPreferences,
    private val context: ContextWrapper,
) {
    @OptIn(DelicateCoroutinesApi::class)
    val serverUrl = serverPreferences.serverUrl().stateIn(GlobalScope)

    fun get(imageCache: ImageCache): ImageLoader =
        ImageLoader {
            components {
                add(KtorUrlFetcher.Factory { http.value })
                add(KtorUrlFetcher.ExternalFactory { httpNoAuth.value })
                register(context, http)
                add(MokoResourceFetcher.Factory())
                add(MangaCoverMapper())
                add(MangaCoverKeyer())
                add(DownloadMangaCoverMapper())
                add(DownloadMangaCoverKeyer())
                add(ExtensionIconMapper())
                add(ExtensionIconKeyer())
                add(SourceIconMapper())
                add(SourceIconKeyer())
            }
            options {
                configure(context)
            }
            interceptor {
                diskCache { imageCache }
                bitmapMemoryCacheConfig { configure(context) }
            }
        }

    inner class MangaCoverMapper : Mapper<Url> {
        override fun map(
            data: Any,
            options: Options,
        ): Url? {
            if (data !is Manga) return null
            return mangaCoverUrl(serverUrl.value, data)
        }
    }

    class MangaCoverKeyer : Keyer {
        override fun key(
            data: Any,
            options: Options,
        ): String? {
            if (data !is Manga) return null
            return mangaCoverKey(data)
        }
    }

    inner class DownloadMangaCoverMapper : Mapper<Url> {
        override fun map(
            data: Any,
            options: Options,
        ): Url? {
            if (data !is DownloadManga) return null
            if (data.thumbnailUrl.isNullOrBlank()) return null
            return Url(serverUrl.value.toString() + data.thumbnailUrl)
        }
    }

    class DownloadMangaCoverKeyer : Keyer {
        override fun key(
            data: Any,
            options: Options,
        ): String? {
            if (data !is DownloadManga) return null
            return "${data.id}-${data.thumbnailUrl}-${data.thumbnailUrlLastFetched}"
        }
    }

    inner class ExtensionIconMapper : Mapper<Url> {
        override fun map(
            data: Any,
            options: Options,
        ): Url? {
            if (data !is Extension) return null
            if (data.iconUrl.isBlank()) return null
            return Url("${serverUrl.value}${data.iconUrl}")
        }
    }

    class ExtensionIconKeyer : Keyer {
        override fun key(
            data: Any,
            options: Options,
        ): String? {
            if (data !is Extension) return null
            return data.iconUrl
        }
    }

    inner class SourceIconMapper : Mapper<Url> {
        override fun map(
            data: Any,
            options: Options,
        ): Url? {
            if (data !is Source) return null
            if (data.iconUrl.isBlank()) return null
            return Url(serverUrl.value.toString() + data.iconUrl)
        }
    }

    class SourceIconKeyer : Keyer {
        override fun key(
            data: Any,
            options: Options,
        ): String? {
            if (data !is Source) return null
            return data.iconUrl
        }
    }
}

fun OptionsBuilder.configure(contextWrapper: ContextWrapper) {
    imageBitmapConfig = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        ImageBitmapConfig.Argb8888
    } else {
        ImageBitmapConfig.Gpu
    }
    androidContext(contextWrapper)
}

fun ComponentRegistryBuilder.register(
    contextWrapper: ContextWrapper,
    http: Http,
) {
    setupDefaultComponents(contextWrapper, httpClient = { http.value })
}

fun DiskCacheBuilder.configure(
    contextWrapper: ContextWrapper,
    cacheDir: String,
) {
    directory(contextWrapper.cacheDir.toOkioPath() / cacheDir)
    maxSizeBytes(if (cacheDir == CHAPTER_CACHE_DIR) CHAPTER_CACHE_BYTES else IMAGE_CACHE_BYTES)
}

fun MemoryCacheBuilder<MemoryKey, Bitmap>.configure(contextWrapper: ContextWrapper) {
    maxSizePercent(contextWrapper, MEMORY_CACHE_PERCENT)
}

private const val CHAPTER_CACHE_DIR = "chapter_cache"
private const val CHAPTER_CACHE_BYTES = 200L * 1024 * 1024
private const val IMAGE_CACHE_BYTES = 50L * 1024 * 1024
private const val MEMORY_CACHE_PERCENT = 0.25
