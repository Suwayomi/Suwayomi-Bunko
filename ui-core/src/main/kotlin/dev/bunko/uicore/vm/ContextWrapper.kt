/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.vm

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import dev.icerock.moko.resources.format
import me.tatarka.inject.annotations.Inject

enum class Length {
    SHORT,
    LONG,
}

@Inject
class ContextWrapper(
    context: Context,
) : ContextWrapper(context) {
    fun toPlatformString(stringResource: StringResource): String = stringResource.desc().toString(this)

    fun toPlatformString(
        stringResource: StringResource,
        vararg args: Any,
    ): String = stringResource.format(*args).toString(this)

    fun toast(
        string: String,
        length: Length,
    ) {
        Toast.makeText(
            this,
            string,
            when (length) {
                Length.SHORT -> Toast.LENGTH_SHORT
                Length.LONG -> Toast.LENGTH_LONG
            },
        ).show()
    }
}
