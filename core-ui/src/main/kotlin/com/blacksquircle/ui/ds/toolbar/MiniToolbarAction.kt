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
package com.blacksquircle.ui.ds.toolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blacksquircle.ui.ds.R

enum class MiniToolbarAction(
    val key: String,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
) {
    OPEN("open", R.drawable.ic_folder_open, R.string.mini_toolbar_open),
    NEW("new", R.drawable.ic_plus, R.string.mini_toolbar_new),
    PASTE("paste", R.drawable.ic_paste, R.string.mini_toolbar_paste),
    COPY("copy", R.drawable.ic_copy, R.string.mini_toolbar_copy),
    CUT("cut", R.drawable.ic_cut, R.string.mini_toolbar_cut),
    SELECT_ALL("select_all", R.drawable.ic_select_all, R.string.mini_toolbar_select_all),
    UNDO("undo", R.drawable.ic_undo, R.string.mini_toolbar_undo),
    REDO("redo", R.drawable.ic_redo, R.string.mini_toolbar_redo),
    SEARCH("search", R.drawable.ic_search, R.string.mini_toolbar_search),
    GO_TO_LINE("goto", R.drawable.ic_goto_line, R.string.mini_toolbar_goto_line),
    SAVE("save", R.drawable.ic_save, R.string.mini_toolbar_save),
    SAVE_AS("save_as", R.drawable.ic_save_as, R.string.mini_toolbar_save_as),
    CLOSE("close", R.drawable.ic_close, R.string.mini_toolbar_close),
}

data class MiniToolbarItem(
    val action: MiniToolbarAction,
    val enabled: Boolean,
)

/** Stored as "key:y;key:n;..." in display order */
object MiniToolbarItems {

    private val DEFAULT_ENABLED = listOf(
        MiniToolbarAction.OPEN,
        MiniToolbarAction.PASTE,
        MiniToolbarAction.UNDO,
        MiniToolbarAction.REDO,
        MiniToolbarAction.SEARCH,
        MiniToolbarAction.SAVE,
        MiniToolbarAction.CLOSE,
    )

    val DEFAULT: List<MiniToolbarItem> =
        DEFAULT_ENABLED.map { MiniToolbarItem(it, true) } +
            MiniToolbarAction.entries
                .filterNot { it in DEFAULT_ENABLED }
                .map { MiniToolbarItem(it, false) }

    fun parse(value: String): List<MiniToolbarItem> {
        if (value.isBlank()) return DEFAULT
        val byKey = MiniToolbarAction.entries.associateBy { it.key }
        val items = value.split(';').mapNotNull { entry ->
            val parts = entry.split(':')
            val action = byKey[parts.getOrNull(0)] ?: return@mapNotNull null
            MiniToolbarItem(action, parts.getOrNull(1) == "y")
        }.distinctBy { it.action }
        val missing = MiniToolbarAction.entries
            .filter { action -> items.none { it.action == action } }
            .map { MiniToolbarItem(it, false) }
        return items + missing
    }

    fun serialize(items: List<MiniToolbarItem>): String {
        return items.joinToString(";") { "${it.action.key}:${if (it.enabled) "y" else "n"}" }
    }
}
