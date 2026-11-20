/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.core.util

fun <T> List<T>.replace(
    position: Int,
    newItem: T,
): List<T> {
    val newList = toMutableList()
    newList[position] = newItem
    return newList
}

inline fun <T> List<T>.replaceFirst(
    predicate: (T) -> Boolean,
    newItem: T,
): List<T> {
    forEachIndexed { index, element ->
        if (predicate(element)) {
            return replace(index, newItem)
        }
    }
    return this
}
