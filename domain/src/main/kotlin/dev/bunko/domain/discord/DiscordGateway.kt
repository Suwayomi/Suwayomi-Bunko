/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.discord

import com.diamondedge.logging.logging
import dev.bunko.domain.discord.model.GatewayMessage
import dev.bunko.domain.discord.model.Identify
import dev.bunko.domain.discord.model.OpCode
import dev.bunko.domain.discord.model.OutgoingMessage
import dev.bunko.domain.discord.model.Presence
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

internal sealed interface SessionEnd {
    data object AuthFailed : SessionEnd

    data object Dropped : SessionEnd
}

internal class DiscordGateway(
    private val client: HttpClient,
    private val json: Json,
) {
    private var sessionId: String? = null
    private var resumeUrl: String? = null
    private var lastSeq: Int? = null

    fun forgetSession() {
        sessionId = null
        resumeUrl = null
        lastSeq = null
    }

    suspend fun session(token: String, presence: StateFlow<Presence>): SessionEnd {
        var end: SessionEnd = SessionEnd.Dropped
        val resumeId = sessionId
        val url = resumeUrl?.takeIf { resumeId != null }?.let { "$it/?v=10&encoding=json" } ?: GATEWAY
        client.webSocket(url) {
            val ready = CompletableDeferred<Unit>()
            var heartbeat: Job? = null
            var ackPending = false
            var leaving = false
            val updates = launch {
                ready.await()
                presence.spaced(PRESENCE_GAP).collect { send(OpCode.PRESENCE_UPDATE, it, Presence.serializer()) }
            }
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val message = runCatching { json.decodeFromString<GatewayMessage>(frame.readText()) }.getOrNull() ?: continue
                    message.s?.let { lastSeq = it }
                    when (message.op) {
                        OpCode.HELLO -> {
                            val interval = message.d?.jsonObject?.get("heartbeat_interval")?.jsonPrimitive?.long
                            if (interval == null) {
                                leaving = true
                                break
                            }
                            heartbeat?.cancel()
                            heartbeat = launch {
                                delay((interval * Random.nextDouble()).toLong())
                                while (isActive) {
                                    if (ackPending) {
                                        log.info { "Discord heartbeat not acknowledged, reconnecting" }
                                        this@webSocket.close()
                                        return@launch
                                    }
                                    ackPending = true
                                    sendHeartbeat(lastSeq)
                                    delay(interval)
                                }
                            }
                            if (resumeId != null) {
                                log.info { "Discord gateway: resuming session" }
                                val resume = buildJsonObject {
                                    put("token", JsonPrimitive(token))
                                    put("session_id", JsonPrimitive(resumeId))
                                    put("seq", JsonPrimitive(lastSeq))
                                }
                                send(OpCode.RESUME, resume, JsonObject.serializer())
                            } else {
                                log.info { "Discord gateway: identifying" }
                                send(
                                    OpCode.IDENTIFY,
                                    Identify(token, Identify.Properties(), presence = presence.value),
                                    Identify.serializer(),
                                )
                            }
                        }

                        OpCode.DISPATCH -> when (message.t) {
                            "READY" -> {
                                val d = message.d?.jsonObject
                                sessionId = d?.get("session_id")?.jsonPrimitive?.content
                                resumeUrl = d?.get("resume_gateway_url")?.jsonPrimitive?.content
                                ready.complete(Unit)
                            }

                            "RESUMED" -> ready.complete(Unit)
                        }

                        OpCode.HEARTBEAT -> sendHeartbeat(lastSeq)

                        OpCode.RECONNECT -> {
                            leaving = true
                            break
                        }

                        OpCode.INVALID_SESSION -> {
                            val resumable = message.d?.jsonPrimitive?.booleanOrNull == true
                            log.info { "Discord gateway: invalid session (resumable=$resumable)" }
                            if (!resumable) forgetSession()
                            leaving = true
                            break
                        }

                        OpCode.HEARTBEAT_ACK -> ackPending = false
                    }
                }
                if (!leaving && runCatching { closeReason.await() }.getOrNull()?.code == AUTH_FAILED) {
                    end = SessionEnd.AuthFailed
                }
            } finally {
                heartbeat?.cancel()
                updates.cancel()
            }
        }
        return end
    }

    private suspend fun <T> DefaultClientWebSocketSession.send(
        op: Int,
        payload: T,
        serializer: kotlinx.serialization.KSerializer<T>,
    ) {
        send(Frame.Text(json.encodeToString(OutgoingMessage.serializer(serializer), OutgoingMessage(op, payload))))
    }

    private suspend fun DefaultClientWebSocketSession.sendHeartbeat(seq: Int?) {
        send(Frame.Text("""{"op":${OpCode.HEARTBEAT},"d":${seq ?: "null"}}"""))
    }

    private companion object {
        val log = logging()
        const val GATEWAY = "wss://gateway.discord.gg/?v=10&encoding=json"
        const val AUTH_FAILED: Short = 4004
        val PRESENCE_GAP = 5.seconds
    }
}
