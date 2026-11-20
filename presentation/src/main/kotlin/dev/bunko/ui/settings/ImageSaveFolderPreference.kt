package dev.bunko.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.bunko.i18n.MR
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.uicore.resources.stringResource
import kotlinx.coroutines.flow.MutableStateFlow

private const val TREE_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

@Composable
fun ImageSaveFolderPreference(folder: MutableStateFlow<String>) {
    val resolver = LocalContext.current.contentResolver
    val value by folder.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            resolver.takePersistableUriPermission(uri, TREE_FLAGS)
            folder.value = uri.toString()
        }
    }
    PreferenceRow(
        title = stringResource(MR.strings.image_save_folder),
        subtitle = value.takeIf { it.isNotEmpty() }?.let { DocumentsContract.getTreeDocumentId(Uri.parse(it)).replace(':', '/') }
            ?: stringResource(MR.strings.image_save_folder_default),
        onClick = { picker.launch(null) },
    )
    PreferenceRow(
        title = stringResource(MR.strings.image_save_folder_reset),
        enabled = value.isNotEmpty(),
        onClick = {
            runCatching { resolver.releasePersistableUriPermission(Uri.parse(value), TREE_FLAGS) }
            folder.value = ""
        },
    )
}
