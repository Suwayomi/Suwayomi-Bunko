/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.image

import dev.bunko.uicore.vm.ContextWrapper
import com.seiko.imageloader.component.decoder.BitmapFactoryDecoder
import com.seiko.imageloader.component.decoder.Decoder

class BitmapDecoderFactory constructor(
    contextWrapper: ContextWrapper,
) : Decoder.Factory by BitmapFactoryDecoder.Factory(contextWrapper, MAX_PARALLEL_DECODES)

private const val MAX_PARALLEL_DECODES = 2
