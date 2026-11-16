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
package com.blacksquircle.ui.feature.editor.ui.editor.compose

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.blacksquircle.ui.feature.editor.ui.editor.model.DocumentState
import timber.log.Timber
import java.io.File

private const val MAX_TEXT_EXTRA = 100_000

/** Shares the saved file when possible, otherwise the current text */
internal fun Context.shareDocument(state: DocumentState) {
    val document = state.document
    val text = state.content?.toString()
    try {
        val localFile = File(document.path)
        val shareSavedFile = document.scheme == "file" && !document.modified && localFile.isFile
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, document.displayName)
        }
        if (shareSavedFile || (text != null && text.length > MAX_TEXT_EXTRA)) {
            val file = if (shareSavedFile) {
                localFile
            } else {
                File(cacheDir, "share").apply { mkdirs() }
                    .resolve(document.displayName.ifBlank { "untitled.txt" })
                    .apply { writeText(text.orEmpty()) }
            }
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            intent.type = contentResolver.getType(uri) ?: "text/plain"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, text.orEmpty())
        }
        startActivity(Intent.createChooser(intent, document.displayName))
    } catch (e: Exception) {
        Timber.e(e, e.message)
    }
}
