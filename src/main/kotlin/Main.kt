import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState
import com.jdisktree.viewmodel.ScanViewModel
import java.nio.file.Paths

fun main() = application {
    val windowState = rememberWindowState(width = 1024.dp, height = 768.dp)
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "J-DiskTree - Disk Space Analyzer"
    ) {
        App()
    }
}

@Composable
fun App() {
    var uiState by remember { mutableStateOf(UiState.idle()) }
    val viewModel = remember { ScanViewModel { newState -> uiState = newState } }
    var pathText by remember { mutableStateOf("C:\\") }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Toolbar
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = pathText,
                    onValueChange = { pathText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Directory Path") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { 
                        viewModel.startScan(Paths.get(pathText), 1000.0, 1000.0) 
                    },
                    enabled = uiState.status() != ScanStatus.SCANNING && uiState.status() != ScanStatus.CALCULATING_TREEMAP
                ) {
                    Text("Scan")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status & Progress
            StatusBanner(uiState)

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content: Treemap or Progress
            Box(modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, Color.Gray)) {
                if (uiState.rects().isNotEmpty()) {
                    TreemapCanvas(uiState.rects())
                } else if (uiState.status() == ScanStatus.SCANNING || uiState.status() == ScanStatus.CALCULATING_TREEMAP) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.status() == ScanStatus.IDLE) {
                    Text("Select a directory and press 'Scan'", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun StatusBanner(state: UiState) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Status: ${state.status()}", style = MaterialTheme.typography.subtitle1)
            state.progress()?.let { p ->
                Text("Files: ${p.filesScanned()} | Scanned: ${formatSize(p.bytesScanned())}")
                Text("Current: ${p.currentPath()}", style = MaterialTheme.typography.caption, maxLines = 1)
            }
            state.errorMessage()?.let { err ->
                Text("Error: $err", color = Color.Red)
            }
        }
    }
}

@Composable
fun TreemapCanvas(rects: List<TreeMapRect>) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Note: Coordinates in rects are calculated for 1000x1000 grid
        val scaleX = canvasWidth / 1000f
        val scaleY = canvasHeight / 1000f

        rects.forEach { rect ->
            // Draw only leaf nodes or files for clarity in this first pass
            if (!rect.isDirectory) {
                val drawX = rect.x().toFloat() * scaleX
                val drawY = rect.y().toFloat() * scaleY
                val drawW = rect.width().toFloat() * scaleX
                val drawH = rect.height().toFloat() * scaleY

                drawRect(
                    color = Color(0xFF42A5F5), // Material Blue 400
                    topLeft = Offset(drawX, drawY),
                    size = Size(drawW, drawH)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(drawX, drawY),
                    size = Size(drawW, drawH),
                    style = Stroke(width = 1f)
                )
            }
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
