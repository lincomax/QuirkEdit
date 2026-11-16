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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.blacksquircle.ui.ds.PreviewBackground
import com.blacksquircle.ui.ds.button.IconButton
import com.blacksquircle.ui.ds.toolbar.Toolbar
import com.blacksquircle.ui.ds.toolbar.ToolbarSizeDefaults
import com.blacksquircle.ui.feature.editor.R
import com.blacksquircle.ui.feature.editor.ui.editor.compose.menu.EditMenu
import com.blacksquircle.ui.feature.editor.ui.editor.compose.menu.FileMenu
import com.blacksquircle.ui.feature.editor.ui.editor.compose.menu.GitMenu
import com.blacksquircle.ui.feature.editor.ui.editor.compose.menu.OtherMenu
import com.blacksquircle.ui.feature.editor.ui.editor.model.DocumentState
import com.blacksquircle.ui.feature.editor.ui.editor.model.EditorSettings
import com.blacksquircle.ui.feature.editor.ui.editor.model.MenuType
import com.blacksquircle.ui.ds.R as UiR

/**
 * Toolbar: drawer button on the left,
 * and three menus on the right: File, Edit and More.
 */
@Composable
internal fun EditorToolbar(
    currentDocument: DocumentState?,
    modifier: Modifier = Modifier,
    settings: EditorSettings = EditorSettings(),
    onDrawerClicked: () -> Unit = {},
    onNewFileClicked: () -> Unit = {},
    onOpenFileClicked: () -> Unit = {},
    onSaveFileClicked: () -> Unit = {},
    onSaveFileAsClicked: () -> Unit = {},
    onReloadFileClicked: () -> Unit = {},
    onCloseFileClicked: () -> Unit = {},
    onCutClicked: () -> Unit = {},
    onCopyClicked: () -> Unit = {},
    onPasteClicked: () -> Unit = {},
    onSelectAllClicked: () -> Unit = {},
    onSelectLineClicked: () -> Unit = {},
    onDeleteLineClicked: () -> Unit = {},
    onDuplicateLineClicked: () -> Unit = {},
    onUndoClicked: () -> Unit = {},
    onRedoClicked: () -> Unit = {},
    onFindClicked: () -> Unit = {},
    onGoToLineClicked: () -> Unit = {},
    onForceSyntaxClicked: () -> Unit = {},
    onStatisticsClicked: () -> Unit = {},
    onInsertColorClicked: () -> Unit = {},
    onFetchClicked: () -> Unit = {},
    onPullClicked: () -> Unit = {},
    onCommitClicked: () -> Unit = {},
    onPushClicked: () -> Unit = {},
    onCheckoutClicked: () -> Unit = {},
    onTerminalClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onMiniToolbarClicked: () -> Unit = {},
    onSymbolBarClicked: () -> Unit = {},
    onWordWrapClicked: () -> Unit = {},
    onReadOnlyClicked: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var menuType by rememberSaveable {
        mutableStateOf<MenuType?>(null)
    }

    Toolbar(
        title = currentDocument?.document?.displayName,
        subtitle = currentDocument?.document?.displayPath,
        toolbarSize = ToolbarSizeDefaults.S,
        navigationIcon = UiR.drawable.ic_menu,
        onNavigationClicked = onDrawerClicked,
        navigationActions = {
            IconButton(
                iconResId = UiR.drawable.ic_folder_open,
                onClick = { menuType = MenuType.FILE },
                contentDescription = stringResource(R.string.editor_menu_file),
                anchor = {
                    FileMenu(
                        expanded = menuType == MenuType.FILE,
                        onDismiss = { menuType = null },
                        onNewFileClicked = { menuType = null; onNewFileClicked() },
                        onOpenFileClicked = { menuType = null; onOpenFileClicked() },
                        onSaveFileClicked = { menuType = null; onSaveFileClicked() },
                        onSaveFileAsClicked = { menuType = null; onSaveFileAsClicked() },
                        onReloadFileClicked = { menuType = null; onReloadFileClicked() },
                        onCloseFileClicked = { menuType = null; onCloseFileClicked() },
                    )
                }
            )
            IconButton(
                iconResId = UiR.drawable.ic_pencil,
                onClick = { menuType = MenuType.EDIT },
                contentDescription = stringResource(R.string.editor_menu_edit),
                anchor = {
                    EditMenu(
                        expanded = menuType == MenuType.EDIT,
                        onDismiss = { menuType = null },
                        canUndo = currentDocument?.canUndo ?: false,
                        canRedo = currentDocument?.canRedo ?: false,
                        onUndoClicked = { menuType = null; onUndoClicked() },
                        onRedoClicked = { menuType = null; onRedoClicked() },
                        onCutClicked = { menuType = null; onCutClicked() },
                        onCopyClicked = { menuType = null; onCopyClicked() },
                        onPasteClicked = { menuType = null; onPasteClicked() },
                        onSelectAllClicked = { menuType = null; onSelectAllClicked() },
                        onSelectLineClicked = { menuType = null; onSelectLineClicked() },
                        onDeleteLineClicked = { menuType = null; onDeleteLineClicked() },
                        onDuplicateLineClicked = { menuType = null; onDuplicateLineClicked() },
                        onInsertColorClicked = { menuType = null; onInsertColorClicked() },
                    )
                }
            )
            IconButton(
                iconResId = UiR.drawable.ic_dots_vertical,
                onClick = { menuType = MenuType.OTHER },
                contentDescription = stringResource(R.string.editor_menu_more),
                anchor = {
                    OtherMenu(
                        showGit = currentDocument?.document?.gitRepository != null,
                        expanded = menuType == MenuType.OTHER,
                        onDismiss = { menuType = null },
                        miniToolbar = settings.miniToolbar,
                        symbolBar = settings.extendedKeyboard,
                        wordWrap = settings.wordWrap,
                        readOnly = settings.readOnly,
                        onFindClicked = { menuType = null; onFindClicked() },
                        onGoToLineClicked = { menuType = null; onGoToLineClicked() },
                        onForceSyntaxClicked = { menuType = null; onForceSyntaxClicked() },
                        onStatisticsClicked = { menuType = null; onStatisticsClicked() },
                        onGitClicked = { menuType = MenuType.GIT },
                        onTerminalClicked = {
                            focusManager.clearFocus(force = true)
                            menuType = null
                            onTerminalClicked()
                        },
                        onSettingsClicked = {
                            focusManager.clearFocus(force = true)
                            menuType = null
                            onSettingsClicked()
                        },
                        onMiniToolbarClicked = { menuType = null; onMiniToolbarClicked() },
                        onSymbolBarClicked = { menuType = null; onSymbolBarClicked() },
                        onWordWrapClicked = { menuType = null; onWordWrapClicked() },
                        onReadOnlyClicked = { menuType = null; onReadOnlyClicked() },
                    )
                    GitMenu(
                        expanded = menuType == MenuType.GIT,
                        onDismiss = { menuType = null },
                        onFetchClicked = { menuType = null; onFetchClicked() },
                        onPullClicked = { menuType = null; onPullClicked() },
                        onCommitClicked = { menuType = null; onCommitClicked() },
                        onPushClicked = { menuType = null; onPushClicked() },
                        onCheckoutClicked = { menuType = null; onCheckoutClicked() },
                    )
                }
            )
        },
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun EditorToolbarPreview() {
    PreviewBackground {
        EditorToolbar(currentDocument = null)
    }
}
