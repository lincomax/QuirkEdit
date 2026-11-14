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
import com.blacksquircle.ui.feature.editor.R
import com.blacksquircle.ui.feature.editor.ui.editor.model.DocumentState
import com.blacksquircle.ui.ds.R as UiR

/**
 * Mini toolbar: a flat row of evenly spaced
 * quick actions pinned to the bottom of the editor.
 */
@Composable
internal fun MiniToolbar(
    currentDocument: DocumentState?,
    modifier: Modifier = Modifier,
    onOpenFileClicked: () -> Unit = {},
    onPasteClicked: () -> Unit = {},
    onUndoClicked: () -> Unit = {},
    onRedoClicked: () -> Unit = {},
    onFindClicked: () -> Unit = {},
    onSaveFileClicked: () -> Unit = {},
    onCloseFileClicked: () -> Unit = {},
) {
    val hasDocument = currentDocument != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SquircleTheme.colors.colorBackgroundSecondary)
    ) {
        MiniAction(UiR.drawable.ic_folder_open, stringResource(R.string.editor_menu_file_open), onOpenFileClicked)
        MiniAction(UiR.drawable.ic_paste, stringResource(android.R.string.paste), onPasteClicked, enabled = hasDocument)
        MiniAction(
            iconResId = UiR.drawable.ic_undo,
            description = stringResource(R.string.editor_menu_edit_undo),
            onClick = onUndoClicked,
            enabled = currentDocument?.canUndo ?: false,
            debounce = false,
        )
        MiniAction(
            iconResId = UiR.drawable.ic_redo,
            description = stringResource(R.string.editor_menu_edit_redo),
            onClick = onRedoClicked,
            enabled = currentDocument?.canRedo ?: false,
            debounce = false,
        )
        MiniAction(UiR.drawable.ic_search, stringResource(R.string.editor_menu_search), onFindClicked, enabled = hasDocument)
        MiniAction(UiR.drawable.ic_save, stringResource(R.string.editor_menu_file_save), onSaveFileClicked, enabled = hasDocument)
        MiniAction(UiR.drawable.ic_close, stringResource(R.string.editor_menu_file_close), onCloseFileClicked, enabled = hasDocument)
    }
}

@Composable
private fun MiniAction(
    iconResId: Int,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    debounce: Boolean = true,
) {
    IconButton(
        iconResId = iconResId,
        onClick = onClick,
        contentDescription = description,
        enabled = enabled,
        debounce = debounce,
        iconButtonStyle = IconButtonStyleDefaults.Secondary,
        iconButtonSize = IconButtonSizeDefaults.S,
    )
}

@PreviewLightDark
@Composable
private fun MiniToolbarPreview() {
    PreviewBackground {
        MiniToolbar(currentDocument = null)
    }
}
