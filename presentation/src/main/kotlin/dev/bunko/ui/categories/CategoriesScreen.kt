/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.categories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.bunko.ui.categories.components.CategoriesScreenContent
import dev.bunko.ui.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import kotlin.jvm.Transient

class CategoriesScreen(
    @Transient
    private val notifyFinished: (() -> Unit)? = null,
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val vm = viewModel { categoryViewModel() }
        CategoriesScreenContent(
            categories = vm.categories.collectAsState().value,
            updateRemoteCategories = vm::updateRemoteCategories,
            moveCategoryUp = vm::moveUp,
            moveCategoryDown = vm::moveDown,
            renameCategory = vm::renameCategory,
            deleteCategory = vm::deleteCategory,
            createCategory = vm::createCategory,
            notifyFinished = notifyFinished,
        )
    }
}
