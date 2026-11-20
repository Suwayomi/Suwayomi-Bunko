/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.lang

import java.util.PriorityQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelIterator
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.selects.select

class PriorityChannel<T : Any>(
    private val maxCapacity: Int = 4096,
    scope: CoroutineScope,
    comparator: Comparator<T>,
) {
    private val inChannel = Channel<T>(Channel.RENDEZVOUS)
    private val outChannel = Channel<T>(Channel.RENDEZVOUS)
    private val buffer = PriorityQueue(INITIAL_CAPACITY, comparator)
    private var open = true

    init {
        require(maxCapacity >= 2) {
            "priorityChannel maxCapacity < 2 does not make any sense"
        }

        scope.async {
            try {
                pump()
            } finally {
                outChannel.close()
            }
        }.start()
    }

    suspend fun send(element: T) = inChannel.send(element)

    operator fun iterator(): ChannelIterator<T> = outChannel.iterator()

    fun close() = inChannel.close()

    private suspend fun pump() {
        while (open || buffer.isNotEmpty()) {
            when {
                buffer.isEmpty() -> accept(inChannel.receiveCatching())
                !open || buffer.size >= maxCapacity -> outChannel.send(buffer.remove())
                else -> {
                    val head: T = buffer.element()
                    select<Any?> {
                        inChannel.onReceiveCatching { accept(it) }
                        outChannel.onSend(head) { buffer.remove() }
                    }
                }
            }
        }
    }

    private fun accept(result: ChannelResult<T>) {
        val element = result.getOrNull()
        if (element != null) buffer.add(element) else open = false
    }

    private companion object {
        const val INITIAL_CAPACITY = 11
    }
}
