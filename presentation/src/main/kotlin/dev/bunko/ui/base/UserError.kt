/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base

import dev.bunko.domain.server.toConnectionError
import dev.bunko.ui.settings.userMessage
import dev.icerock.moko.resources.StringResource

val Throwable.userMessage: StringResource
    get() = toConnectionError().userMessage
