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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import com.jdisktree.ui.DirectoryPicker
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
                OutlinedButton(
                    onClick = { 
                        DirectoryPicker.pickDirectory()?.let { pathText = it }
                    },
                    enabled = uiState.status() != ScanStatus.SCANNING && uiState.status() != ScanStatus.CALCULATING_TREEMAP
                ) {
                    Text("Browse")
                }
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
                        index = uiState.index(),
                        onHover = { rect, pos -> 
                            hoveredRect = rect
                            mousePosition = pos
                        }
                    )
                    
                    // Tooltip
                    hoveredRect?.let { rect ->
                        Tooltip(rect, mousePosition)
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
fun TreemapCanvas(
    rects: List<TreeMapRect>,
    index: com.jdisktree.treemap.index.SpatialGridIndex?,
    onHover: (TreeMapRect?, Offset) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var currentHovered by remember { mutableStateOf<TreeMapRect?>(null) }

    // Cache the static treemap background in a bitmap
    val treemapBitmap = remember(rects, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || rects.isEmpty()) null
        else {
            val bitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
            val canvas = Canvas(bitmap)
            val scaleX = canvasSize.width / 1000f
            val scaleY = canvasSize.height / 1000f

            rects.forEach { rect ->
                if (!rect.isDirectory) {
                    val drawX = rect.x().toFloat() * scaleX
                    val drawY = rect.y().toFloat() * scaleY
                    val drawW = rect.width().toFloat() * scaleX
                    val drawH = rect.height().toFloat() * scaleY

                    if (drawW >= 0.5f && drawH >= 0.5f) {
                        val paint = Paint().apply {
                            color = getColorForExtension(rect.extension())
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), paint)
                        
                        val strokePaint = Paint().apply {
                            color = Color(0x33FFFFFF)
                            style = PaintingStyle.Stroke
                            strokeWidth = 0.5f
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), strokePaint)
                    }
                }
            }
            bitmap
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .onPointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.first().position
                val scaleX = canvasSize.width / 1000f
                val scaleY = canvasSize.height / 1000f
                
                val indexX = pos.x / scaleX
                val indexY = pos.y / scaleY
                
                val candidates = index?.query(indexX.toDouble(), indexY.toDouble()) ?: rects
                
                val found = candidates.findLast { rect ->
                    indexX >= rect.x() &&
                    indexX <= (rect.x() + rect.width()) &&
                    indexY >= rect.y() &&
                    indexY <= (rect.y() + rect.height())
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
        
        // Draw the cached bitmap
        treemapBitmap?.let {
            drawImage(it)
        }

        // Draw ONLY the highlight box dynamically
        currentHovered?.let { rect ->
            val scaleX = size.width / 1000f
            val scaleY = size.height / 1000f
            val drawX = rect.x().toFloat() * scaleX
            val drawY = rect.y().toFloat() * scaleY
            val drawW = rect.width().toFloat() * scaleX
            val drawH = rect.height().toFloat() * scaleY

            drawRect(
                color = Color.White,
                topLeft = Offset(drawX, drawY),
                size = Size(drawW, drawH),
                style = Stroke(width = 2f)
            )
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
                Text("Size: ${formatSize(rect.size())}", style = MaterialTheme.typography.body2, color = Color(0xFF81C784))
                Text("Path: ${rect.path()}", style = MaterialTheme.typography.caption)
                if (rect.isDirectory) {
                    Text("Type: DIRECTORY", style = MaterialTheme.typography.caption)
                } else if (rect.extension().isNotEmpty()) {
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
