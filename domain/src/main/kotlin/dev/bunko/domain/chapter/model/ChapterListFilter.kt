/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.domain.chapter.model

import dev.bunko.domain.library.model.FilterState

fun <T> List<T>.applyChapterOptions(
    options: ChapterListOptions,
    chapter: (T) -> Chapter,
    isSavedOnDevice: (Chapter) -> Boolean,
): List<T> {
    val comparator = options.sort.comparator().let { if (options.descending) it.reversed() else it }
    return filter { item ->
        val it = chapter(item)
        options.unread.matches(!it.read) &&
            options.downloaded.matches(it.downloaded || isSavedOnDevice(it)) &&
            options.bookmarked.matches(it.bookmarked) &&
            !it.isExcluded(options)
    }.sortedWith(compareBy(comparator, chapter))
}

fun <T> List<T>.withoutExcludedScanlators(
    options: ChapterListOptions,
    chapter: (T) -> Chapter,
): List<T> = if (options.excludedScanlators.isEmpty()) this else filterNot { chapter(it).isExcluded(options) }

fun List<Chapter>.withoutExcludedScanlators(options: ChapterListOptions): List<Chapter> = withoutExcludedScanlators(options) { it }

fun List<Chapter>.scanlators(): List<String> = mapNotNull { it.scanlatorOrNull() }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

/**
 * Counts whole chapter numbers absent between the lowest and highest known number.
 * Fractional numbers are truncated since a gap after 16.5 can't be told apart from a split chapter.
 */
fun List<Chapter>.missingChapterCount(): Int = map { it.chapterNumber }.missingCount()

fun chapterGap(
    higher: Chapter,
    lower: Chapter,
): Int {
    if (higher.chapterNumber < 0f || lower.chapterNumber < 0f) return 0
    return (higher.chapterNumber.toInt() - lower.chapterNumber.toInt() - 1).coerceAtLeast(0)
}

internal fun List<Float>.missingCount(): Int {
    val numbers = filter { it >= 0f }.map { it.toInt() }.distinct().sorted()
    if (numbers.isEmpty()) return 0
    var missing = 0
    var previous = 0
    for (number in numbers) {
        if (number > previous + 1) missing += number - previous - 1
        previous = number
    }
    return missing
}

/** "12" for 12.0, "12.5" for 12.5, null when the source gave no number. */
fun formatChapterNumber(number: Float): String? {
    if (number < 0f) return null
    val whole = number.toLong()
    return if (number == whole.toFloat()) whole.toString() else number.toString()
}

private fun Chapter.scanlatorOrNull(): String? = scanlator?.trim()?.takeIf { it.isNotEmpty() }

private fun Chapter.isExcluded(options: ChapterListOptions): Boolean = scanlatorOrNull()?.let { it in options.excludedScanlators } == true

private fun FilterState.matches(value: Boolean): Boolean =
    when (this) {
        FilterState.IGNORED -> true
        FilterState.INCLUDED -> value
        FilterState.EXCLUDED -> !value
    }

private fun ChapterSort.comparator(): Comparator<Chapter> =
    when (this) {
        ChapterSort.SOURCE -> compareBy { it.index }
        ChapterSort.NUMBER -> compareBy<Chapter> { it.chapterNumber }.thenBy { it.index }
        ChapterSort.UPLOAD_DATE -> compareBy<Chapter> { it.uploadDate }.thenBy { it.index }
        ChapterSort.ALPHABETICAL -> compareBy<Chapter, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.index }
    }

/** The closest chapter after [current] in source order, skipping hidden scanlators. */
fun List<Chapter>.nextReadable(
    current: Chapter,
    options: ChapterListOptions?,
): Chapter? = readable(options).filter { it.index > current.index }.minByOrNull { it.index }

/** The closest chapter before [current] in source order, skipping hidden scanlators. */
fun List<Chapter>.previousReadable(
    current: Chapter,
    options: ChapterListOptions?,
): Chapter? = readable(options).filter { it.index < current.index }.maxByOrNull { it.index }

private fun List<Chapter>.readable(options: ChapterListOptions?): List<Chapter> =
    if (options == null) this else withoutExcludedScanlators(options)
