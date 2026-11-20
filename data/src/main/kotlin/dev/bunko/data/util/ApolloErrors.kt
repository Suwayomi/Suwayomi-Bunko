/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.data.util

import dev.bunko.domain.server.ServerRejectedException
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation

internal fun String?.looksUnauthorized() =
    this != null && (contains("unauthorized", ignoreCase = true) || contains("unauthenticated", ignoreCase = true))

internal val <D : Operation.Data> ApolloResponse<D>.dataOrRejected: D
    get() {
        val errors = errors.orEmpty()
        if (exception == null && errors.isNotEmpty() && errors.none { it.message.looksUnauthorized() }) {
            throw ServerRejectedException(errors.joinToString { it.message }, null)
        }
        return dataAssertNoErrors
    }
