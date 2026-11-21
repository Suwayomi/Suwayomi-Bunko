/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.diamondedge.logging.logging
import dev.bunko.core.di.AppScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class ServerReachability {
    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    fun reportReachable() {
        _offline.value = false
    }

    fun reportUnreachable() {
        _offline.value = true
    }

    fun attach(context: Context) {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        runCatching {
            connectivity.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        reportReachable()
                    }

                    override fun onLost(network: Network) {
                        if (connectivity.activeNetwork == null) reportUnreachable()
                    }
                },
            )
        }.onFailure { log.warn(it) { "Network callback not registered, relying on request results" } }
    }

    private companion object {
        private val log = logging()
    }
}
