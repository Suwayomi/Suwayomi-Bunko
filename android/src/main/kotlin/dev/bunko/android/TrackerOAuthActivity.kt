/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TrackerOAuthActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data?.toString()
        if (uri != null) {
            val trackerOAuth = AppComponent.getInstance(applicationContext).trackerOAuth
            ProcessLifecycleOwner.get().lifecycleScope.launch { trackerOAuth.handleCallback(uri) }
        }
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        }
        finish()
    }
}
