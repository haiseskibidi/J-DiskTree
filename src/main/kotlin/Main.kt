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

            // 1. Draw all files (monolithic data layer)
            rects.forEach { rect ->
                if (!rect.isDirectory) {
                    val drawX = rect.x().toFloat() * scaleX
                    val drawY = rect.y().toFloat() * scaleY
                    val drawW = rect.width().toFloat() * scaleX
                    val drawH = rect.height().toFloat() * scaleY

                    // Ensure minimum visible size
                    val finalW = if (drawW < 1.0f) 1.0f else drawW
                    val finalH = if (drawH < 1.0f) 1.0f else drawH

                    val baseColor = getColorForExtension(rect.extension())
                    
                    // Adaptive visual style based on size
                    val isTiny = finalW < 3f || finalH < 3f
                    
                    if (isTiny) {
                        // Draw only solid color for tiny items to avoid border-clutter
                        val paint = Paint().apply { color = baseColor }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + finalW, drawY + finalH), paint)
                    } else {
                        // Main body with subtle vertical gradient for larger items
                        val paint = Paint().apply {
                            shader = LinearGradientShader(
                                from = Offset(drawX, drawY),
                                to = Offset(drawX, drawY + finalH),
                                colors = listOf(
                                    baseColor.copy(alpha = 1f),
                                    baseColor.copy(red = baseColor.red * 0.85f, green = baseColor.green * 0.85f, blue = baseColor.blue * 0.85f)
                                )
                            )
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + finalW, drawY + finalH), paint)
                        
                        // 3D Bevel and Border (only for visible items)
                        val lightPaint = Paint().apply {
                            color = Color.White.copy(alpha = 0.2f)
                            strokeWidth = 1f
                            style = PaintingStyle.Stroke
                        }
                        canvas.drawLine(Offset(drawX, drawY), Offset(drawX + finalW, drawY), lightPaint)
                        canvas.drawLine(Offset(drawX, drawY), Offset(drawX, drawY + finalH), lightPaint)

                        val darkPaint = Paint().apply {
                            color = Color.Black.copy(alpha = 0.3f)
                            strokeWidth = 1f
                            style = PaintingStyle.Stroke
                        }
                        canvas.drawLine(Offset(drawX, drawY + finalH), Offset(drawX + finalW, drawY + finalH), darkPaint)
                        canvas.drawLine(Offset(drawX + finalW, drawY), Offset(drawX + finalW, drawY + finalH), darkPaint)

                        val borderPaint = Paint().apply {
                            color = Color.Black.copy(alpha = 0.2f)
                            strokeWidth = 0.5f
                            style = PaintingStyle.Stroke
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + finalW, drawY + finalH), borderPaint)
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

            // Glowing overlay
            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(drawX, drawY),
                size = Size(drawW, drawH)
            )
            
            // Thick white border
            drawRect(
                color = Color.White,
                topLeft = Offset(drawX, drawY),
                size = Size(drawW, drawH),
                style = Stroke(width = 3f)
            )
        }
    }
}

@Composable
fun Tooltip(rect: TreeMapRect, position: Offset) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = if (position.x > 800) (position.x - 320).toInt() else (position.x + 15).toInt(),
            y = if (position.y > 600) (position.y - 120).toInt() else (position.y + 15).toInt()
        )
    ) {
        Card(
            backgroundColor = Color(0xFF333333),
            contentColor = Color.White,
            elevation = 8.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            modifier = Modifier.widthIn(max = 300.dp).border(1.dp, Color(0xFF555555), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val fileName = Paths.get(rect.path()).fileName?.toString() ?: "Unknown"
                Text(fileName, style = MaterialTheme.typography.subtitle2, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Size: ${formatSize(rect.size())}", style = MaterialTheme.typography.body2, color = Color(0xFF81C784), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(rect.path(), style = MaterialTheme.typography.caption, color = Color.LightGray, maxLines = 3)
                if (rect.isDirectory) {
                    Text("Type: DIRECTORY", style = MaterialTheme.typography.caption, color = Color(0xFF42A5F5))
                } else if (rect.extension().isNotEmpty()) {
                    Text("Type: ${rect.extension().uppercase()}", style = MaterialTheme.typography.caption, color = Color(0xFFFFA726))
                }
            }
        }
    }
}

fun getColorForExtension(ext: String): Color {
    return when (ext.lowercase()) {
        "exe", "dll", "sys", "msi", "com" -> Color(0xFFEF5350) // Soft Red - System
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp" -> Color(0xFF66BB6A) // Soft Green - Images
        "mp4", "mkv", "avi", "mov", "flv", "webm" -> Color(0xFF42A5F5) // Soft Blue - Video
        "mp3", "wav", "flac", "ogg", "m4a" -> Color(0xFFAB47BC) // Soft Purple - Audio
        "pdf", "doc", "docx", "txt", "rtf", "md", "odt", "xls", "xlsx" -> Color(0xFFFFA726) // Soft Orange - Docs
        "zip", "rar", "7z", "tar", "gz", "bz2" -> Color(0xFF8D6E63) // Soft Brown - Archives
        "java", "kt", "py", "cpp", "c", "js", "html", "css", "ts", "json", "xml" -> Color(0xFF26A69A) // Teal - Code
        else -> Color(0xFF78909C) // Blue Grey - Others
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
