package dev.bunko.ui.reader

internal fun imageExtension(head: ByteArray): String {
    fun at(offset: Int, text: String) = text.indices.all { head.getOrNull(offset + it) == text[it].code.toByte() }
    return when {
        head.getOrNull(0) == 0x89.toByte() && at(1, "PNG") -> "png"
        at(0, "GIF8") -> "gif"
        at(0, "RIFF") && at(8, "WEBP") -> "webp"
        at(4, "ftypavif") -> "avif"
        else -> "jpg"
    }
}

internal fun imageMime(extension: String): String = if (extension == "jpg") "image/jpeg" else "image/$extension"

private fun String.pathSafe() = replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "").trim()

internal fun folderName(manga: String): String = manga.pathSafe().take(100).trim('.', ' ').ifEmpty { "Unknown" }

internal fun pageFileName(manga: String, chapter: String, page: Int, extension: String): String =
    "${"$manga - $chapter - ${page.toString().padStart(3, '0')}".pathSafe().take(200)}.$extension"
