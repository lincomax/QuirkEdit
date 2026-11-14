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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.blacksquircle.ui.ds.SquircleTheme
import com.blacksquircle.ui.ds.dialog.AlertDialog
import com.blacksquircle.ui.feature.editor.R
import io.github.rosemoe.sora.text.Content

private data class TextStatistics(
    val lines: Int,
    val words: Int,
    val chars: Int,
    val charsNoSpaces: Int,
)

private fun countStatistics(text: CharSequence, lines: Int): TextStatistics {
    var words = 0
    var charsNoSpaces = 0
    var inWord = false
    for (i in 0 until text.length) {
        val c = text[i]
        if (c.isWhitespace()) {
            inWord = false
        } else {
            charsNoSpaces++
            if (!inWord) {
                words++
                inWord = true
            }
        }
    }
    return TextStatistics(
        lines = lines,
        words = words,
        chars = text.length,
        charsNoSpaces = charsNoSpaces,
    )
}

@Composable
internal fun StatisticsDialog(
    content: Content,
    onDismiss: () -> Unit,
) {
    val statistics = remember(content) {
        countStatistics(content, content.lineCount)
    }
    Dialog(onDismissRequest = onDismiss) {
        AlertDialog(
            title = stringResource(R.string.editor_menu_statistics),
            content = {
                Column(Modifier.fillMaxWidth()) {
                    StatisticsRow(stringResource(R.string.editor_statistics_lines), statistics.lines)
                    StatisticsRow(stringResource(R.string.editor_statistics_words), statistics.words)
                    StatisticsRow(stringResource(R.string.editor_statistics_chars), statistics.chars)
                    StatisticsRow(
                        stringResource(R.string.editor_statistics_chars_no_spaces),
                        statistics.charsNoSpaces,
                    )
                }
            },
            confirmButton = stringResource(android.R.string.ok),
            onConfirmClicked = onDismiss,
        )
    }
}

@Composable
private fun StatisticsRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = SquircleTheme.colors.colorTextAndIconSecondary,
            style = SquircleTheme.typography.text16Regular,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = "%,d".format(value),
            color = SquircleTheme.colors.colorTextAndIconPrimary,
            style = SquircleTheme.typography.text16Regular,
        )
    }
}
