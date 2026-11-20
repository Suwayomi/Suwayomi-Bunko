package dev.bunko.ui.reader

fun resumePageIndex(
    lastPageRead: Int,
    pageCount: Int,
): Int = if (pageCount <= 0 || lastPageRead !in 1 until pageCount - 1) 0 else lastPageRead
