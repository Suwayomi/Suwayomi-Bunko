/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import androidx.compose.runtime.Composable
import dev.bunko.domain.reader.model.DefaultReaderMode
import dev.bunko.i18n.MR
import dev.bunko.uicore.resources.stringResource
import dev.icerock.moko.resources.StringResource

internal fun readerModeResource(mode: String): StringResource? =
    when (DefaultReaderMode.entries.find { it.res == mode }) {
        DefaultReaderMode.RTL -> MR.strings.reader_mode_rtl
        DefaultReaderMode.LTR -> MR.strings.reader_mode_ltr
        DefaultReaderMode.Vertical -> MR.strings.reader_mode_vertical
        DefaultReaderMode.ContinuesVertical -> MR.strings.reader_mode_continuous_vertical
        DefaultReaderMode.LongStrip -> MR.strings.reader_mode_long_strip
        null -> null
    }

@Composable
fun readerModeLabel(mode: String): String = readerModeResource(mode)?.let { stringResource(it) } ?: mode
