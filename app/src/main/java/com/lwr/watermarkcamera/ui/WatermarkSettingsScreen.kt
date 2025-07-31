package com.lwr.watermarkcamera.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.platform.LocalContext
import com.lwr.watermarkcamera.data.WatermarkData
import com.lwr.watermarkcamera.data.WatermarkPosition
import com.lwr.watermarkcamera.data.WatermarkStyle
import com.lwr.watermarkcamera.data.TimeFormat
import com.lwr.watermarkcamera.data.AppDatabase
import com.lwr.watermarkcamera.data.HistoryRecord
import kotlinx.coroutines.launch

@Composable
fun WatermarkSettingsScreen(
    watermarkData: WatermarkData,
    onWatermarkDataChange: (WatermarkData) -> Unit,
    onStartCamera: () -> Unit,
    onStartImageSelection: () -> Unit,
    onBackToFolderSelection: () -> Unit,
    database: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 历史记录状态
    val titleHistory by database.historyRecordDao().getHistoryByType("title")
        .collectAsState(initial = emptyList())
    val imageNameHistory by database.historyRecordDao().getHistoryByType("imageName")
        .collectAsState(initial = emptyList())

    var showTitleHistory by remember { mutableStateOf(false) }
    var showImageNameHistory by remember { mutableStateOf(false) }

    // 删除历史记录函数
    fun deleteFromHistory(history: HistoryRecord) {
        scope.launch {
            database.historyRecordDao().deleteHistory(history)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "请输入标题（必填）",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = { showTitleHistory = !showTitleHistory }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "历史记录",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                    Text(
                                        "提示：点击开始拍照后将保持此标题",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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

                        // 标题历史记录
                        if (showTitleHistory && titleHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.3f
                                    )
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "历史记录",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    titleHistory.forEach { history ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onWatermarkDataChange(watermarkData.copy(title = history.content))
                                                    showTitleHistory = false
                                                }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = history.content,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { deleteFromHistory(history) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "删除",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 图片名称输入
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "图片名称设置",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { showImageNameHistory = !showImageNameHistory },
                                    enabled = watermarkData.showImageName
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "历史记录",
                                        tint = if (watermarkData.showImageName)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }

                                Switch(
                                    checked = watermarkData.showImageName,
                                    onCheckedChange = {
                                        onWatermarkDataChange(
                                            watermarkData.copy(
                                                showImageName = it
                                            )
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (watermarkData.showImageName) {
                            OutlinedTextField(
                                value = watermarkData.imageName,
                                onValueChange = { onWatermarkDataChange(watermarkData.copy(imageName = it)) },
                                label = { Text("图片名称") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("例如：A001、检查点1、会议室") },
                                isError = watermarkData.imageName.isEmpty(),
                                supportingText = {
                                    if (watermarkData.imageName.isEmpty()) {
                                        Text(
                                            "请输入图片名称",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            "保存格式：标题-图片名称",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (watermarkData.imageName.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                onWatermarkDataChange(
                                                    watermarkData.copy(
                                                        imageName = ""
                                                    )
                                                )
                                            }
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

                            // 图片名称历史记录
                            if (showImageNameHistory && imageNameHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.3f
                                        )
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = "历史记录",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        imageNameHistory.forEach { history ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onWatermarkDataChange(
                                                            watermarkData.copy(
                                                                imageName = history.content
                                                            )
                                                        )
                                                        showImageNameHistory = false
                                                    }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = history.content,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { deleteFromHistory(history) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "删除",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.3f
                                    )
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "图片名称已禁用",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "保存格式：标题",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 预览保存的完整名称
                        if (watermarkData.title.isNotEmpty() && (!watermarkData.showImageName || watermarkData.imageName.isNotEmpty())) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.3f
                                    )
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
                                        text = if (watermarkData.showImageName && watermarkData.imageName.isNotEmpty()) {
                                            "${watermarkData.title}-${watermarkData.imageName}.jpg"
                                        } else {
                                            "${watermarkData.title}.jpg"
                                        },
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
                                    onClick = {
                                        onWatermarkDataChange(
                                            watermarkData.copy(
                                                watermarkStyle = style
                                            )
                                        )
                                    }
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
                                onClick = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            watermarkPosition = WatermarkPosition.TOP_LEFT
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            WatermarkPositionButton(
                                text = "右上",
                                selected = watermarkData.watermarkPosition == WatermarkPosition.TOP_RIGHT,
                                onClick = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            watermarkPosition = WatermarkPosition.TOP_RIGHT
                                        )
                                    )
                                },
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
                                onClick = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            watermarkPosition = WatermarkPosition.BOTTOM_LEFT
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            WatermarkPositionButton(
                                text = "右下",
                                selected = watermarkData.watermarkPosition == WatermarkPosition.BOTTOM_RIGHT,
                                onClick = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            watermarkPosition = WatermarkPosition.BOTTOM_RIGHT
                                        )
                                    )
                                },
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
                                text = "显示时间",
                                fontSize = 16.sp
                            )
                            Switch(
                                checked = watermarkData.showTime,
                                onCheckedChange = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            showTime = it
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "显示日期",
                                fontSize = 16.sp
                            )
                            Switch(
                                checked = watermarkData.showDate,
                                onCheckedChange = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            showDate = it
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "显示地点",
                                fontSize = 16.sp
                            )
                            Switch(
                                checked = watermarkData.showLocation,
                                onCheckedChange = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            showLocation = it
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "显示经纬度",
                                fontSize = 16.sp
                            )
                            Switch(
                                checked = watermarkData.showCoordinates,
                                onCheckedChange = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            showCoordinates = it
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
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
                                onCheckedChange = {
                                    onWatermarkDataChange(
                                        watermarkData.copy(
                                            showWeatherInfo = it
                                        )
                                    )
                                },
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
                            text = "照片将根据设置添加以下水印信息：",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "• 标题（必填）\n" +
                                    "• 拍摄时间（可选）\n" +
                                    "• 地理位置（可选）\n" +
                                    "• 经纬度（可选）\n" +
                                    "• 天气信息（可选）\n" +
                                    "• 图片名称（可选）\n" +
                                    "• 保存格式：启用图片名称时为 标题-图片名称，禁用时为 标题",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // 底部按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 开始拍照按钮
            Button(
                onClick = {
                    if (watermarkData.title.isNotEmpty() &&
                        (!watermarkData.showImageName || watermarkData.imageName.isNotEmpty())
                    ) {
                        onStartCamera()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = watermarkData.title.isNotEmpty() &&
                        (!watermarkData.showImageName || watermarkData.imageName.isNotEmpty())
            ) {
                Text(
                    text = "开始拍照",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // 选择图片按钮
            OutlinedButton(
                onClick = {
                    onStartImageSelection()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "选择图片添加水印",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }


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