/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord

import com.diamondedge.logging.logging
import dev.bunko.core.di.AppScope
import dev.bunko.core.prefs.getAsFlow
import dev.bunko.domain.discord.model.DiscordStatus
import dev.bunko.domain.discord.model.Presence
import dev.bunko.domain.discord.model.ReadingActivity
import dev.bunko.domain.discord.service.DiscordPreferences
import dev.bunko.domain.ui.service.UiPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@AppScope
@Inject
class DiscordPresence(
    private val preferences: DiscordPreferences,
    private val uiPreferences: UiPreferences,
) {
    private val reading = MutableStateFlow<ReadingActivity?>(null)

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(WebSockets) { pingIntervalMillis = 30.seconds.inWholeMilliseconds }
            install(HttpTimeout) {
                connectTimeoutMillis = 10.seconds.inWholeMilliseconds
                requestTimeoutMillis = 20.seconds.inWholeMilliseconds
            }
        }
    }
    private val api by lazy { DiscordApi(client, json) }
    private val gateway by lazy { DiscordGateway(client, json) }
    private val aniList by lazy { AniListCovers(client, json) }

    private val coverCache = mutableMapOf<Long, String?>()
    private val coverLock = Mutex()

    fun setReading(activity: ReadingActivity?) {
        reading.value = activity
    }

    suspend fun checkToken(token: String): TokenCheck = api.checkToken(token.trim())

    private data class Session(val token: String, val status: DiscordStatus)

    private data class Shown(
        val reading: ReadingActivity?,
        val showCover: Boolean,
        val options: PresenceOptions,
        val visible: Boolean,
    )

    fun start(scope: CoroutineScope, foreground: StateFlow<Boolean>, labels: PresenceLabels) {
        preferences.dropStaleLink()
        val inUse = foreground.transformLatest {
            if (!it) delay(BACKGROUND_GRACE)
            emit(it)
        }
        val shown = combine(
            preferences.enabled().getAsFlow(),
            uiPreferences.incognito().getAsFlow(),
            inUse,
        ) { enabled, incognito, used -> enabled && !incognito && used }
            .stateIn(scope, SharingStarted.Eagerly, false)
        val link = combine(preferences.token().getAsFlow(), preferences.status().getAsFlow()) { token, status ->
            Session(token.trim(), status).takeIf { it.token.isNotEmpty() }
        }
        val connect = combine(link, shown, ::Pair)
            .transformLatest { (link, shown) ->
                if (link != null && shown) {
                    emit(link)
                } else {
                    if (link != null) delay(CLEAR_LINGER)
                    emit(null)
                }
            }
            .distinctUntilChanged()

        scope.launch(Dispatchers.IO) {
            connect.collectLatest { if (it != null) keepConnected(it, shown, labels) }
        }
    }

    private suspend fun keepConnected(session: Session, shown: StateFlow<Boolean>, labels: PresenceLabels) = coroutineScope {
        val presence = presences(session, shown, labels).stateIn(this)
        var backoff = MIN_BACKOFF
        try {
            while (true) {
                if (!applyStatus(session)) return@coroutineScope
                val started = Clock.System.now()
                val end = try {
                    gateway.session(session.token, presence)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.info { "Discord gateway unreachable: ${e.message}" }
                    SessionEnd.Dropped
                }
                if (end == SessionEnd.AuthFailed) {
                    rejected()
                    return@coroutineScope
                }
                if (Clock.System.now() - started > STABLE_SESSION) backoff = MIN_BACKOFF
                delay(backoff * (1 + Random.nextDouble(JITTER)))
                backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
            }
        } finally {
            gateway.forgetSession()
            log.info { "Discord gateway: closed" }
        }
    }

    private fun rejected() {
        log.warn { "Discord rejected the token; account unlinked" }
        preferences.unlink()
    }

    private suspend fun applyStatus(session: Session): Boolean {
        try {
            val status = api.setStatus(session.token, session.status.wire)
            if (status == HttpStatusCode.Unauthorized) {
                rejected()
                return false
            }
            if (!status.isSuccess()) log.info { "Discord refused the status change (${status.value})" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.info { "Discord status change failed: ${e.message}" }
        }
        return true
    }

    private fun presences(session: Session, shown: StateFlow<Boolean>, labels: PresenceLabels): Flow<Presence> {
        val since = Clock.System.now().toEpochMilliseconds()
        return combine(
            reading,
            preferences.showChapter().getAsFlow(),
            preferences.showCover().getAsFlow(),
            preferences.showTimestamp().getAsFlow(),
            shown,
        ) { reading, showChapter, showCover, showTimestamp, shown ->
            Shown(reading, showCover, PresenceOptions(showChapter, showTimestamp), shown)
        }.mapLatest { (reading, showCover, options, visible) ->
            val cover = if (visible && showCover && reading != null) cover(session.token, reading) else null
            Presence(
                activities = if (visible) listOf(presenceActivity(reading, cover, since, labels, options, APPLICATION_ID)) else emptyList(),
                status = session.status.wire,
                afk = session.status != DiscordStatus.Online,
                since = 0,
            )
        }
    }

    private suspend fun cover(token: String, reading: ReadingActivity): String? {
        coverLock.withLock {
            if (reading.mangaId in coverCache) return coverCache[reading.mangaId]
        }
        val url = when (val found = aniList.lookup(reading.mangaTitle)) {
            is CoverLookup.Found -> found.url
            CoverLookup.NotFound -> return remember(reading.mangaId, null)
            is CoverLookup.Failed -> {
                log.info { "AniList cover lookup failed: ${found.cause.message}" }
                return null
            }
        }
        val asset = try {
            api.externalAsset(APPLICATION_ID, token, url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.info { "Discord external asset failed: ${e.message}" }
            return null
        }
        return remember(reading.mangaId, asset)
    }

    private suspend fun remember(mangaId: Long, asset: String?): String? {
        coverLock.withLock { coverCache[mangaId] = asset }
        return asset
    }

    private companion object {
        private val log = logging()

        const val APPLICATION_ID = "1573027689168896120"

        val BACKGROUND_GRACE = 30.seconds
        val MIN_BACKOFF = 5.seconds
        val MAX_BACKOFF = 5.minutes
        val STABLE_SESSION = 1.minutes
        val CLEAR_LINGER = 6.seconds
        const val JITTER = 0.25
    }
}
