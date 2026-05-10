package com.jdisktree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState
import com.jdisktree.viewmodel.ScanViewModel
import java.nio.file.Paths

@Composable
fun Toolbar(
    uiState: UiState,
    pathText: String,
    onPathChange: (String) -> Unit,
    viewModel: ScanViewModel
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = pathText,
            onValueChange = onPathChange,
            modifier = Modifier.weight(1f),
            label = { Text(stringResource("prop_path")) },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = { 
                DirectoryPicker.pickDirectory()?.let { onPathChange(it) }
            },
            enabled = uiState.status() != ScanStatus.SCANNING && uiState.status() != ScanStatus.CALCULATING_TREEMAP
        ) {
            Text(stringResource("browse"))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { 
                viewModel.startScan(Paths.get(pathText), 1000.0, 1000.0) 
            },
            enabled = uiState.status() != ScanStatus.SCANNING && uiState.status() != ScanStatus.CALCULATING_TREEMAP
        ) {
            Text(stringResource("scan"))
        }
    }
}
