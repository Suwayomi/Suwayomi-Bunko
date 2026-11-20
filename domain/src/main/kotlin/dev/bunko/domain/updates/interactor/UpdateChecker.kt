/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.updates.interactor

import dev.bunko.domain.BuildConfig
import dev.bunko.domain.server.HttpNoAuth
import dev.bunko.domain.updates.model.GithubRelease
import dev.bunko.domain.updates.service.UpdatePreferences
import com.diamondedge.logging.logging
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.singleOrNull
import me.tatarka.inject.annotations.Inject

@Inject
class UpdateChecker(
    private val updatePreferences: UpdatePreferences,
    private val client: HttpNoAuth,
) {
    suspend fun await(
        manualFetch: Boolean,
        onError: suspend (Throwable) -> Unit = {},
    ) = asFlow(manualFetch)
        .catch {
            onError(it)
            log.warn(it) { "Failed to check for updates" }
        }
        .singleOrNull()

    fun asFlow(manualFetch: Boolean) =
        flow {
            if (!manualFetch && !updatePreferences.enabled().get()) return@flow
            val latestRelease = client.value.get(
                "https://api.github.com/repos/$GITHUB_REPO/releases/latest",
            ).body<GithubRelease>()

            if (isNewVersion(latestRelease.version)) {
                emit(Update.UpdateFound(latestRelease))
            } else {
                emit(Update.NoUpdatesFound)
            }
        }.flowOn(Dispatchers.IO)

    sealed class Update {
        data class UpdateFound(
            val release: GithubRelease,
        ) : Update()

        data object NoUpdatesFound : Update()
    }

    private fun isNewVersion(versionTag: String): Boolean {
        val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")

        return newVersion != BuildConfig.VERSION
    }

    companion object {
        private const val GITHUB_REPO = "melu/bunko"

        val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/v${BuildConfig.VERSION}"

        private val log = logging()
    }
}
