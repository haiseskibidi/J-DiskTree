package com.jdisktree.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState

@Composable
fun StatusBanner(state: UiState) {
    val statusText = when (state.status()) {
        ScanStatus.IDLE, null -> ""
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
            
            state.diskSpaceInfo()?.let { diskInfo ->
                if (state.status() == ScanStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val usedSpace = diskInfo.totalSpace() - diskInfo.usableSpace()
                        val fillPercentage = if (diskInfo.totalSpace() > 0) usedSpace.toFloat() / diskInfo.totalSpace().toFloat() else 0f
                        
                        Text(
                            text = stringResource("drive_name", diskInfo.driveName()),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        
                        LinearProgressIndicator(
                            progress = fillPercentage,
                            modifier = Modifier.width(100.dp).height(8.dp).padding(horizontal = 8.dp),
                            color = if (fillPercentage > 0.9f) Color.Red else MaterialTheme.colors.primary,
                            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                        )
                        
                        Text(
                            text = stringResource("disk_free_of_total", formatSize(diskInfo.usableSpace()), formatSize(diskInfo.totalSpace())),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        
                        state.rootNode()?.let { root ->
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource("current_space", formatSize(root.size())),
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
