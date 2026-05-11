package com.jdisktree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState
import com.jdisktree.viewmodel.ScanViewModel
import com.jdisktree.domain.ScanExclusion
import java.nio.file.Paths

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset

@Composable
fun Toolbar(
    uiState: UiState,
    pathText: String,
    onPathChange: (String) -> Unit,
    viewModel: ScanViewModel,
    scanExclusions: List<ScanExclusion>,
    searchFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    val strings = LocalStrings.current
    var ageMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        TextField(
            value = pathText,
            onValueChange = onPathChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource("prop_path")) },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

        // Search Field with Integrated Age Filter
        Box(modifier = Modifier.width(300.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery(),
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester),
                placeholder = { Text(stringResource("search_placeholder"), style = MaterialTheme.typography.body2) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall)) },
                trailingIcon = {
                    IconButton(
                        onClick = { ageMenuExpanded = true },
                        modifier = Modifier
                            .size(32.dp)
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Age Filter",
                            modifier = Modifier.size(Dimens.IconMedium),
                            tint = if (uiState.ageFilterDays() > 0) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.body2,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                )
            )

            if (ageMenuExpanded) {
                Popup(
                    onDismissRequest = { ageMenuExpanded = false },
                    offset = IntOffset(0, 64),
                    focusable = true
                ) {
                    Card(
                        elevation = 8.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.width(260.dp) // More compact width
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = strings.get("age_filter").split(":")[0],
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val currentDaysTotal = uiState.ageFilterDays()
                            val years = currentDaysTotal / 365
                            val months = (currentDaysTotal % 365) / 30
                            val days = (currentDaysTotal % 365) % 30

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                @Composable
                                fun AgeInput(label: String, value: Int, onValueChange: (Int) -> Unit) {
                                    val focusRequester = remember { FocusRequester() }
                                    var isFocused by remember { mutableStateOf(false) }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.overline, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp)
                                                .border(
                                                    1.dp, 
                                                    if (isFocused) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.2f), 
                                                    MaterialTheme.shapes.small
                                                )
                                                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = { focusRequester.requestFocus() }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Hide placeholder '0' when focused or has value
                                            if (value == 0 && !isFocused) {
                                                Text(
                                                    "0", 
                                                    style = MaterialTheme.typography.body2, 
                                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                                    textAlign = TextAlign.Center
                                                )
                                            }

                                            BasicTextField(
                                                value = if (value > 0) value.toString() else "",
                                                onValueChange = { 
                                                    val filtered = it.filter { c -> c.isDigit() }
                                                    onValueChange(filtered.toIntOrNull() ?: 0) 
                                                },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.body2.copy(
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colors.onSurface
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                                modifier = Modifier
                                                    .focusRequester(focusRequester)
                                                    .onFocusChanged { isFocused = it.isFocused }
                                                    .width(IntrinsicSize.Min)
                                                    .defaultMinSize(minWidth = 2.dp)
                                            )
                                        }
                                    }
                                }

                                AgeInput(strings.get("age_days"), days) { viewModel.setAgeFilter(years * 365 + months * 30 + it) }
                                AgeInput(strings.get("age_months"), months) { viewModel.setAgeFilter(years * 365 + it * 30 + days) }
                                AgeInput(strings.get("age_years"), years) { viewModel.setAgeFilter(it * 365 + months * 30 + days) }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.setAgeFilter(0); ageMenuExpanded = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(strings.get("age_any"), style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
                                }
                                
                                Button(
                                    onClick = { ageMenuExpanded = false },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Text("OK", style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

        OutlinedButton(
            onClick = {
                DirectoryPicker.pickDirectory()?.let { onPathChange(it) }
            },
            modifier = Modifier.height(48.dp),
            enabled = uiState.status() != ScanStatus.SCANNING && uiState.status() != ScanStatus.CALCULATING_TREEMAP
        ) {
            Text(stringResource("browse"))
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Button(
            onClick = {
                viewModel.startScan(Paths.get(pathText), 1000.0, 1000.0, scanExclusions)
            },
            modifier = Modifier.height(48.dp),
            enabled = uiState.status() != ScanStatus.SCANNING && uiState.status() != ScanStatus.CALCULATING_TREEMAP
        ) {
            Text(stringResource("scan"))
        }
    }
}
