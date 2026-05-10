package com.jdisktree.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.treemap.index.SpatialGridIndex

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TreemapCanvas(
    rects: List<TreeMapRect>,
    index: SpatialGridIndex?,
    selectedPath: String?,
    highlightedExtension: String?,
    baseWidth: Double,
    baseHeight: Double,
    onHover: (TreeMapRect?, Offset) -> Unit,
    onClick: (String) -> Unit,
    onSecondaryClick: (String, Offset) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var currentHovered by remember { mutableStateOf<TreeMapRect?>(null) }

    val treemapBitmap = remember(rects, canvasSize, baseWidth, baseHeight, highlightedExtension) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || rects.isEmpty()) null
        else {
            val bitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
            val canvas = Canvas(bitmap)
            val scaleX = canvasSize.width / baseWidth.toFloat()
            val scaleY = canvasSize.height / baseHeight.toFloat()

            // 1. Draw all directory backgrounds
            rects.forEach { rect ->
                if (rect.isDirectory) {
                    val drawX = rect.x().toFloat() * scaleX
                    val drawY = rect.y().toFloat() * scaleY
                    val drawW = rect.width().toFloat() * scaleX
                    val drawH = rect.height().toFloat() * scaleY

                    val paint = Paint().apply { color = Color(0xFF242424) }
                    canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), paint)
                }
            }

            // 2. Draw all files on top
            rects.forEach { rect ->
                if (!rect.isDirectory) {
                    val drawX = rect.x().toFloat() * scaleX
                    val drawY = rect.y().toFloat() * scaleY
                    val drawW = rect.width().toFloat() * scaleX
                    val drawH = rect.height().toFloat() * scaleY

                    var baseColor = getColorForExtension(rect.extension())
                    
                    // Dim files that don't match the highlighted extension
                    if (highlightedExtension != null && rect.extension() != highlightedExtension) {
                        baseColor = baseColor.copy(alpha = 0.2f)
                    }

                    val isTiny = drawW < 5f || drawH < 5f
                    
                    if (isTiny) {
                        val paint = Paint().apply { color = baseColor }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), paint)
                    } else {
                        val paint = Paint().apply {
                            shader = LinearGradientShader(
                                from = Offset(drawX, drawY),
                                to = Offset(drawX, drawY + drawH),
                                colors = listOf(
                                    baseColor,
                                    baseColor.copy(red = baseColor.red * 0.85f, green = baseColor.green * 0.85f, blue = baseColor.blue * 0.85f)
                                )
                            )
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), paint)
                        
                        // Highlight matched extensions with a bright border
                        if (highlightedExtension != null && rect.extension() == highlightedExtension) {
                            val highlightPaint = Paint().apply {
                                color = Color.White
                                strokeWidth = 2f
                                style = PaintingStyle.Stroke
                            }
                            canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), highlightPaint)
                        } else {
                            val lightPaint = Paint().apply {
                                color = Color.White.copy(alpha = if (highlightedExtension == null) 0.2f else 0.05f)
                                strokeWidth = 1f
                                style = PaintingStyle.Stroke
                            }
                            canvas.drawLine(Offset(drawX, drawY), Offset(drawX + drawW, drawY), lightPaint)
                            canvas.drawLine(Offset(drawX, drawY), Offset(drawX, drawY + drawH), lightPaint)

                            val darkPaint = Paint().apply {
                                color = Color.Black.copy(alpha = if (highlightedExtension == null) 0.3f else 0.1f)
                                strokeWidth = 1f
                                style = PaintingStyle.Stroke
                            }
                            canvas.drawLine(Offset(drawX, drawY + drawH), Offset(drawX + drawW, drawY + drawH), darkPaint)
                            canvas.drawLine(Offset(drawX + drawW, drawY), Offset(drawX + drawW, drawY + drawH), darkPaint)
                        }
                    }
                }
            }
            bitmap
        }
    }

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .onPointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.first().position
                val scaleX = canvasSize.width / baseWidth.toFloat()
                val scaleY = canvasSize.height / baseHeight.toFloat()
                
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
            .onPointerEvent(PointerEventType.Press) { event ->
                val change = event.changes.first()
                if (change.pressed) {
                    currentHovered?.let { 
                        if (event.buttons.isSecondaryPressed) {
                            onSecondaryClick(it.path(), change.position)
                        } else {
                            onClick(it.path())
                        }
                    }
                }
            }
    ) {
        canvasSize = size
        
        treemapBitmap?.let {
            drawImage(it)
        }

        val scaleX = size.width / baseWidth.toFloat()
        val scaleY = size.height / baseHeight.toFloat()

        currentHovered?.let { rect ->
            val drawX = rect.x().toFloat() * scaleX
            val drawY = rect.y().toFloat() * scaleY
            val drawW = rect.width().toFloat() * scaleX
            val drawH = rect.height().toFloat() * scaleY

            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(drawX, drawY),
                size = Size(drawW, drawH)
            )
            
            drawRect(
                color = Color.White,
                topLeft = Offset(drawX, drawY),
                size = Size(drawW, drawH),
                style = Stroke(width = 3f)
            )
        }

        // Draw selection highlight - make it pop!
        if (selectedPath != null) {
            rects.find { it.path() == selectedPath }?.let { rect ->
                val drawX = rect.x().toFloat() * scaleX
                val drawY = rect.y().toFloat() * scaleY
                val drawW = rect.width().toFloat() * scaleX
                val drawH = rect.height().toFloat() * scaleY

                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(drawX, drawY),
                    size = Size(drawW, drawH)
                )
                
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(drawX, drawY),
                    size = Size(drawW, drawH),
                    style = Stroke(width = 6f)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(drawX, drawY),
                    size = Size(drawW, drawH),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}
