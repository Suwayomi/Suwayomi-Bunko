/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.main.more

import dev.bunko.domain.ui.service.UiPreferences
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import me.tatarka.inject.annotations.Inject

@Inject
class MoreViewModel(
    uiPreferences: UiPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    val incognito = uiPreferences.incognito().asStateFlow()
}
