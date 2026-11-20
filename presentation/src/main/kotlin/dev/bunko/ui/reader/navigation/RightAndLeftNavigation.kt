/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader.navigation

import androidx.compose.runtime.Immutable
import dev.bunko.ui.reader.model.Navigation

@Immutable
class RightAndLeftNavigation : ViewerNavigation() {
    override var regions: List<Region> = listOf(
        Region(
            rect = Rect(0, 0, 33, 100),
            type = Navigation.LEFT,
        ),
        Region(
            rect = Rect(66, 0, 100, 100),
            type = Navigation.RIGHT,
        ),
    )
}
