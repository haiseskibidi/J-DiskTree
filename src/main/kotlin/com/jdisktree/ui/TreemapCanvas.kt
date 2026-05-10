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
import androidx.compose.ui.input.pointer.onPointerEvent
import com.jdisktree.domain.TreeMapRect
import com.jdisktree.treemap.index.SpatialGridIndex

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun TreemapCanvas(
    rects: List<TreeMapRect>,
    index: SpatialGridIndex?,
    onHover: (TreeMapRect?, Offset) -> Unit,
    onClick: (String) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var currentHovered by remember { mutableStateOf<TreeMapRect?>(null) }

    val treemapBitmap = remember(rects, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || rects.isEmpty()) null
        else {
            val bitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
            val canvas = Canvas(bitmap)
            val scaleX = canvasSize.width / 1000f
            val scaleY = canvasSize.height / 1000f

            // 1. Draw all directory backgrounds (provides a clean base color for gaps)
            rects.forEach { rect ->
                if (rect.isDirectory) {
                    val drawX = rect.x().toFloat() * scaleX
                    val drawY = rect.y().toFloat() * scaleY
                    val drawW = rect.width().toFloat() * scaleX
                    val drawH = rect.height().toFloat() * scaleY

                    // Subtle folder color (slightly lighter than global background)
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

                    val baseColor = getColorForExtension(rect.extension())
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
                                    baseColor.copy(alpha = 1f),
                                    baseColor.copy(red = baseColor.red * 0.85f, green = baseColor.green * 0.85f, blue = baseColor.blue * 0.85f)
                                )
                            )
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), paint)
                        
                        val lightPaint = Paint().apply {
                            color = Color.White.copy(alpha = 0.2f)
                            strokeWidth = 1f
                            style = PaintingStyle.Stroke
                        }
                        canvas.drawLine(Offset(drawX, drawY), Offset(drawX + drawW, drawY), lightPaint)
                        canvas.drawLine(Offset(drawX, drawY), Offset(drawX, drawY + drawH), lightPaint)

                        val darkPaint = Paint().apply {
                            color = Color.Black.copy(alpha = 0.3f)
                            strokeWidth = 1f
                            style = PaintingStyle.Stroke
                        }
                        canvas.drawLine(Offset(drawX, drawY + drawH), Offset(drawX + drawW, drawY + drawH), darkPaint)
                        canvas.drawLine(Offset(drawX + drawW, drawY), Offset(drawX + drawW, drawY + drawH), darkPaint)

                        val borderPaint = Paint().apply {
                            color = Color.Black.copy(alpha = 0.2f)
                            strokeWidth = 0.5f
                            style = PaintingStyle.Stroke
                        }
                        canvas.drawRect(androidx.compose.ui.geometry.Rect(drawX, drawY, drawX + drawW, drawY + drawH), borderPaint)
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
            .onPointerEvent(PointerEventType.Press) {
                currentHovered?.let { onClick(it.path()) }
            }
    ) {
        canvasSize = size
        
        treemapBitmap?.let {
            drawImage(it)
        }

        currentHovered?.let { rect ->
            val scaleX = size.width / 1000f
            val scaleY = size.height / 1000f
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
    }
}
