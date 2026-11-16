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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.blacksquircle.ui.ds.PreviewBackground
import com.blacksquircle.ui.ds.SquircleTheme
import com.blacksquircle.ui.ds.button.IconButton
import com.blacksquircle.ui.ds.button.IconButtonSizeDefaults
import com.blacksquircle.ui.ds.button.IconButtonStyleDefaults
import com.blacksquircle.ui.ds.toolbar.MiniToolbarAction
import com.blacksquircle.ui.ds.toolbar.MiniToolbarItems
import com.blacksquircle.ui.feature.editor.ui.editor.model.DocumentState

/**
 * Mini toolbar: a flat row of evenly spaced quick actions
 * pinned to the bottom of the editor, chosen and ordered in settings.
 */
@Composable
internal fun MiniToolbar(
    currentDocument: DocumentState?,
    modifier: Modifier = Modifier,
    items: List<MiniToolbarAction> = MiniToolbarItems.DEFAULT.filter { it.enabled }.map { it.action },
    onOpenFileClicked: () -> Unit = {},
    onNewFileClicked: () -> Unit = {},
    onPasteClicked: () -> Unit = {},
    onCopyClicked: () -> Unit = {},
    onCutClicked: () -> Unit = {},
    onSelectAllClicked: () -> Unit = {},
    onUndoClicked: () -> Unit = {},
    onRedoClicked: () -> Unit = {},
    onFindClicked: () -> Unit = {},
    onGoToLineClicked: () -> Unit = {},
    onSaveFileClicked: () -> Unit = {},
    onSaveFileAsClicked: () -> Unit = {},
    onCloseFileClicked: () -> Unit = {},
) {
    val hasDocument = currentDocument != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(SquircleTheme.colors.colorBackgroundSecondary)
    ) {
        items.forEach { action ->
            val (onClick, enabled) = when (action) {
                MiniToolbarAction.OPEN -> onOpenFileClicked to true
                MiniToolbarAction.NEW -> onNewFileClicked to true
                MiniToolbarAction.PASTE -> onPasteClicked to hasDocument
                MiniToolbarAction.COPY -> onCopyClicked to hasDocument
                MiniToolbarAction.CUT -> onCutClicked to hasDocument
                MiniToolbarAction.SELECT_ALL -> onSelectAllClicked to hasDocument
                MiniToolbarAction.UNDO -> onUndoClicked to (currentDocument?.canUndo ?: false)
                MiniToolbarAction.REDO -> onRedoClicked to (currentDocument?.canRedo ?: false)
                MiniToolbarAction.SEARCH -> onFindClicked to hasDocument
                MiniToolbarAction.GO_TO_LINE -> onGoToLineClicked to hasDocument
                MiniToolbarAction.SAVE -> onSaveFileClicked to hasDocument
                MiniToolbarAction.SAVE_AS -> onSaveFileAsClicked to hasDocument
                MiniToolbarAction.CLOSE -> onCloseFileClicked to hasDocument
            }
            IconButton(
                iconResId = action.iconRes,
                onClick = onClick,
                contentDescription = stringResource(action.titleRes),
                enabled = enabled,
                debounce = action != MiniToolbarAction.UNDO && action != MiniToolbarAction.REDO,
                iconButtonStyle = IconButtonStyleDefaults.Accessory,
                iconButtonSize = IconButtonSizeDefaults.S,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MiniToolbarPreview() {
    PreviewBackground {
        MiniToolbar(currentDocument = null)
    }
}
