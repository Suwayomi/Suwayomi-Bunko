/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.settings

import dev.bunko.ui.base.userMessage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bunko.core.io.copyTo
import dev.bunko.core.io.deleteTemp
import dev.bunko.core.io.saveTo
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.backup.interactor.ExportBackupFile
import dev.bunko.domain.backup.interactor.ImportBackupFile
import dev.bunko.domain.backup.interactor.ValidateBackupFile
import dev.bunko.domain.backup.model.RestoreState
import dev.bunko.domain.backup.model.RestoreStatus
import dev.bunko.i18n.MR
import dev.bunko.ui.base.file.rememberFileChooser
import dev.bunko.ui.base.file.rememberFileSaver
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.base.navigation.Toolbar
import dev.bunko.ui.base.prefs.PreferenceRow
import dev.bunko.ui.main.components.bottomNav
import dev.bunko.ui.viewModel
import dev.bunko.uicore.components.VerticalScrollbar
import dev.bunko.uicore.components.rememberScrollbarAdapter
import dev.bunko.uicore.resources.stringResource
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.diamondedge.logging.logging
import io.ktor.client.plugins.onDownload
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer
import kotlin.random.Random
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.base.dialog.rememberDialogState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

class SettingsBackupScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { settingsBackupViewModel() }
        SettingsBackupScreenContent(
            restoreStatus = vm.restoreStatus.collectAsState().value,
            creatingStatus = vm.creatingStatus.collectAsState().value,
            missingSourceFlowHolder = vm.missingSourceFlow,
            createFlowHolder = vm.createFlow,
            restoreFile = vm::restoreFile,
            restoreBackup = vm::restoreBackup,
            stopRestore = vm::stopRestore,
            exportBackup = vm::exportBackup,
            exportBackupFileFound = vm::exportBackupFileFound,
        )
    }
}

@Inject
class SettingsBackupViewModel(
    private val validateBackupFile: ValidateBackupFile,
    private val importBackupFile: ImportBackupFile,
    private val exportBackupFile: ExportBackupFile,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    private val _restoreStatus = MutableStateFlow<Status>(Status.Nothing)
    val restoreStatus = _restoreStatus.asStateFlow()

    private val _missingSourceFlow = MutableSharedFlow<Pair<Path, ImmutableList<String>>>()
    val missingSourceFlow = StableHolder(_missingSourceFlow.asSharedFlow())

    private val _creatingStatus = MutableStateFlow<Status>(Status.Nothing)
    val creatingStatus = _creatingStatus.asStateFlow()
    private val _createFlow = MutableSharedFlow<String>()
    val createFlow = StableHolder(_createFlow.asSharedFlow())

    fun restoreFile(source: Source) {
        scope.launch {
            val file = try {
                FileSystem.SYSTEM_TEMPORARY_DIRECTORY
                    .resolve("bunko.${Random.nextLong()}.tachibk")
                    .also { file ->
                        source.saveTo(file)
                    }
            } catch (e: Exception) {
                log.warn(e) { "Error creating backup file" }
                _restoreStatus.value = Status.Error
                e.throwIfCancellation()
                return@launch
            }

            var handedOff = false

            validateBackupFile.asFlow(file)
                .onEach { (missingSources) ->
                    handedOff = true
                    if (missingSources.isEmpty()) {
                        restoreBackup(file)
                    } else {
                        _missingSourceFlow.emit(file to missingSources.toImmutableList())
                    }
                }
                .catch {
                    toast(it.userMessage.toPlatformString())
                    log.warn(it) { "Error importing backup" }
                    _restoreStatus.value = Status.Error
                }
                .onCompletion { if (!handedOff) file.deleteTemp() }
                .collect()
        }
    }

    fun restoreBackup(file: Path) {
        importBackupFile
            .asFlow(file)
            .onStart {
                _restoreStatus.value = Status.InProgress(null)
            }
            .onEach {
                _restoreStatus.value = it.second.toStatus()
            }
            .catch {
                toast(it.userMessage.toPlatformString())
                log.warn(it) { "Error importing backup" }
                _restoreStatus.value = Status.Error
            }
            .onCompletion { file.deleteTemp() }
            .launchIn(scope)
    }

    private fun RestoreStatus.toStatus() =
        when (state) {
            RestoreState.IDLE -> Status.Success
            RestoreState.SUCCESS -> Status.Success
            RestoreState.FAILURE -> Status.Error
            RestoreState.RESTORING_CATEGORIES -> Status.InProgress(0.01f)
            RestoreState.RESTORING_SETTINGS -> Status.InProgress(0.02f)
            RestoreState.RESTORING_MANGA -> Status.InProgress((completed.toFloat() / total).coerceIn(0f, 0.99f))
            RestoreState.RESTORING_META -> Status.InProgress(1f)
            RestoreState.UNKNOWN -> Status.Error
        }

    fun stopRestore(file: Path?) {
        _restoreStatus.value = Status.Error
        file?.let { scope.launch { it.deleteTemp() } }
    }

    private val tempFile = MutableStateFlow<Path?>(null)
    private val mutex = Mutex()

    fun exportBackup() {
        exportBackupFile
            .asFlow(
                true,
                true,
            ) {
                onDownload { bytesSentTotal, contentLength ->
                    _creatingStatus.value = Status.InProgress(
                        (bytesSentTotal.toFloat() / (contentLength ?: Long.MAX_VALUE))
                            .coerceAtMost(0.99F),
                    )
                }
            }
            .onStart {
                _creatingStatus.value = Status.InProgress(null)
            }
            .onEach { (filename, source) ->
                tempFile.value = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve(filename).also {
                    mutex.tryLock()
                    scope.launch {
                        try {
                            source.saveTo(it)
                        } catch (e: Exception) {
                            e.throwIfCancellation()
                            log.warn(e) { "Error creating backup" }
                            _creatingStatus.value = Status.Error
                        } finally {
                            mutex.unlock()
                        }
                    }
                }
                _createFlow.emit(filename)
            }
            .catch {
                toast(it.userMessage.toPlatformString())
                log.warn(it) { "Error exporting backup" }
                _creatingStatus.value = Status.Error
            }
            .launchIn(scope)
    }

    fun exportBackupFileFound(backupSink: Sink) {
        scope.launch {
            mutex.withLock {
                val file = tempFile.value
                if (creatingStatus.value is Status.InProgress && file != null) {
                    try {
                        FileSystem.SYSTEM.source(file).copyTo(backupSink.buffer())
                        _creatingStatus.value = Status.Success
                    } catch (e: Exception) {
                        e.throwIfCancellation()
                        log.error(e) { "Error moving created backup" }
                        _creatingStatus.value = Status.Error
                    } finally {
                        file.deleteTemp()
                        tempFile.value = null
                    }
                } else {
                    _creatingStatus.value = Status.Error
                }
            }
        }
    }

    private companion object {
        private val log = logging()
    }
}

