/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.track.interactor

import com.diamondedge.logging.logging
import dev.bunko.core.di.AppScope
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.track.service.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

// marking a chapter read over graphql only stores the flag; trackers sync on trackProgress.
// reads arriving while a sync is running collapse into one follow-up per manga instead of a call each
@AppScope
@Inject
class SyncTrackProgress(
    private val trackRepository: TrackRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = MutableStateFlow(emptySet<Long>())
    private val wake = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (signal in wake) {
                while (true) {
                    val mangaId = pending.value.firstOrNull() ?: break
                    pending.update { it - mangaId }
                    runCatching { trackRepository.syncProgress(mangaId) }
                        .throwIfCancellation()
                        .onFailure { log.warn(it) { "Tracker progress sync failed for manga $mangaId" } }
                }
            }
        }
    }

    fun request(mangaIds: Collection<Long>) {
        val ids = mangaIds.filter { it >= 0 }
        if (ids.isEmpty()) return
        pending.update { it + ids }
        wake.trySend(Unit)
    }

    private companion object {
        val log = logging()
    }
}
