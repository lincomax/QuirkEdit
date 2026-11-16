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
package com.blacksquircle.ui.feature.settings.ui.editor.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.blacksquircle.ui.ds.SquircleTheme
import com.blacksquircle.ui.ds.checkbox.CheckBox
import com.blacksquircle.ui.ds.dialog.AlertDialog
import com.blacksquircle.ui.ds.toolbar.MiniToolbarItem
import com.blacksquircle.ui.ds.toolbar.MiniToolbarItems
import com.blacksquircle.ui.feature.settings.R
import com.blacksquircle.ui.ds.R as UiR

private val RowHeight = 48.dp

/** Pick and reorder the bottom toolbar items: drag the handle, tick the box */
@Composable
internal fun MiniToolbarDialog(
    items: List<MiniToolbarItem>,
    onConfirm: (List<MiniToolbarItem>) -> Unit,
    onDismiss: () -> Unit,
) {
    val list = remember { items.toMutableStateList() }
    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }
    var dragging by remember { mutableStateOf<MiniToolbarItem?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        title = stringResource(R.string.settings_mini_toolbar_title),
        content = {
            Column(Modifier.fillMaxWidth()) {
                list.forEach { item ->
                    key(item.action) {
                        val isDragged = dragging?.action == item.action
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(RowHeight)
                                .padding(end = 8.dp)
                                .zIndex(if (isDragged) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragged) dragOffset else 0f
                                    alpha = if (isDragged) 0.85f else 1f
                                }
                        ) {
                            Icon(
                                painter = painterResource(UiR.drawable.ic_drag),
                                contentDescription = null,
                                tint = SquircleTheme.colors.colorTextAndIconSecondary,
                                modifier = Modifier
                                    .size(RowHeight)
                                    .padding(12.dp)
                                    .pointerInput(item.action) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragging = list.first { it.action == item.action }
                                                dragOffset = 0f
                                            },
                                            onDragEnd = { dragging = null; dragOffset = 0f },
                                            onDragCancel = { dragging = null; dragOffset = 0f },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount.y
                                                val from = list.indexOfFirst { it.action == item.action }
                                                if (dragOffset > rowHeightPx / 2 && from < list.lastIndex) {
                                                    list.add(from + 1, list.removeAt(from))
                                                    dragOffset -= rowHeightPx
                                                } else if (dragOffset < -rowHeightPx / 2 && from > 0) {
                                                    list.add(from - 1, list.removeAt(from))
                                                    dragOffset += rowHeightPx
                                                }
                                            },
                                        )
                                    }
                            )
                            Icon(
                                painter = painterResource(item.action.iconRes),
                                contentDescription = null,
                                tint = SquircleTheme.colors.colorTextAndIconSecondary,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = stringResource(item.action.titleRes),
                                style = SquircleTheme.typography.text14Regular,
                                color = SquircleTheme.colors.colorTextAndIconPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.weight(1f))
                            CheckBox(
                                checked = item.enabled,
                                onClick = {
                                    val index = list.indexOfFirst { it.action == item.action }
                                    list[index] = list[index].copy(enabled = !list[index].enabled)
                                },
                            )
                        }
                    }
                }
            }
        },
        horizontalPadding = false,
        confirmButton = stringResource(UiR.string.common_save),
        onConfirmClicked = { onConfirm(list.toList()) },
        dismissButton = stringResource(R.string.settings_mini_toolbar_reset),
        onDismissClicked = { onConfirm(MiniToolbarItems.DEFAULT) },
        onDismiss = onDismiss,
        properties = DialogProperties(),
    )
}
