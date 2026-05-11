package com.jdisktree.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*

@Composable
fun Toolbar(
    uiState: UiState,
    pathText: String,
    onPathChange: (String) -> Unit,
    viewModel: ScanViewModel,
    scanExclusions: List<ScanExclusion>,
    searchFocusRequester: FocusRequester = remember { FocusRequester() }
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
        
        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

        // Search Field
        OutlinedTextField(
            value = uiState.searchQuery(),
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .width(250.dp)
                .focusRequester(searchFocusRequester),
            placeholder = { Text(stringResource("search_placeholder"), style = MaterialTheme.typography.body2) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.body2,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

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
                viewModel.startScan(Paths.get(pathText), 1000.0, 1000.0, scanExclusions)
            },
            enabled = uiState.status() != ScanStatus.SCANNING && uiState.status() != ScanStatus.CALCULATING_TREEMAP
        ) {
            Text(stringResource("scan"))
        }
    }
}
