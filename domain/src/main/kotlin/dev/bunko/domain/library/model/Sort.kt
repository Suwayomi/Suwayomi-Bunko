/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.library.model

import androidx.compose.runtime.Stable
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.StringResource
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Stable
enum class Sort(
    @Transient val res: StringResource,
) {
    ALPHABETICAL(MR.strings.sort_alphabetical),

    UNREAD(MR.strings.sort_unread),

    DATE_ADDED(MR.strings.sort_date_added),
}
