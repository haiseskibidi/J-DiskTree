package com.jdisktree.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.treemap.index.SpatialGridIndex
import com.jdisktree.domain.DiffNode
import com.jdisktree.domain.DiffStatus

import com.jdisktree.domain.FileColorConfig

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TreemapCanvas(
    rects: List<TreeMapRect>,
    index: SpatialGridIndex?,
    selectedPaths: Set<String>,
    highlightedExtension: String?,
    searchQuery: String = "",
    ageFilterDays: Int = 0,
    diffNode: DiffNode? = null,
    customColors: List<FileColorConfig> = emptyList(),
    baseWidth: Double,
    baseHeight: Double,
    isResizing: Boolean = false,
    onHover: (TreeMapRect?, Offset) -> Unit,
    onClick: (String?, Boolean) -> Unit, // path, isCtrl
    onSecondaryClick: (Set<String>, Offset) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentHover by remember { mutableStateOf<TreeMapRect?>(null) }
    var canvasPosition by remember { mutableStateOf(Offset.Zero) }
    
    // Background Bitmap generation logic with debouncing
    val treemapBitmap by produceState<ImageBitmap?>(initialValue = null, rects, highlightedExtension, searchQuery, ageFilterDays, diffNode, customColors) {
        if (rects.isEmpty()) {
            value = null
            return@produceState
        }

        // Debounce search/filter input to keep UI responsive
        if (searchQuery.isNotBlank() || ageFilterDays > 0) {
            kotlinx.coroutines.delay(60)
        }

        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val internalW = 1000
            val internalH = 1000
            val bitmap = ImageBitmap(internalW, internalH)
            val canvas = Canvas(bitmap)
            
            val scaleX = internalW.toFloat() / baseWidth.toFloat()
            val scaleY = internalH.toFloat() / baseHeight.toFloat()
            val now = System.currentTimeMillis()

            // Pre-calculate diff mapping for fast lookup during render
            val diffMap = mutableMapOf<String, DiffStatus>()
            diffNode?.let { buildDiffMap(it, diffMap) }

            val dirPaint = Paint().apply { color = Color(0xFF242424) }
            rects.forEach { rect ->
                if (rect.isDirectory) {
                    val drawX = rect.x().toFloat() * scaleX
                    val drawY = rect.y().toFloat() * scaleY
                    val drawW = rect.width().toFloat() * scaleX
                    val drawH = rect.height().toFloat() * scaleY
                    canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), dirPaint)
                }
            }

            rects.forEach { rect ->
                if (!rect.isDirectory) {
                    val drawX = rect.x().toFloat() * scaleX
                    val drawY = rect.y().toFloat() * scaleY
                    val drawW = rect.width().toFloat() * scaleX
                    val drawH = rect.height().toFloat() * scaleY

                    var baseColor = getColorForExtension(rect.extension(), customColors)
                    
                    val status = diffMap[rect.path()]
                    if (diffNode != null) {
                        baseColor = when (status) {
                            DiffStatus.ADDED -> Color(0xFF2ECC71) // Material Emerald
                            DiffStatus.MODIFIED -> Color(0xFFF1C40F) // Material Sunflower
                            else -> baseColor.copy(alpha = 0.1f) // Dim unchanged in diff mode
                        }
                    }

                    // Apply Dimming (Extension Filter + Search Filter + Age Filter)
                    val matchesExtension = highlightedExtension == null || rect.extension() == highlightedExtension
                    val matchesSearch = searchQuery.isBlank() || rect.path().contains(searchQuery, ignoreCase = true)
                    val matchesAge = ageFilterDays == 0 || 
                        (now - rect.lastModified) > (ageFilterDays.toLong() * 24 * 60 * 60 * 1000L)
                    
                    if (diffNode == null && (!matchesExtension || !matchesSearch || !matchesAge)) {
                        baseColor = baseColor.copy(alpha = 0.15f)
                    }

                    if (drawW < 3f || drawH < 3f) {
                        val paint = Paint().apply { color = baseColor }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), paint)
                    } else {
                        val paint = Paint().apply {
                            shader = LinearGradientShader(
                                from = Offset(drawX, drawY),
                                to = Offset(drawX, drawY + drawH),
                                colors = listOf(baseColor, baseColor.copy(red = baseColor.red * 0.85f, green = baseColor.green * 0.85f, blue = baseColor.blue * 0.85f))
                            )
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), paint)
                        
                        val lightPaint = Paint().apply {
                            color = Color.White.copy(alpha = if (highlightedExtension == null && searchQuery.isBlank() && diffNode == null) 0.15f else 0.05f)
                            strokeWidth = 1f
                            style = PaintingStyle.Stroke
                        }
                        canvas.drawLine(Offset(drawX, drawY), Offset(drawX + drawW, drawY), lightPaint)
                        canvas.drawLine(Offset(drawX, drawY), Offset(drawX, drawY + drawH), lightPaint)
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
            .onGloballyPositioned { canvasPosition = it.positionInWindow() }
            .onSizeChanged { canvasSize = it }
            .onPointerEvent(PointerEventType.Move) { event ->
                if (isResizing) return@onPointerEvent 
                val pos = event.changes.first().position
                val scaleX = canvasSize.width.toFloat() / baseWidth.toFloat()
                val scaleY = canvasSize.height.toFloat() / baseHeight.toFloat()
                
                val indexX = (pos.x / scaleX).toDouble()
                val indexY = (pos.y / scaleY).toDouble()
                
                val found = index?.query(indexX, indexY)?.findLast { rect ->
                    indexX >= rect.x() && indexX <= (rect.x() + rect.width()) &&
                    indexY >= rect.y() && indexY <= (rect.y() + rect.height())
                }
                currentHover = found
                onHover(found, pos)
            }
            .onPointerEvent(PointerEventType.Exit) {
                currentHover = null
                onHover(null, Offset.Zero)
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                val change = event.changes.first()
                if (change.pressed) {
                    if (currentHover == null) {
                        onClick(null, false)
                    } else {
                        currentHover?.let { rect ->
                            if (event.buttons.isSecondaryPressed) {
                                val isAlreadySelected = selectedPaths.contains(rect.path())
                                val finalSelection = if (isAlreadySelected) {
                                    selectedPaths
                                } else {
                                    onClick(rect.path(), false)
                                    setOf(rect.path())
                                }
                                onSecondaryClick(finalSelection, canvasPosition + change.position)
                            } else {
                                onClick(rect.path(), event.keyboardModifiers.isCtrlPressed)
                            }
                        }
                    }
                    change.consume()
                }
            }
    ) {
        // DRAW BITMAP (GPU SCALED)
        treemapBitmap?.let {
            drawImage(
                image = it,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = if (isResizing) FilterQuality.Low else FilterQuality.Medium
            )
        }

        if (isResizing) return@Canvas

        val scaleX = size.width / baseWidth.toFloat()
        val scaleY = size.height / baseHeight.toFloat()

        currentHover?.let { rect ->
            val drawX = rect.x().toFloat() * scaleX
            val drawY = rect.y().toFloat() * scaleY
            val drawW = rect.width().toFloat() * scaleX
            val drawH = rect.height().toFloat() * scaleY
            drawRect(Color.White.copy(alpha = 0.2f), Offset(drawX, drawY), Size(drawW, drawH))
            drawRect(Color.White, Offset(drawX, drawY), Size(drawW, drawH), style = Stroke(width = 3f))
        }

        // 2. Draw Selection: Thick solid white border (persistent)
        selectedPaths.forEach { path ->
            rects.find { it.path() == path }?.let { rect ->
                val drawX = rect.x().toFloat() * scaleX
                val drawY = rect.y().toFloat() * scaleY
                val drawW = rect.width().toFloat() * scaleX
                val drawH = rect.height().toFloat() * scaleY
                drawRect(Color.White, Offset(drawX, drawY), Size(drawW, drawH), style = Stroke(width = 3f))
            }
        }
    }
}

private fun buildDiffMap(node: DiffNode, map: MutableMap<String, DiffStatus>) {
    if (node.status != DiffStatus.UNCHANGED) {
        map[node.absolutePath] = node.status
    }
    node.children.forEach { buildDiffMap(it, map) }
}
