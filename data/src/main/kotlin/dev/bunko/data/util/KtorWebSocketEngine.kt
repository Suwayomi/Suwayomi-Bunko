package dev.bunko.data.util

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloWebSocketClosedException
import com.apollographql.apollo.network.websocket.WebSocket
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketListener
import com.diamondedge.logging.logging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ApolloExperimental::class)
class KtorWebSocketEngine(
    private val client: HttpClient,
) : WebSocketEngine {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
        return open(Url(url), headers, listener)
    }

    private fun open(url: Url, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
        val newUrl = URLBuilder(url).apply {
            protocol = when (url.protocol) {
                URLProtocol.HTTPS -> URLProtocol.WSS
                URLProtocol.HTTP -> URLProtocol.WS
                URLProtocol.WS, URLProtocol.WSS -> url.protocol
                else -> throw UnsupportedOperationException("${url.protocol.name} is not a supported protocol")
            }
        }.build()
        val sendChannel = Channel<Frame>(Channel.UNLIMITED)

        coroutineScope.launch {
            val terminated = AtomicBoolean(false)
            fun terminate(block: () -> Unit) {
                if (terminated.compareAndSet(false, true)) block()
            }

            try {
                client.webSocket(
                    request = {
                        headers {
                            headers.forEach { append(it.name, it.value) }
                        }
                        url(newUrl)
                    },
                ) {
                    val session = this
                    val sender = launch {
                        for (frame in sendChannel) {
                            session.send(frame)
                            if (frame is Frame.Close) {
                                terminate { listener.onClosed(1000, null) }
                                session.cancel()
                                return@launch
                            }
                        }
                    }
                    try {
                        listener.onOpen()
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> listener.onMessage(frame.readText())
                                is Frame.Binary -> listener.onMessage(frame.readBytes())
                                else -> Unit
                            }
                        }
                        val closed = closedException(closeReasonOrNull(), null)
                        terminate { listener.onError(closed) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        log.warn(e) { "Websocket failed" }
                        val failure = closedException(closeReasonOrNull(), e)
                        terminate { listener.onError(failure) }
                    } finally {
                        sender.cancel()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                log.warn(e) { "Websocket could not connect" }
                terminate { listener.onError(ApolloNetworkException("Web socket communication error", e)) }
            } finally {
                sendChannel.close()
                terminate { listener.onError(ApolloNetworkException("Web socket closed", null)) }
            }
        }

        return object : WebSocket {
            override fun send(data: ByteArray) {
                sendChannel.trySend(Frame.Binary(true, data))
            }

            override fun send(text: String) {
                sendChannel.trySend(Frame.Text(text))
            }

            override fun close(code: Int, reason: String) {
                sendChannel.trySend(Frame.Close(CloseReason(code.toShort(), reason)))
            }
        }
    }

    private fun closedException(reason: CloseReason?, cause: Throwable?) =
        if (reason != null) {
            ApolloWebSocketClosedException(
                code = reason.code.toInt(),
                reason = reason.message,
                cause = cause,
            )
        } else {
            ApolloNetworkException(message = "Web socket communication error", platformCause = cause)
        }

    private suspend fun DefaultClientWebSocketSession.closeReasonOrNull(): CloseReason? {
        return try {
            closeReason.await()
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }
            null
        }
    }

    override fun close() {
        log.info { "Closing websocket engine" }
        coroutineScope.cancel()
    }

    companion object {
        private val log = logging()
    }
}
