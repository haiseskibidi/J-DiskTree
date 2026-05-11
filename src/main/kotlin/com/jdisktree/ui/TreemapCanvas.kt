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

import com.jdisktree.domain.FileColorConfig

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TreemapCanvas(
    rects: List<TreeMapRect>,
    index: SpatialGridIndex?,
    selectedPaths: Set<String>,
    highlightedExtension: String?,
    searchQuery: String = "",
    customColors: List<FileColorConfig> = emptyList(),
    baseWidth: Double,
    baseHeight: Double,
    isResizing: Boolean = false,
    onHover: (TreeMapRect?, Offset) -> Unit,
    onClick: (String, Boolean) -> Unit, // path, isCtrl
    onSecondaryClick: (Set<String>, Offset) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentHover by remember { mutableStateOf<TreeMapRect?>(null) }
    var canvasPosition by remember { mutableStateOf(Offset.Zero) }
    
    // Bitmap generation logic
    val treemapBitmap = remember(rects, highlightedExtension, searchQuery, customColors) {
        if (rects.isEmpty()) null
        else {
            val internalW = 1000
            val internalH = 1000
            val bitmap = ImageBitmap(internalW, internalH)
            val canvas = Canvas(bitmap)
            
            val scaleX = internalW.toFloat() / baseWidth.toFloat()
            val scaleY = internalH.toFloat() / baseHeight.toFloat()

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
                    
                    // Apply Dimming (Extension Filter + Search Filter)
                    val matchesExtension = highlightedExtension == null || rect.extension() == highlightedExtension
                    val matchesSearch = searchQuery.isBlank() || rect.path().contains(searchQuery, ignoreCase = true)
                    
                    if (!matchesExtension || !matchesSearch) {
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
                            color = Color.White.copy(alpha = if (highlightedExtension == null && searchQuery.isBlank()) 0.15f else 0.05f)
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
                    currentHover?.let { rect ->
                        if (event.buttons.isSecondaryPressed) {
                            val isAlreadySelected = selectedPaths.contains(rect.path())
                            val finalSelection = if (isAlreadySelected) {
                                selectedPaths
                            } else {
                                // Select only this item if it wasn't part of selection
                                onClick(rect.path(), false)
                                setOf(rect.path())
                            }
                            // Use absolute window coordinates
                            onSecondaryClick(finalSelection, canvasPosition + change.position)
                        } else {
                            onClick(rect.path(), event.keyboardModifiers.isCtrlPressed)
                        }
                    }
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
