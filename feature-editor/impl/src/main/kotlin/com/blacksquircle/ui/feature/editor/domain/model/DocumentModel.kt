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

package com.blacksquircle.ui.feature.editor.domain.model

import android.net.Uri
import java.io.File

internal data class DocumentModel(
    val uuid: String,
    val fileUri: String,
    val filesystemUuid: String,
    val displayName: String,
    val language: String,
    val modified: Boolean,
    val position: Int,
    val scrollX: Int,
    val scrollY: Int,
    val selectionStart: Int,
    val selectionEnd: Int,
    val gitRepository: String?,
) {
    val scheme: String
        get() = fileUri.substringBefore("://")
    val path: String
        get() = fileUri.substringAfterLast("://").ifEmpty(File::separator)
    val extension: String
        get() = "." + displayName.substringAfterLast('.')

    /** A new tab that hasn't been saved anywhere yet */
    val isUntitled: Boolean
        get() = fileUri.startsWith(UNTITLED_PREFIX)

    /** [displayPath] with [prefix] removed from the start, cut only at a whole folder */
    fun shortPath(prefix: String): String? {
        val full = displayPath ?: return null
        val cut = prefix.trim().trimEnd('/')
        return when {
            cut.isEmpty() -> full
            full == cut -> "/"
            full.startsWith("$cut/") -> full.removePrefix(cut)
            else -> full
        }
    }

    /** Human-readable location of the file, shown under the file name */
    val displayPath: String?
        get() = when {
            isUntitled -> null
            scheme == "file" -> path
            scheme == "content" -> {
                val decoded = Uri.decode(fileUri)
                val documentId = decoded.substringAfterLast("/document/", "")
                when {
                    documentId.startsWith("primary:") ->
                        "/storage/emulated/0/" + documentId.removePrefix("primary:")
                    documentId.startsWith("raw:") -> documentId.removePrefix("raw:")
                    documentId.contains(":") && !documentId.startsWith("msf:") ->
                        "/storage/" + documentId.replaceFirst(":", "/")
                    else -> decoded
                }
            }
            else -> fileUri
        }

    companion object {
        const val UNTITLED_PREFIX = "untitled://"
    }
}