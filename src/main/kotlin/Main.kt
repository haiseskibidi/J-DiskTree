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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.state.ScanStatus
import com.jdisktree.state.UiState
import com.jdisktree.viewmodel.ScanViewModel
import java.nio.file.Paths

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 900.dp)
    
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
    var hoveredRect by remember { mutableStateOf<TreeMapRect?>(null) }
    var mousePosition by remember { mutableStateOf(Offset.Zero) }

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
                    TreemapCanvas(
                        rects = uiState.rects(),
                        onHover = { rect, pos -> 
                            hoveredRect = rect
                            mousePosition = pos
                        }
                    )
                    
                    // Tooltip
                    hoveredRect?.let { rect ->
                        if (!rect.isDirectory) {
                            Tooltip(rect, mousePosition)
                        }
                    }
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
                if (state.status() == ScanStatus.SCANNING || state.status() == ScanStatus.CALCULATING_TREEMAP) {
                    Text("Current: ${p.currentPath()}", style = MaterialTheme.typography.caption, maxLines = 1)
                }
            }
            state.errorMessage()?.let { err ->
                Text("Error: $err", color = Color.Red)
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TreemapCanvas(rects: List<TreeMapRect>, onHover: (TreeMapRect?, Offset) -> Unit) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var currentHovered by remember { mutableStateOf<TreeMapRect?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .onPointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.first().position
                val scaleX = canvasSize.width / 1000f
                val scaleY = canvasSize.height / 1000f
                
                val found = rects.find { rect ->
                    !rect.isDirectory &&
                    pos.x >= rect.x() * scaleX &&
                    pos.x <= (rect.x() + rect.width()) * scaleX &&
                    pos.y >= rect.y() * scaleY &&
                    pos.y <= (rect.y() + rect.height()) * scaleY
                }
                currentHovered = found
                onHover(found, pos)
            }
            .onPointerEvent(PointerEventType.Exit) {
                currentHovered = null
                onHover(null, Offset.Zero)
            }
    ) {
        canvasSize = size
        val scaleX = size.width / 1000f
        val scaleY = size.height / 1000f

        rects.forEach { rect ->
            if (!rect.isDirectory) {
                val drawX = rect.x().toFloat() * scaleX
                val drawY = rect.y().toFloat() * scaleY
                val drawW = rect.width().toFloat() * scaleX
                val drawH = rect.height().toFloat() * scaleY

                val isHovered = rect == currentHovered
                val baseColor = getColorForExtension(rect.extension())
                val color = if (isHovered) baseColor.copy(alpha = 0.8f) else baseColor

                drawRect(
                    color = color,
                    topLeft = Offset(drawX, drawY),
                    size = Size(drawW, drawH)
                )
                
                if (isHovered) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, drawH),
                        style = Stroke(width = 2f)
                    )
                } else {
                    drawRect(
                        color = Color(0x33FFFFFF),
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, drawH),
                        style = Stroke(width = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun Tooltip(rect: TreeMapRect, position: Offset) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(position.x.toInt() + 15, position.y.toInt() + 15)
    ) {
        Card(
            backgroundColor = Color(0xFF333333),
            contentColor = Color.White,
            elevation = 4.dp,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val fileName = Paths.get(rect.path()).fileName?.toString() ?: "Unknown"
                Text(fileName, style = MaterialTheme.typography.subtitle2)
                Text("Path: ${rect.path()}", style = MaterialTheme.typography.caption)
                if (rect.extension().isNotEmpty()) {
                    Text("Type: ${rect.extension().uppercase()}", style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}

fun getColorForExtension(ext: String): Color {
    return when (ext.lowercase()) {
        "exe", "dll", "sys", "msi" -> Color(0xFFE57373) // Reddish - System/Binary
        "jpg", "jpeg", "png", "gif", "bmp", "svg" -> Color(0xFF81C784) // Greenish - Images
        "mp4", "mkv", "avi", "mov", "flv" -> Color(0xFF64B5F6) // Blueish - Video
        "mp3", "wav", "flac", "ogg" -> Color(0xFFBA68C8) // Purple - Audio
        "pdf", "doc", "docx", "txt", "rtf", "md" -> Color(0xFFFFB74D) // Orange - Docs
        "zip", "rar", "7z", "tar", "gz" -> Color(0xFFA1887F) // Brown - Archives
        "java", "kt", "py", "cpp", "c", "js", "html", "css" -> Color(0xFF4DB6AC) // Teal - Code
        else -> Color(0xFF90A4AE) // Grey - Others
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
