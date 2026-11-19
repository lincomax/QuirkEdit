/*
 * Copyright Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blacksquircle.ui.feature.terminal.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.blacksquircle.ui.feature.terminal.R
import timber.log.Timber

/** Receives Termux's result for a run command and explains why it was refused */
internal class TermuxResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = intent.getBundleExtra(EXTRA_RESULT) ?: return
        val error = result.getInt(EXTRA_ERR, ERR_SUCCESS)
        if (error == ERR_SUCCESS) return
        val errorMessage = result.getString(EXTRA_ERRMSG).orEmpty()
        Timber.w("Termux refused command: %s", errorMessage)
        val message = if (errorMessage.contains("allow-external-apps", ignoreCase = true)) {
            context.getString(R.string.terminal_termux_external_apps)
        } else {
            context.getString(R.string.terminal_termux_refused, errorMessage.lineSequence().first())
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        /** Keys used by Termux's RUN_COMMAND result bundle */
        private const val EXTRA_RESULT = "result"
        private const val EXTRA_ERR = "err"
        private const val EXTRA_ERRMSG = "errmsg"
        private const val ERR_SUCCESS = -1
    }
}
