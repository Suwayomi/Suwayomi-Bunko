/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.server

import dev.bunko.domain.server.model.parseServerUrl

// the scheme://host:port pairs that stored credentials are sent to
data class CredentialOrigins(
    val primary: String?,
    val fallback: String?,
)

fun credentialOrigins(
    server: String,
    port: Int,
    fallback: String,
): CredentialOrigins {
    val primary = parseServerUrl(server, port, "")
    val backup = parseFallbackOrigin(fallback, primary?.protocol?.name ?: "http", primary?.port ?: port)
    return CredentialOrigins(
        primary = primary?.let { "${it.protocol.name}://${it.host}:${it.port}" },
        fallback = backup?.let { "${it.scheme}://${it.host}:${it.port}" },
    )
}

// only a move from one real origin to another counts; clearing an address sends nothing anywhere
fun CredentialOrigins.movedFrom(old: CredentialOrigins): Boolean =
    moved(old.primary, primary) || moved(old.fallback, fallback)

private fun moved(
    old: String?,
    new: String?,
): Boolean = old != null && new != null && old != new
