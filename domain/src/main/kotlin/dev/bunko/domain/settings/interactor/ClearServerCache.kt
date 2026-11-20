/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.settings.interactor

import dev.bunko.domain.settings.service.SettingsRepository
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.singleOrNull
import me.tatarka.inject.annotations.Inject

@Inject
class ClearServerCache(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun await(
        pages: Boolean,
        thumbnails: Boolean,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(pages, thumbnails)
        .catch {
            onError(it)
            log.warn(it) { "Failed to clear server cache" }
        }
        .singleOrNull()

    fun asFlow(
        pages: Boolean,
        thumbnails: Boolean,
    ) = settingsRepository.clearServerCache(pages, thumbnails)

    companion object {
        private val log = logging()
    }
}
