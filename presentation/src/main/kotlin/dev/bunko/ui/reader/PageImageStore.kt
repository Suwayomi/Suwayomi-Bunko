package dev.bunko.ui.reader

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.reader.service.ReaderPreferences
import dev.bunko.uicore.vm.ContextWrapper
import me.tatarka.inject.annotations.Inject
import okio.BufferedSink
import okio.buffer
import okio.sink
import okio.use

@Inject
class PageImageStore(
    private val context: ContextWrapper,
    private val readerPreferences: ReaderPreferences,
) {
    fun save(folder: String, name: String, mime: String, write: (BufferedSink) -> Unit): Result<Unit> =
        runCatching {
            val tree = readerPreferences.imageSaveFolder().get()
            if (tree.isNotEmpty()) saveToTree(Uri.parse(tree), folder, name, mime, write) else saveToPictures(folder, name, mime, write)
        }.throwIfCancellation()

    private fun saveToPictures(folder: String, name: String, mime: String, write: (BufferedSink) -> Unit) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { "pick an image save folder, pictures is only writable without permission from android 10" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Bunko/$folder")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) { "media store refused the image" }
        runCatching {
            checkNotNull(resolver.openOutputStream(uri)).sink().buffer().use(write)
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }.onFailure { resolver.delete(uri, null, null) }.getOrThrow()
    }

    private fun saveToTree(tree: Uri, folder: String, name: String, mime: String, write: (BufferedSink) -> Unit) {
        val resolver = context.contentResolver
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val dir = childDirectory(resolver, tree, root, folder)
            ?: checkNotNull(DocumentsContract.createDocument(resolver, root, Document.MIME_TYPE_DIR, folder)) { "couldn't create $folder" }
        val file = checkNotNull(DocumentsContract.createDocument(resolver, dir, mime, name)) { "couldn't create $name" }
        runCatching { checkNotNull(resolver.openOutputStream(file)).sink().buffer().use(write) }
            .onFailure { DocumentsContract.deleteDocument(resolver, file) }
            .getOrThrow()
    }

    private fun childDirectory(resolver: ContentResolver, tree: Uri, parent: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        return resolver.query(children, columns, null, null, null)?.use { cursor ->
            generateSequence { cursor.takeIf { it.moveToNext() } }
                .firstOrNull { it.getString(1) == name && it.getString(2) == Document.MIME_TYPE_DIR }
                ?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(0)) }
        }
    }
}
