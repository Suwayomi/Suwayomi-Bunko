/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader.navigation

import androidx.compose.runtime.Immutable
import dev.bunko.ui.reader.model.Navigation

@Immutable
open class LNavigation : ViewerNavigation() {
    override var regions: List<Region> = listOf(
        Region(
            rect = Rect(0, 34, 33, 66),
            type = Navigation.PREV,
        ),
        Region(
            rect = Rect(0, 0, 100, 33),
            type = Navigation.PREV,
        ),
        Region(
            rect = Rect(67, 34, 100, 66),
            type = Navigation.NEXT,
        ),
        Region(
            rect = Rect(0, 67, 100, 100),
            type = Navigation.NEXT,
        ),
    )
}
