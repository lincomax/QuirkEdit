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
import android.graphics.Paint
import io.github.rosemoe.sora.widget.EditorRenderer
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Changes:
 * - Current line highlight can use a custom color
 * - In word wrap mode, only the visual row with the cursor is highlighted
 * - The highlight stays out of the line number column
 */
internal class SquircleRenderer(private val codeEditor: CodeEditor) : EditorRenderer(codeEditor) {

    private val highlightPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    override fun drawRowBackground(canvas: Canvas, color: Int, row: Int, right: Int) {
        val currentLineColor = codeEditor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
        if (color != currentLineColor || !codeEditor.isHighlightCurrentLine) {
            super.drawRowBackground(canvas, color, row, right)
            return
        }
        if (right < codeEditor.width) {
            /** This is the line number column pass, leave it alone */
            return
        }
        if (codeEditor.isWordwrap && row != cursorRow()) {
            return
        }
        val override = codeEditor.highlightColor
        highlightPaint.color = if (override != 0) override else color
        val top = codeEditor.getRowTop(row) - codeEditor.offsetY
        val bottom = codeEditor.getRowBottom(row) - codeEditor.offsetY
        canvas.drawRect(textRegionLeft(), top.toFloat(), right.toFloat(), bottom.toFloat(), highlightPaint)
    }

    /** Where the text area starts, to the right of the line numbers */
    private fun textRegionLeft(): Float {
        if (!codeEditor.isLineNumberEnabled) {
            return 0f
        }
        val offset = codeEditor.measureTextRegionOffset() - codeEditor.dividerMarginRight
        val left = if (codeEditor.isLineNumberPinned) offset else offset - codeEditor.offsetX
        return left.coerceAtLeast(0f)
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
