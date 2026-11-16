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

package com.blacksquircle.ui.feature.editor.ui.editor.compose.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blacksquircle.ui.ds.checkbox.CheckBox
import com.blacksquircle.ui.ds.divider.HorizontalDivider
import com.blacksquircle.ui.ds.popupmenu.PopupMenu
import com.blacksquircle.ui.ds.popupmenu.PopupMenuItem
import com.blacksquircle.ui.feature.editor.R
import com.blacksquircle.ui.ds.R as UiR

@Composable
internal fun OtherMenu(
    showGit: Boolean,
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    miniToolbar: Boolean = true,
    symbolBar: Boolean = true,
    wordWrap: Boolean = false,
    readOnly: Boolean = false,
    onFindClicked: () -> Unit = {},
    onShareClicked: () -> Unit = {},
    onGoToLineClicked: () -> Unit = {},
    onForceSyntaxClicked: () -> Unit = {},
    onStatisticsClicked: () -> Unit = {},
    onGitClicked: () -> Unit = {},
    onTerminalClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onMiniToolbarClicked: () -> Unit = {},
    onSymbolBarClicked: () -> Unit = {},
    onWordWrapClicked: () -> Unit = {},
    onReadOnlyClicked: () -> Unit = {},
) {
    PopupMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        verticalOffset = (-56).dp,
        modifier = modifier,
    ) {
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_search),
            iconResId = UiR.drawable.ic_search,
            onClick = onFindClicked,
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_share),
            iconResId = UiR.drawable.ic_share,
            onClick = onShareClicked,
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_goto_line),
            iconResId = UiR.drawable.ic_goto_line,
            onClick = onGoToLineClicked,
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_syntax),
            iconResId = UiR.drawable.ic_syntax,
            onClick = onForceSyntaxClicked,
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_statistics),
            iconResId = UiR.drawable.ic_statistics,
            onClick = onStatisticsClicked,
        )
        if (showGit) {
            PopupMenuItem(
                title = stringResource(R.string.editor_menu_git),
                iconResId = UiR.drawable.ic_git,
                onClick = onGitClicked,
                submenu = true,
            )
        }
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_terminal),
            iconResId = UiR.drawable.ic_console,
            onClick = onTerminalClicked,
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_settings),
            iconResId = UiR.drawable.ic_settings,
            onClick = onSettingsClicked,
        )
        HorizontalDivider()
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_mini_toolbar),
            iconResId = UiR.drawable.ic_mini_toolbar,
            onClick = onMiniToolbarClicked,
            trailing = { CheckBox(checked = miniToolbar, onClick = onMiniToolbarClicked) },
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_symbol_bar),
            iconResId = UiR.drawable.ic_keyboard,
            onClick = onSymbolBarClicked,
            trailing = { CheckBox(checked = symbolBar, onClick = onSymbolBarClicked) },
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_word_wrap),
            iconResId = UiR.drawable.ic_wrap,
            onClick = onWordWrapClicked,
            trailing = { CheckBox(checked = wordWrap, onClick = onWordWrapClicked) },
        )
        PopupMenuItem(
            title = stringResource(R.string.editor_menu_file_read_only),
            iconResId = UiR.drawable.ic_lock,
            onClick = onReadOnlyClicked,
            trailing = { CheckBox(checked = readOnly, onClick = onReadOnlyClicked) },
        )
    }
}
