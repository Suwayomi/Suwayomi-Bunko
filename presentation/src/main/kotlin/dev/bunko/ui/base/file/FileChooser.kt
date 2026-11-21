/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.base.file

import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import okio.Source
import okio.source

class FileChooser(
    private val resultLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
) {
    fun launch(vararg extensions: String) {
        val mimes = extensions.flatMap { extension ->
            listOfNotNull(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)) +
                ExtraMimes[extension].orEmpty()
        }.distinct().toTypedArray()
        resultLauncher.launch(mimes)
    }
}

private val ExtraMimes = mapOf(
    "apk" to listOf("application/vnd.android.package-archive"),
    "jar" to listOf("application/java-archive", "application/x-java-archive"),
)

@Composable
fun rememberNamedFileChooser(onFileFound: (Source, String?) -> Unit): FileChooser {
    val context = LocalContext.current
    val result = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            context.contentResolver.openInputStream(uri)?.source()?.let { onFileFound(it, name) }
        }
    }

    return remember { FileChooser(result) }
}

@Composable
fun rememberFileChooser(onFileFound: (Source) -> Unit): FileChooser {
    val context = LocalContext.current
    val result = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) {
            context.contentResolver.openInputStream(it)?.source()?.let(onFileFound)
        }
    }

    return remember { FileChooser(result) }
}
