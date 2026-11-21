/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: error("targetAppId was not passed to the instrumentation"),
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
            scrollMainList()
            TABS.forEach { openTab(it) }
        }

    private fun MacrobenchmarkScope.openTab(label: String) {
        val tab = device.findObject(By.text(label)) ?: return
        tab.click()
        device.wait(Until.hasObject(By.scrollable(true)), WAIT_MS)
        device.waitForIdle()
        scrollMainList()
    }

    private fun MacrobenchmarkScope.scrollMainList() {
        val list = device.findObject(By.scrollable(true)) ?: return
        list.setGestureMargin(device.displayWidth / 5)
        list.fling(Direction.DOWN)
        device.waitForIdle()
        list.fling(Direction.UP)
        device.waitForIdle()
    }

    private companion object {
        const val WAIT_MS = 3_000L
        val TABS = listOf("History", "Schedule", "Browse", "More", "Library")
    }
}
