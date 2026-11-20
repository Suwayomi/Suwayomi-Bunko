/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.offline

import dev.bunko.uicore.vm.ContextWrapper
import okio.Path
import okio.Path.Companion.toOkioPath

fun offlineChaptersRoot(contextWrapper: ContextWrapper): Path = contextWrapper.filesDir.toOkioPath() / "offline_chapters"
