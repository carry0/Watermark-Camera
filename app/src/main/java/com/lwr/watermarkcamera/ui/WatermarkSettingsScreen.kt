package com.lwr.watermarkcamera.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lwr.watermarkcamera.data.WatermarkData
import com.lwr.watermarkcamera.data.WatermarkPosition
import com.lwr.watermarkcamera.data.WatermarkStyle
import com.lwr.watermarkcamera.data.TimeFormat

@Composable
fun WatermarkSettingsScreen(
    watermarkData: WatermarkData,
    onWatermarkDataChange: (WatermarkData) -> Unit,
    onStartCamera: () -> Unit,
    onBackToFolderSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToFolderSelection,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.Black
                )
            }

            Text(
                text = "拍照前设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // 占位符，保持标题居中
            Box(modifier = Modifier.size(48.dp))
        }

        // 可滚动的内容区域
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题输入
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "请输入标题（必填）",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = watermarkData.title,
                            onValueChange = { onWatermarkDataChange(watermarkData.copy(title = it)) },
                            label = { Text("标题") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例如：工程检查、会议记录") },
                            isError = watermarkData.title.isEmpty(),
                            supportingText = {
                                if (watermarkData.title.isEmpty()) {
                                    Text("请输入标题", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("提示：点击开始拍照后将保持此标题", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            trailingIcon = {
                                if (watermarkData.title.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onWatermarkDataChange(watermarkData.copy(title = "")) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "清空标题",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 图片名称输入
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "请输入图片名称（必填）",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = watermarkData.imageName,
                            onValueChange = { onWatermarkDataChange(watermarkData.copy(imageName = it)) },
                            label = { Text("图片名称") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("例如：A001、检查点1、会议室") },
                            isError = watermarkData.imageName.isEmpty(),
                            supportingText = {
                                if (watermarkData.imageName.isEmpty()) {
                                    Text("请输入图片名称", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("保存格式：(标题+图片名称)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            trailingIcon = {
                                if (watermarkData.imageName.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onWatermarkDataChange(watermarkData.copy(imageName = "")) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "清空图片名称",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                        
                        // 预览保存的完整名称
                        if (watermarkData.title.isNotEmpty() && watermarkData.imageName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "预览保存名称",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${watermarkData.title}-${watermarkData.imageName}.jpg",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 水印样式设置
            item {
                Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                            text = "水印样式",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(WatermarkStyle.entries.size) { index ->
                                val style = WatermarkStyle.entries[index]
                                WatermarkStylePreview(
                                    style = style,
                                    selected = watermarkData.watermarkStyle == style,
                                    onClick = { onWatermarkDataChange(watermarkData.copy(watermarkStyle = style)) }
                                )
                            }
                        }
                    }
                }
            }

            // 水印位置设置
            item {
                Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                            text = "水印位置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WatermarkPositionButton(
                                text = "左上",
                                selected = watermarkData.watermarkPosition == WatermarkPosition.TOP_LEFT,
                                onClick = { onWatermarkDataChange(watermarkData.copy(watermarkPosition = WatermarkPosition.TOP_LEFT)) },
                                modifier = Modifier.weight(1f)
                            )
                            WatermarkPositionButton(
                                text = "右上",
                                selected = watermarkData.watermarkPosition == WatermarkPosition.TOP_RIGHT,
                                onClick = { onWatermarkDataChange(watermarkData.copy(watermarkPosition = WatermarkPosition.TOP_RIGHT)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WatermarkPositionButton(
                                text = "左下",
                                selected = watermarkData.watermarkPosition == WatermarkPosition.BOTTOM_LEFT,
                                onClick = { onWatermarkDataChange(watermarkData.copy(watermarkPosition = WatermarkPosition.BOTTOM_LEFT)) },
                                modifier = Modifier.weight(1f)
                            )
                            WatermarkPositionButton(
                                text = "右下",
                                selected = watermarkData.watermarkPosition == WatermarkPosition.BOTTOM_RIGHT,
                                onClick = { onWatermarkDataChange(watermarkData.copy(watermarkPosition = WatermarkPosition.BOTTOM_RIGHT)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 显示设置
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "显示设置",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "时间格式",
                                fontSize = 16.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = watermarkData.timeFormat == TimeFormat.DATE_ONLY,
                                    onClick = { onWatermarkDataChange(watermarkData.copy(timeFormat = TimeFormat.DATE_ONLY)) },
                                    label = { Text("仅日期") }
                                )
                                FilterChip(
                                    selected = watermarkData.timeFormat == TimeFormat.DATE_TIME,
                                    onClick = { onWatermarkDataChange(watermarkData.copy(timeFormat = TimeFormat.DATE_TIME)) },
                                    label = { Text("日期时间") }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "显示天气",
                                fontSize = 16.sp
                            )
                            Switch(
                                checked = watermarkData.showWeatherInfo,
                                onCheckedChange = { onWatermarkDataChange(watermarkData.copy(showWeatherInfo = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // 水印信息说明
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "水印信息说明",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "照片将自动添加以下水印信息：",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "• 标题（必填）\n" +
                                  "• 拍摄时间\n" +
                                  "• 地理位置和经纬度\n" +
                                  "• 保存格式：(标题+图片名称)，如：水印-A001",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // 底部按钮
        Button(
            onClick = {
                if (watermarkData.title.isNotEmpty() && watermarkData.imageName.isNotEmpty()) {
                    onStartCamera()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = watermarkData.title.isNotEmpty() && watermarkData.imageName.isNotEmpty()
        ) {
            Text(
                text = "开始拍照",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun WatermarkPositionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.height(48.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun WatermarkStylePreview(
    style: WatermarkStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 样式名称
            Text(
                text = when (style) {
                    WatermarkStyle.SIMPLE -> "简约样式"
                    WatermarkStyle.CARD -> "卡片样式"
                    WatermarkStyle.GRADIENT -> "渐变样式"
                    WatermarkStyle.BORDERED -> "描边样式"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 样式预览示例
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (style) {
                    WatermarkStyle.SIMPLE -> {
                        // 简约样式：白色文字带阴影
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "标题",
                                fontSize = 12.sp,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            )
                            Text(
                                text = "时间: 2024-01-20",
                                fontSize = 10.sp,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                )
                            )
                        }
                    }
                    WatermarkStyle.CARD -> {
                        // 卡片样式：半透明背景
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "标题",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "时间: 2024-01-20",
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    WatermarkStyle.GRADIENT -> {
                        // 渐变样式：渐变背景
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Black.copy(alpha = 0.3f)
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "标题",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "时间: 2024-01-20",
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    WatermarkStyle.BORDERED -> {
                        // 描边样式：文字描边效果
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "标题",
                                fontSize = 12.sp,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                            Text(
                                text = "时间: 2024-01-20",
                                fontSize = 10.sp,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}