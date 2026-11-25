/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.extensions

import dev.bunko.domain.ServerListeners
import dev.bunko.ui.base.userMessage
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.intl.Locale
import dev.bunko.core.io.deleteTemp
import dev.bunko.core.io.saveTo
import dev.bunko.core.lang.displayName
import dev.bunko.core.lang.throwIfCancellation
import dev.bunko.domain.extension.interactor.GetExtensionList
import dev.bunko.domain.extension.interactor.InstallExtension
import dev.bunko.domain.extension.interactor.InstallExtensionFile
import dev.bunko.domain.extension.interactor.UninstallExtension
import dev.bunko.domain.extension.interactor.UpdateExtension
import dev.bunko.domain.extension.model.Extension
import dev.bunko.domain.extension.service.ExtensionPreferences
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.StringResource
import dev.bunko.uicore.vm.ContextWrapper
import dev.bunko.uicore.vm.ViewModel
import com.diamondedge.logging.logging
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import okio.FileSystem
import okio.Source
import kotlin.random.Random

data class ExtensionsState(
    val extensionList: List<Extension>? = null,
    val searchQuery: String? = null,
    val workingExtensions: List<String> = emptyList()
)

@Inject
class ExtensionsScreenViewModel(
    private val getExtensionList: GetExtensionList,
    private val installExtensionFile: InstallExtensionFile,
    private val installExtension: InstallExtension,
    private val updateExtension: UpdateExtension,
    private val uninstallExtension: UninstallExtension,
    private val serverListeners: ServerListeners,
    extensionPreferences: ExtensionPreferences,
    contextWrapper: ContextWrapper,
) : ViewModel(contextWrapper) {
    private val _state = MutableStateFlow(ExtensionsState())
    val state = _state.asStateFlow()

    private val _enabledLangs = extensionPreferences.languages().asStateFlow()
    val enabledLangs = _enabledLangs.map { it.toImmutableSet() }
        .stateIn(scope, SharingStarted.Eagerly, persistentSetOf())

    val extensions = combine(
        _state,
        enabledLangs
    ) { state, enabledLangs ->
        search(
            state.searchQuery,
            state.extensionList,
            enabledLangs,
            state.workingExtensions
        )
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    val availableLangs = _state.map { state ->
        state.extensionList?.map { it.lang }.orEmpty().distinct().toImmutableList()
    }.stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    val isLoading = _state.map { state ->
        state.extensionList == null
    }.stateIn(scope, SharingStarted.Eagerly, true)

    init {
        scope.launch {
            getExtensions()
        }
    }

    private suspend fun getExtensions() {
        val extensionList = getExtensionList.await(onError = { toast(it.userMessage.toPlatformString()) }).orEmpty()
        _state.update { current ->
            current.copy(extensionList = extensionList)
        }
    }

    fun install(
        source: Source,
        fileName: String?,
    ) {
        log.info { "Install file clicked" }
        scope.launch {
            val type = if (fileName?.endsWith(".jar", ignoreCase = true) == true) "jar" else "apk"
            val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("bunko.${Random.nextLong()}.$type")
            try {
                source.saveTo(file)
                installExtensionFile.await(file, onError = { toast(it.userMessage.toPlatformString()) })
            } catch (e: Exception) {
                log.warn(e) { "Error creating apk file" }
                e.throwIfCancellation()
            } finally {
                file.deleteTemp()
            }

            getExtensions()
            serverListeners.updateSources()
        }
    }

    fun install(extension: Extension) {
        log.info { "Install clicked" }
        runExtensionAction(extension, MR.strings.extension_installed_toast) { onError ->
            installExtension.await(extension, onError)
        }
    }

    fun update(extension: Extension) {
        log.info { "Update clicked" }
        runExtensionAction(extension, MR.strings.extension_updated_toast) { onError ->
            updateExtension.await(extension, onError)
        }
    }

    fun uninstall(extension: Extension) {
        log.info { "Uninstall clicked" }
        runExtensionAction(extension, MR.strings.extension_uninstalled_toast) { onError ->
            uninstallExtension.await(extension, onError)
        }
    }

    private fun runExtensionAction(
        extension: Extension,
        successMessage: StringResource,
        action: suspend (onError: suspend (Throwable) -> Unit) -> Unit,
    ) {
        scope.launch {
            _state.update { current ->
                current.copy(workingExtensions = current.workingExtensions + extension.pkgName)
            }
            var failed = false
            action {
                failed = true
                toast(it.userMessage.toPlatformString())
            }
            getExtensions()
            serverListeners.updateSources()
            _state.update { current ->
                current.copy(workingExtensions = current.workingExtensions - extension.pkgName)
            }
            if (!failed) {
                toast(successMessage.toPlatformString(extension.name))
            }
        }
    }

    fun setEnabledLanguages(langs: Set<String>) {
        _enabledLangs.value = langs
    }

    fun setQuery(query: String) {
        _state.update { current ->
            current.copy(searchQuery = query)
        }
    }

    private fun search(
        searchQuery: String?,
        extensionList: List<Extension>?,
        enabledLangs: Set<String>,
        workingExtensions: List<String>,
    ): ImmutableList<ExtensionUI> {
        val extensions = extensionList?.filter { it.lang in enabledLangs }
            .orEmpty()
        return if (searchQuery.isNullOrBlank()) {
            extensions.splitSort(workingExtensions)
        } else {
            val queries = searchQuery.split(" ")
            val filteredExtensions = extensions.toMutableList()
            queries.forEach { query ->
                filteredExtensions.removeAll { !it.name.contains(query, true) }
            }
            filteredExtensions.toList().splitSort(workingExtensions)
        }
    }

    private fun List<Extension>.splitSort(workingExtensions: List<String>): ImmutableList<ExtensionUI> {
        val all = MR.strings.all.toPlatformString()
        val availableLabel = MR.strings.available.toPlatformString()
        val byName = compareBy<Extension, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
        fun List<Extension>.toItems() = map { ExtensionUI.ExtensionItem(it, it.pkgName in workingExtensions) }
        fun section(title: String, items: List<Extension>): List<ExtensionUI> =
            if (items.isEmpty()) emptyList() else listOf(ExtensionUI.Header(title)) + items.toItems()

        val (installed, notInstalled) = partition(Extension::installed)
        val (updates, rest) = installed.partition { it.hasUpdate && !it.obsolete }

        return (
            section(
                "${MR.strings.updates_available.toPlatformString()} (${updates.size})",
                updates.sortedWith(byName),
            ) +
                section(
                    MR.strings.installed.toPlatformString(),
                    rest.sortedWith(compareBy<Extension> { !it.obsolete }.then(byName)),
                ) +
                notInstalled
                    .groupBy(Extension::lang)
                    .mapKeys { (key) ->
                        when (key) {
                            "all" -> all
                            else -> Locale(key).displayName
                        }
                    }
                    .toList()
                    .sortedWith(
                        compareBy<Pair<String, *>> { (key) ->
                            if (key == all) 1 else 2
                        }.thenBy(String.CASE_INSENSITIVE_ORDER, Pair<String, *>::first),
                    )
                    .flatMap { (lang, items) ->
                        section("$availableLabel · $lang", items.sortedWith(byName))
                    }
            ).toImmutableList()
    }

    private companion object {
        private val log = logging()
    }
}

@Immutable
sealed class ExtensionUI {
    data class Header(
        val header: String,
    ) : ExtensionUI()

    data class ExtensionItem(
        val extension: Extension,
        val isWorking: Boolean = false,
    ) : ExtensionUI()
}
