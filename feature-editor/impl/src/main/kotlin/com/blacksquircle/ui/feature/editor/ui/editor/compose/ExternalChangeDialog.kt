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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.blacksquircle.ui.ds.SquircleTheme
import com.blacksquircle.ui.ds.dialog.AlertDialog
import com.blacksquircle.ui.feature.editor.R
import com.blacksquircle.ui.feature.editor.domain.model.DocumentModel

/** Asks whether to reload a file another app changed */
@Composable
internal fun ExternalChangeDialog(
    document: DocumentModel,
    hasUnsavedChanges: Boolean,
    onReloadClicked: () -> Unit,
    onKeepClicked: () -> Unit,
) {
    AlertDialog(
        title = stringResource(R.string.editor_external_change_title),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.editor_external_change_message, document.displayName),
                    color = SquircleTheme.colors.colorTextAndIconSecondary,
                    style = SquircleTheme.typography.text16Regular,
                )
                if (hasUnsavedChanges) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.editor_external_change_unsaved),
                        color = SquircleTheme.colors.colorTextAndIconSecondary,
                        style = SquircleTheme.typography.text16Regular,
                    )
                }
            }
        },
        onDismiss = onKeepClicked,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        confirmButton = stringResource(R.string.editor_external_change_reload),
        dismissButton = stringResource(R.string.editor_external_change_keep),
        onConfirmClicked = onReloadClicked,
        onDismissClicked = onKeepClicked,
    )
}
