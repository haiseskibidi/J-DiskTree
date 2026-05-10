package com.jdisktree.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.jdisktree.domain.TreeMapRect
import java.nio.file.Paths

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
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.widthIn(max = 300.dp).border(1.dp, Color(0xFF555555), RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val fileName = Paths.get(rect.path()).fileName?.toString() ?: "Unknown"
                Text(fileName, style = MaterialTheme.typography.subtitle2, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Size: ${formatSize(rect.size())}", style = MaterialTheme.typography.body2, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(rect.path(), style = MaterialTheme.typography.caption, color = Color.LightGray, maxLines = 3)
                if (rect.isDirectory) {
                    Text("Type: DIRECTORY", style = MaterialTheme.typography.caption, color = Color(0xFF42A5F5))
                } else if (rect.extension().isNotEmpty()) {
                    val extLabel = if (rect.extension() == "dir_block") "COMPRESSED FOLDER" else rect.extension().uppercase()
                    Text("Type: $extLabel", style = MaterialTheme.typography.caption, color = Color(0xFFFFA726))
                }
            }
        }
    }
}
