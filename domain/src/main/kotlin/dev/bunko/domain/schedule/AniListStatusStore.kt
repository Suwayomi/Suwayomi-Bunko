/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.schedule

data class StoredStatus(
    val status: AniListStatus?,
    val fetchedAt: Long,
)

interface AniListStatusStore {
    fun load(): Map<Long, StoredStatus>

    fun save(entries: Map<Long, StoredStatus>)
}
