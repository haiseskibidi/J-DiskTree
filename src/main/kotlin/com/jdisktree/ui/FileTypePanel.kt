package com.jdisktree.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdisktree.domain.FileTypeStat

@Composable
fun FileTypePanel(
    stats: List<FileTypeStat>,
    selectedExtension: String?,
    onSelect: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(Dimens.SpacingMedium)) {
        Text(
            text = stringResource("file_types"),
            style = MaterialTheme.typography.h6,
            color = Color.White,
            modifier = Modifier.padding(bottom = Dimens.SpacingMedium)
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
            items(stats, key = { stat -> stat.extension }) { stat ->
                val isSelected = stat.extension == selectedExtension
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.RadiusMedium))
                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { 
                            onSelect(if (isSelected) null else stat.extension)
                        }
                        .padding(Dimens.SpacingMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.IconSmall)
                                    .background(getColorForExtension(stat.extension), RoundedCornerShape(Dimens.RadiusSmall))
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                            Text(
                                text = ".${stat.extension}",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = formatSize(stat.totalSize),
                            style = MaterialTheme.typography.caption,
                            color = Color.LightGray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    
                    LinearProgressIndicator(
                        progress = stat.percentage.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(Dimens.SpacingSmall).clip(RoundedCornerShape(Dimens.RadiusSmall)),
                        color = getColorForExtension(stat.extension),
                        backgroundColor = Color.DarkGray
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = String.format("%.1f%%", stat.percentage * 100),
                            style = MaterialTheme.typography.overline,
                            color = Color.Gray
                        )
                        Text(
                            text = "${stat.count} ${stringResource("files")}",
                            style = MaterialTheme.typography.overline,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
