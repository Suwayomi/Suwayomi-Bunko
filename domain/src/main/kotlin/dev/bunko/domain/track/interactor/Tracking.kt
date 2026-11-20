/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.track.interactor

import com.diamondedge.logging.logging
import dev.bunko.domain.track.model.TrackRecord
import dev.bunko.domain.track.model.TrackSearch
import dev.bunko.domain.track.model.TrackUpdate
import dev.bunko.domain.track.model.Tracker
import dev.bunko.domain.track.service.TrackRepository
import kotlinx.coroutines.CancellationException
import me.tatarka.inject.annotations.Inject

@Inject
class Tracking(
    private val repository: TrackRepository,
) {
    suspend fun trackers(): Result<List<Tracker>> = attempt("load trackers") { repository.trackers() }

    suspend fun records(mangaId: Long): Result<List<TrackRecord>> =
        attempt("load track records of $mangaId") { repository.records(mangaId) }

    suspend fun search(trackerId: Int, query: String): Result<List<TrackSearch>> =
        attempt("search tracker $trackerId") { repository.search(trackerId, query.trim()) }

    suspend fun bind(mangaId: Long, trackerId: Int, remoteId: Long): Result<TrackRecord> =
        attempt("bind $mangaId to tracker $trackerId") { repository.bind(mangaId, trackerId, remoteId) }

    suspend fun update(recordId: Int, update: TrackUpdate): Result<TrackRecord?> =
        attempt("update track record $recordId") { repository.update(recordId, update) }

    suspend fun unbind(recordId: Int, deleteRemote: Boolean): Result<Unit> =
        attempt("unbind track record $recordId") { repository.unbind(recordId, deleteRemote) }

    suspend fun loginOAuth(trackerId: Int, callbackUrl: String): Result<Boolean> =
        attempt("log in to tracker $trackerId") { repository.loginOAuth(trackerId, callbackUrl) }

    suspend fun loginCredentials(trackerId: Int, username: String, password: String): Result<Boolean> =
        attempt("log in to tracker $trackerId") { repository.loginCredentials(trackerId, username, password) }

    suspend fun logout(trackerId: Int): Result<Unit> =
        attempt("log out of tracker $trackerId") { repository.logout(trackerId) }

    private suspend fun <T> attempt(what: String, block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "Failed to $what" }
            Result.failure(e)
        }

    private companion object {
        private val log = logging()
    }
}