sealed class Status {
    data object Nothing : Status()

    data class InProgress(
        val progress: Float?,
    ) : Status()

    data object Success : Status()

    data object Error : Status()
}

@Composable
private fun SettingsBackupScreenContent(
    restoreStatus: Status,
    creatingStatus: Status,
    missingSourceFlowHolder: StableHolder<SharedFlow<Pair<Path, ImmutableList<String>>>>,
    createFlowHolder: StableHolder<SharedFlow<String>>,
    restoreFile: (Source) -> Unit,
    restoreBackup: (Path) -> Unit,
    stopRestore: (Path?) -> Unit,
    exportBackup: () -> Unit,
    exportBackupFileFound: (Sink) -> Unit,
) {
    var backupFile by remember { mutableStateOf<Path?>(null) }
    var missingSources: ImmutableList<String> by remember { mutableStateOf(persistentListOf()) }
    val dialogState = rememberDialogState()
    val fileSaver = rememberFileSaver(exportBackupFileFound)
    val fileChooser = rememberFileChooser(restoreFile)
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            missingSourceFlowHolder.item.collect { (backup, sources) ->
                backupFile = backup
                missingSources = sources
                dialogState.show()
            }
        }
        launch(Dispatchers.IO) {
            createFlowHolder.item.collect { filename ->
                fileSaver.save(filename)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.statusBars.add(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
            ),
        ),
        topBar = {
            Toolbar(stringResource(MR.strings.settings_backup_screen))
        },
    ) {
        Box(Modifier.padding(it)) {
            val state = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = WindowInsets.bottomNav.add(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom,
                    ),
                ).asPaddingValues(),
            ) {
                item {
                    PreferenceFile(
                        stringResource(MR.strings.backup_restore),
                        stringResource(MR.strings.backup_restore_sub),
                        restoreStatus,
                    ) {
                        fileChooser.launch("gz", "tachibk")
                    }
                    PreferenceFile(
                        stringResource(MR.strings.backup_create),
                        stringResource(MR.strings.backup_create_sub),
                        creatingStatus,
                        exportBackup,
                    )
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(state),
                Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .windowInsetsPadding(
                        WindowInsets.bottomNav.add(
                            WindowInsets.navigationBars.only(
                                WindowInsetsSides.Bottom,
                            ),
                        ),
                    ),
            )
        }
    }
    MissingSourcesDialog(
        dialogState,
        missingSources,
        onPositiveClick = {
            restoreBackup(backupFile ?: return@MissingSourcesDialog)
        },
        onNegativeClick = { stopRestore(backupFile) },
    )
}

@Composable
private fun MissingSourcesDialog(
    state: DialogState,
    missingSources: ImmutableList<String>,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit,
) {
    AppDialog(
        state = state,
        title = stringResource(MR.strings.missing_sources),
        confirm = DialogButton(stringResource(MR.strings.action_ok), onClick = onPositiveClick),
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onNegativeClick,
    ) {
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(missingSources) { source ->
                Text(source, style = MaterialTheme.typography.bodyLarge, modifier = DialogContentPadding.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun PreferenceFile(
    title: String,
    subtitle: String,
    status: Status,
    onClick: () -> Unit,
) {
    PreferenceRow(
        title = title,
        onClick = onClick,
        enabled = status !is Status.InProgress,
        subtitle = subtitle,
    ) {
        val modifier = Modifier.align(Alignment.Center)
            .size(24.dp)
        if (status is Status.InProgress) {
            if (status.progress != null && !status.progress.isNaN()) {
                CircularProgressIndicator(
                    progress = { status.progress },
                    modifier = modifier,
                )
            } else {
                CircularProgressIndicator(modifier)
            }
        } else if (status != Status.Nothing) {
            when (status) {
                Status.Error -> Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    modifier = modifier,
                    tint = Color.Red,
                )

                Status.Success -> Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = modifier,
                )

                else -> Unit
            }
        }
    }
}
