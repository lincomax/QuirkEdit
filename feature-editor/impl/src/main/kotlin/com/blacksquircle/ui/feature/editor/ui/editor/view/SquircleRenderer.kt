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

import android.graphics.Canvas
import io.github.rosemoe.sora.widget.EditorRenderer
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Changes:
 * - Current line highlight can use a custom color
 * - In word wrap mode, only the visual row with the cursor is highlighted
 */
internal class SquircleRenderer(private val codeEditor: CodeEditor) : EditorRenderer(codeEditor) {

    override fun drawRowBackground(canvas: Canvas, color: Int, row: Int, right: Int) {
        val currentLineColor = codeEditor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
        if (color != currentLineColor || !codeEditor.isHighlightCurrentLine) {
            super.drawRowBackground(canvas, color, row, right)
            return
        }
        if (codeEditor.isWordwrap && row != cursorRow()) {
            return
        }
        val override = codeEditor.highlightColor
        super.drawRowBackground(canvas, if (override != 0) override else color, row, right)
    }

    private fun cursorRow(): Int {
        val cursor = codeEditor.cursor
        val rowHeight = codeEditor.rowHeight
        if (rowHeight <= 0) {
            return -1
        }
        val offset = codeEditor.layout.getCharLayoutOffset(cursor.leftLine, cursor.leftColumn)
        return (offset[0] / rowHeight).toInt() - 1
    }
}
