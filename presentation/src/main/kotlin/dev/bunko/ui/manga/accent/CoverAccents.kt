/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.manga.accent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toArgb
import dev.bunko.core.di.AppScope
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.manga.model.Manga
import dev.bunko.ui.base.ImageCache
import dev.bunko.domain.server.service.ServerPreferences
import dev.bunko.ui.base.image.mangaCoverKey
import dev.bunko.ui.base.image.mangaCoverUrl
import dev.bunko.ui.manga.dominantColor
import dev.bunko.uicore.vm.ContextWrapper
import com.diamondedge.logging.logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject
import okio.FileSystem
import okio.Path.Companion.toOkioPath

@AppScope
@Inject
class CoverAccents(
    private val imageCache: ImageCache,
    private val serverPreferences: ServerPreferences,
    contextWrapper: ContextWrapper,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val file = CoverAccentFile(contextWrapper.filesDir.toOkioPath() / ACCENT_FILE, FileSystem.SYSTEM)
    private val _accents = MutableStateFlow<Map<Long, CachedAccent>>(emptyMap())
    internal val accents: StateFlow<Map<Long, CachedAccent>> = _accents.asStateFlow()
    private val requests = Channel<List<Manga>>(Channel.UNLIMITED)

    init {
        scope.launch {
            val stored = runCatching { file.load() }.throwIfCancellation()
                .onFailure { log.warn(it) { "Cover accents not loaded" } }
                .getOrDefault(emptyMap())
            _accents.update { stored + it }
            launch { sampleLoop() }
            _accents.drop(1).collect { persist(it) }
        }
    }

    fun precache(manga: List<Manga>) {
        requests.trySend(manga)
    }

    internal fun isCurrent(manga: Manga): Boolean = _accents.value[manga.id]?.coverHash == coverHash(manga)

    internal fun record(
        manga: Manga,
        bitmap: Bitmap,
    ) {
        put(manga, dominantColor(bitmap)?.toArgb())
    }

    private fun put(
        manga: Manga,
        argb: Int?,
    ) {
        _accents.update { it + (manga.id to CachedAccent(coverHash(manga), argb)) }
    }

    private suspend fun sampleLoop() {
        val waiting = LinkedHashMap<Long, Manga>()
        while (true) {
            val batch = if (waiting.isEmpty()) requests.receive() else withTimeoutOrNull(RETRY_MS) { requests.receive() }
            batch?.forEach { waiting[it.id] = it }
            waiting.values.toList().forEach { if (sampleCached(it)) waiting.remove(it.id) }
        }
    }

    private suspend fun sampleCached(manga: Manga): Boolean {
        if (manga.thumbnailUrl.isNullOrBlank() || isCurrent(manga)) return true
        val bitmap = runCatching { decodeCached(manga) }.throwIfCancellation()
            .onFailure { log.warn(it) { "Cached cover of ${manga.id} not decoded" } }
            .getOrElse { return true } ?: return false
        put(manga, dominantColor(bitmap)?.toArgb())
        bitmap.recycle()
        delay(SAMPLE_PAUSE_MS)
        return true
    }

    private fun decodeCached(manga: Manga): Bitmap? =
        mangaCoverUrl(serverPreferences.serverUrl().get(), manga)?.let { imageCache.openSnapshot(it.toString()) }?.use { snapshot ->
            val path = snapshot.data.toString()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
            BitmapFactory.decodeFile(path, options) ?: error("unsupported image format")
        }

    private fun persist(accents: Map<Long, CachedAccent>) {
        runCatching { file.save(accents) }.onFailure { log.warn(it) { "Cover accents not saved" } }
    }

    private companion object {
        private val log = logging()
        const val ACCENT_FILE = "cover_accents"
        const val SAMPLE_PAUSE_MS = 25L
        const val RETRY_MS = 30_000L
    }
}

private fun coverHash(manga: Manga): Int = mangaCoverKey(manga).hashCode()

internal fun sampleSizeFor(
    width: Int,
    height: Int,
    target: Int = 64,
): Int {
    // the long side bounds the allocation; a 100 x 2,000,000 cover must still shrink
    var size = 1
    while (maxOf(width, height) / (size * 2) >= target) size *= 2
    return size
}
