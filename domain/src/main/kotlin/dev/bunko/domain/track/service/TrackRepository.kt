/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.track.service

import dev.bunko.domain.track.model.TrackRecord
import dev.bunko.domain.track.model.TrackSearch
import dev.bunko.domain.track.model.TrackUpdate
import dev.bunko.domain.track.model.Tracker

interface TrackRepository {
    suspend fun trackers(): List<Tracker>

    suspend fun records(mangaId: Long): List<TrackRecord>

    suspend fun search(trackerId: Int, query: String): List<TrackSearch>

    suspend fun bind(mangaId: Long, trackerId: Int, remoteId: Long): TrackRecord

    suspend fun update(recordId: Int, update: TrackUpdate): TrackRecord?

    suspend fun unbind(recordId: Int, deleteRemote: Boolean)

    suspend fun loginOAuth(trackerId: Int, callbackUrl: String): Boolean

    suspend fun loginCredentials(trackerId: Int, username: String, password: String): Boolean

    suspend fun logout(trackerId: Int)
}
