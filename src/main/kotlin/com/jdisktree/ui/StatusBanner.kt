package com.jdisktree.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState

@Composable
fun StatusBanner(state: UiState) {
    val statusText = when (state.status()) {
        ScanStatus.IDLE -> ""
        ScanStatus.SCANNING -> stringResource("status_scanning")
        ScanStatus.CALCULATING_TREEMAP -> stringResource("status_calculating")
        ScanStatus.COMPLETED -> stringResource("status_completed")
        ScanStatus.ERROR -> stringResource("status_error")
    }

    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Status: $statusText", style = MaterialTheme.typography.subtitle1)
            state.progress()?.let { p ->
                Text("${stringResource("files_count", p.filesScanned())} | ${stringResource("scanned_size", formatSize(p.bytesScanned()))}")
                if (state.status() == ScanStatus.SCANNING || state.status() == ScanStatus.CALCULATING_TREEMAP) {
                    Text(stringResource("current_path", p.currentPath()), style = MaterialTheme.typography.caption, maxLines = 1)
                }
            }
            state.errorMessage()?.let { err ->
                Text("${stringResource("status_error")}: $err", color = Color.Red)
            }
        }
    }
}
