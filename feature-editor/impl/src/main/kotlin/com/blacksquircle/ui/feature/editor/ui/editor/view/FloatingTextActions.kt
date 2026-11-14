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
package com.blacksquircle.ui.feature.editor.ui.editor.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow

/**
 * Replaces the editor's built-in popup with Android's standard floating
 * text selection toolbar (Select All, Cut, Copy, Paste and an overflow menu).
 */
internal class FloatingTextActions(
    private val codeEditor: CodeEditor,
) : EditorTextActionWindow(codeEditor) {

    private var actionMode: ActionMode? = null

    private val callback = object : ActionMode.Callback2() {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            populateMenu(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            populateMenu(menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val cursor = codeEditor.cursor
            when (item.itemId) {
                ID_SELECT_ALL -> {
                    codeEditor.selectAll()
                    return true
                }
                ID_CUT -> {
                    if (cursor.isSelected) {
                        codeEditor.cutText()
                    }
                }
                ID_COPY -> {
                    codeEditor.copyText()
                    codeEditor.setSelection(cursor.rightLine, cursor.rightColumn)
                }
                ID_PASTE -> {
                    codeEditor.pasteText()
                    codeEditor.setSelection(cursor.rightLine, cursor.rightColumn)
                }
                else -> {
                    val intent = item.intent ?: return false
                    try {
                        codeEditor.context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // ignored
                    }
                }
            }
            mode.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            if (actionMode === mode) {
                actionMode = null
            }
        }

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
            computeContentRect(outRect)
        }
    }

    override fun displayWindow() {
        if (!isEnabled || codeEditor.snippetController.isInSnippet() ||
            !codeEditor.hasFocus() || codeEditor.isInMouseMode
        ) {
            return
        }
        val mode = actionMode
        if (mode != null) {
            mode.invalidate()
            mode.invalidateContentRect()
        } else {
            actionMode = codeEditor.startActionMode(callback, ActionMode.TYPE_FLOATING)
        }
    }

    override fun show() {
        displayWindow()
    }

    override fun isShowing(): Boolean {
        return actionMode != null
    }

    override fun dismiss() {
        val mode = actionMode
        actionMode = null
        mode?.finish()
    }

    private fun populateMenu(menu: Menu) {
        menu.clear()
        val cursor = codeEditor.cursor
        val selected = cursor.isSelected
        val editable = codeEditor.isEditable
        val allSelected = selected && cursor.left == 0 && cursor.right == codeEditor.text.length

        if (!allSelected && codeEditor.text.isNotEmpty()) {
            menu.add(Menu.NONE, ID_SELECT_ALL, 0, android.R.string.selectAll)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (selected && editable) {
            menu.add(Menu.NONE, ID_CUT, 1, android.R.string.cut)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (selected) {
            menu.add(Menu.NONE, ID_COPY, 2, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (editable && codeEditor.hasClip()) {
            menu.add(Menu.NONE, ID_PASTE, 3, android.R.string.paste)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        if (selected) {
            addProcessTextItems(menu, cursor.left, cursor.right)
        }
    }

    /** Adds apps that handle selected text (Translate, Search, etc.) to the overflow menu */
    private fun addProcessTextItems(menu: Menu, start: Int, end: Int) {
        val selectedText = try {
            codeEditor.text.subSequence(start, end).toString()
        } catch (e: Exception) {
            return
        }
        if (selectedText.isEmpty() || selectedText.length > MAX_PROCESS_TEXT_LENGTH) {
            return
        }
        val packageManager = codeEditor.context.packageManager
        val query = Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain")
        val activities = try {
            packageManager.queryIntentActivities(query, 0)
        } catch (e: Exception) {
            return
        }
        activities.forEachIndexed { index, info ->
            val intent = Intent(Intent.ACTION_PROCESS_TEXT)
                .setType("text/plain")
                .setClassName(info.activityInfo.packageName, info.activityInfo.name)
                .putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
                .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            menu.add(Menu.NONE, ID_PROCESS_TEXT + index, 100 + index, info.loadLabel(packageManager))
                .setIntent(intent)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
    }

    private fun computeContentRect(outRect: Rect) {
        val cursor = codeEditor.cursor
        val layout = codeEditor.layout
        val rowHeight = codeEditor.rowHeight
        val left = layout.getCharLayoutOffset(cursor.leftLine, cursor.leftColumn)
        val right = layout.getCharLayoutOffset(cursor.rightLine, cursor.rightColumn)
        val textOffset = codeEditor.measureTextRegionOffset() - codeEditor.offsetX
        val top = (left[0] - rowHeight - codeEditor.offsetY).toInt()
        val bottom = (right[0] - codeEditor.offsetY).toInt()
        val sameRow = left[0] == right[0]
        val x1 = if (sameRow) (left[1] + textOffset).toInt() else 0
        val x2 = if (sameRow) (right[1] + textOffset).toInt() else codeEditor.width
        outRect.set(
            x1.coerceIn(0, codeEditor.width),
            top.coerceIn(0, codeEditor.height),
            maxOf(x1 + 1, x2).coerceIn(0, codeEditor.width),
            bottom.coerceIn(0, codeEditor.height),
        )
    }

    companion object {
        private const val ID_SELECT_ALL = 1
        private const val ID_CUT = 2
        private const val ID_COPY = 3
        private const val ID_PASTE = 4
        private const val ID_PROCESS_TEXT = 100
        private const val MAX_PROCESS_TEXT_LENGTH = 100_000
    }
}
