package com.lwr.watermarkcamera.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import com.lwr.watermarkcamera.data.WatermarkData
import com.lwr.watermarkcamera.utils.WatermarkGenerator
import com.lwr.watermarkcamera.utils.FolderManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu

// 图片信息数据类
data class ImageInfo(
    val uri: Uri,
    var latitude: String = "",
    var longitude: String = "",
    var location: String = "", // 改为可选
    var date: String = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.getDefault()
    ).format(Date()), // 改为年月日格式
    var showDate: Boolean = false, // 是否在水印中显示日期（默认关闭）
    var showLocation: Boolean = false, // 是否在水印中显示地点（默认关闭）
    var watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    var hasExifGpsData: Boolean = false, // 是否包含EXIF GPS数据
    var exifTimestamp: Long = 0, // EXIF中的时间戳
    var imageName: String = "", // 图片名称
    var showImageName: Boolean = false // 是否显示图片名称
)

// 水印位置枚举
enum class WatermarkPosition(val displayName: String) {
    TOP_LEFT("左上角"),
    TOP_RIGHT("右上角"),
    BOTTOM_LEFT("左下角"),
    BOTTOM_RIGHT("右下角"),
    CENTER("居中")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectionScreen(
    selectedFolder: File,
    watermarkData: WatermarkData,
    watermarkGenerator: WatermarkGenerator,
    folderManager: FolderManager,
    exifDataReader: com.lwr.watermarkcamera.utils.ExifDataReader,
    locationService: com.lwr.watermarkcamera.utils.LocationService,
    permissionChecker: com.lwr.watermarkcamera.utils.PermissionChecker,
    database: com.lwr.watermarkcamera.data.AppDatabase,
    onImageSelected: (Uri) -> Unit,
    onBackPressed: () -> Unit,
    onStartCamera: () -> Unit,
    onProcessingComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImages by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
    var commonTitle by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var processedCount by remember { mutableStateOf(0) }
    var isLoadingExif by remember { mutableStateOf(false) }
    var loadedExifCount by remember { mutableStateOf(0) }

    // 历史记录状态
    val titleHistory by database.historyRecordDao().getHistoryByType("title")
        .collectAsState(initial = emptyList())
    var showTitleHistory by remember { mutableStateOf(false) }

    // 删除历史记录函数
    fun deleteFromHistory(history: com.lwr.watermarkcamera.data.HistoryRecord) {
        scope.launch {
            database.historyRecordDao().deleteHistory(history)
        }
    }

    // 检查权限状态
    val permissionReport = remember {
        permissionChecker.checkAllPermissions()
    }

    val hasMediaPermission = remember {
        permissionChecker.hasMediaPermission()
    }
    
    // 检查Android 10+的存储权限
    val hasStoragePermission = remember {
        permissionChecker.hasStoragePermission()
    }
    
    // 综合权限检查
    val canAccessImages = remember {
        hasMediaPermission && hasStoragePermission
    }

    // 多图片选择器
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        try {
            if (uris.isNotEmpty()) {
                // 先创建基本的ImageInfo对象
                val initialImages = uris.take(9).map { uri ->
                    ImageInfo(uri = uri)
                }
                selectedImages = initialImages

                // 然后异步加载每张图片的EXIF数据
                scope.launch {
                    isLoadingExif = true
                    loadedExifCount = 0

                    val updatedImages = initialImages.mapIndexed { index, imageInfo ->
                        try {
                            // 提取EXIF数据
                            val exifData = exifDataReader.extractExifData(imageInfo.uri)

                            // 检查是否有错误
                            if (exifData.errorMessage != null) {
                                Log.w(
                                    "ImageSelection",
                                    "图片 ${index + 1} EXIF提取失败: ${exifData.errorMessage}"
                                )
                                loadedExifCount++
                                return@mapIndexed imageInfo // 返回原始对象
                            }

                            // 如果有GPS数据，尝试获取地址信息
                            var locationName = ""
                            if (exifData.hasGpsData && exifData.latitude != 0.0 && exifData.longitude != 0.0) {
                                try {
                                    locationName = locationService.getAddressFromLocation(
                                        exifData.latitude,
                                        exifData.longitude
                                    )
                                } catch (e: Exception) {
                                    Log.w("ImageSelection", "获取地址信息失败: ${e.message}")
                                }
                            }

                            // 更新ImageInfo对象
                            val updatedInfo = imageInfo.copy(
                                latitude = if (exifData.hasGpsData) exifData.latitude.toString() else "",
                                longitude = if (exifData.hasGpsData) exifData.longitude.toString() else "",
                                location = locationName,
                                date = exifData.formattedDate,
                                hasExifGpsData = exifData.hasGpsData,
                                exifTimestamp = exifData.timestamp,
                                // 根据是否有地点信息自动设置开关
                                showLocation = locationName.isNotBlank(),
                                // 根据是否有日期信息自动设置开关
                                showDate = exifData.formattedDate.isNotBlank() && exifData.formattedDate != SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).format(Date()) // 如果不是默认日期才开启
                            )

                            loadedExifCount++
                            updatedInfo
                        } catch (e: Exception) {
                            Log.e("ImageSelection", "处理图片 ${index + 1} 时出错: ${e.message}")
                            loadedExifCount++
                            // 如果出错，返回原始对象但确保开关关闭
                            imageInfo.copy(
                                showLocation = false,
                                showDate = false
                            )
                        }
                    }
                    selectedImages = updatedImages
                    isLoadingExif = false

                    // 显示提示
                    withContext(Dispatchers.Main) {
                        val exifCount = updatedImages.count { it.hasExifGpsData }
                        if (exifCount > 0) {
                            Toast.makeText(
                                context,
                                "已从 $exifCount/${updatedImages.size} 张图片中提取GPS信息",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "未能从图片中提取GPS信息，请手动输入",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                // 用户取消了选择
                Toast.makeText(context, "未选择任何图片", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ImageSelection", "图片选择失败: ${e.message}")
            Toast.makeText(context, "图片选择失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 处理图片的函数
    fun processImages() {
        if (selectedImages.isEmpty() || commonTitle.isBlank()) return

        scope.launch {
            isProcessing = true
            processedCount = 0

            try {
                withContext(Dispatchers.IO) {
                    selectedImages.forEachIndexed { index, imageInfo ->
                        try {
                            // 验证必填字段（只有标题和经纬度是必填的）
                            if (imageInfo.latitude.isBlank() || imageInfo.longitude.isBlank()) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "图片 ${index + 1} 缺少经纬度信息",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@forEachIndexed
                            }

                            // 验证经纬度格式并保持精度
                            val lat = try {
                                java.math.BigDecimal(imageInfo.latitude).toDouble()
                            } catch (e: NumberFormatException) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "图片 ${index + 1} 纬度格式错误",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@forEachIndexed
                            }

                            val lng = try {
                                java.math.BigDecimal(imageInfo.longitude).toDouble()
                            } catch (e: NumberFormatException) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "图片 ${index + 1} 经度格式错误",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@forEachIndexed
                            }

                            // 获取序列号
                            val sequenceNumber =
                                folderManager.getNextSequenceNumberFromFiles(selectedFolder.name)

                            // 解析日期时间
                            val timestamp = try {
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).parse(imageInfo.date)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }

                            // 创建水印数据
                            val currentWatermarkData = watermarkData.copy(
                                title = commonTitle,
                                timestamp = timestamp,
                                latitude = lat.toDouble(),
                                longitude = lng.toDouble(),
                                latitudeString = imageInfo.latitude, // 保持原始精度
                                longitudeString = imageInfo.longitude, // 保持原始精度
                                locationName = imageInfo.location.takeIf { it.isNotBlank() }
                                    ?: "", // 可选字段
                                weather = "", // 清空天气信息
                                temperature = "", // 清空温度信息
                                showWeatherInfo = false, // 不显示天气信息
                                showLocation = imageInfo.showLocation, // 是否显示地点
                                sequenceNumber = sequenceNumber,
                                watermarkPosition = convertWatermarkPosition(imageInfo.watermarkPosition),
                                showDate = imageInfo.showDate // 是否显示日期
                            )

                            // 生成文件名
                            val finalImageName = if (imageInfo.showImageName && imageInfo.imageName.isNotBlank()) {
                                imageInfo.imageName
                            } else {
                                ""
                            }
                            
                            val fileName = folderManager.generateFileName(
                                sequenceNumber = sequenceNumber,
                                timestamp = currentWatermarkData.timestamp,
                                title = commonTitle,
                                imageName = finalImageName,
                                showImageName = imageInfo.showImageName
                            )

                            // 添加水印并保存
                            val success = watermarkGenerator.addWatermarkToSelectedImageAndSave(
                                context = context,
                                imageUri = imageInfo.uri,
                                watermarkData = currentWatermarkData,
                                fileName = fileName,
                                folderName = selectedFolder.name
                            )

                            if (success) {
                                processedCount++
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "已处理第 ${processedCount} 张图片",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "图片 ${index + 1} 处理失败",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "图片 ${index + 1} 处理出错: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                // 处理完成
                withContext(Dispatchers.Main) {
                    if (processedCount > 0) {
                        Toast.makeText(
                            context,
                            "成功处理 $processedCount 张图片",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // 保存标题到历史记录
                        if (commonTitle.isNotBlank()) {
                            scope.launch {
                                val historyRecord = com.lwr.watermarkcamera.data.HistoryRecord(
                                    type = "title",
                                    content = commonTitle,
                                    timestamp = System.currentTimeMillis()
                                )
                                database.historyRecordDao().insertHistory(historyRecord)
                            }
                        }
                        
                        // 清空已处理的图片
                        selectedImages = emptyList()
                        commonTitle = ""
                        onProcessingComplete()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "批量处理失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isProcessing = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.Black
                )
            }

            Text(
                text = "选择图片",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // 占位符，保持标题居中
            Box(modifier = Modifier.size(48.dp))
        }

        // 主要内容区域（占满剩余空间）
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues( vertical = 16.dp)
        ) {
            // 功能选择区域
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "选择功能",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 20.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 拍照功能卡片
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.1f
                                )
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier =Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "拍照添加水印",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "使用相机拍摄新照片并添加水印",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = onStartCamera,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("开始", fontSize = 14.sp)
                                }
                            }
                        }

                        // 选择图片功能卡片
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.1f
                                )
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "选择多张图片添加水印",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "从相册中选择图片（最多9张）",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { multipleImagePickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        enabled = !isProcessing && canAccessImages
                                    ) {
                                        Text("选择", fontSize = 14.sp)
                                    }
                                }

                                // 权限提示
                                if (!canAccessImages) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                                alpha = 0.3f
                                            )
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = "⚠️ 缺少存储访问权限",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Android ${permissionReport.androidVersion}需要授予存储权限才能访问图片文件",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (permissionReport.androidVersion >= 29) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Android 10+请确保已授予所有文件访问权限",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.error
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

            // 共用标题输入
            if (selectedImages.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "共用标题",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
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
                                value = commonTitle,
                                onValueChange = { commonTitle = it },
                                label = { Text("请输入标题") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isProcessing,
                                placeholder = { Text("例如：工程检查、会议记录") },
                                isError = commonTitle.isEmpty(),
                                supportingText = {
                                    if (commonTitle.isEmpty()) {
                                        Text("请输入标题", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text(
                                            "提示：处理完成后将保存到历史记录",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (commonTitle.isNotEmpty()) {
                                        IconButton(
                                            onClick = { commonTitle = "" },
                                            enabled = !isProcessing
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
                                                        commonTitle = history.content
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
            }

            // 处理进度显示
            if (isProcessing) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "正在处理图片...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = if (selectedImages.isNotEmpty()) processedCount.toFloat() / selectedImages.size else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "已处理 $processedCount / ${selectedImages.size} 张图片",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 加载EXIF状态指示器
            if (isLoadingExif) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "正在提取图片信息...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = if (selectedImages.isNotEmpty()) loadedExifCount.toFloat() / selectedImages.size else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "已处理 $loadedExifCount / ${selectedImages.size} 张图片",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 选中的图片列表
            if (selectedImages.isNotEmpty()) {
                itemsIndexed(selectedImages) { index, imageInfo ->
                    ImageConfigCard(
                        index = index,
                        imageInfo = imageInfo,
                        onImageInfoChanged = { updatedImageInfo ->
                            selectedImages = selectedImages.toMutableList().apply {
                                set(index, updatedImageInfo)
                            }
                        },
                        onRemoveImage = {
                            selectedImages = selectedImages.toMutableList().apply {
                                removeAt(index)
                            }
                        },
                        enabled = !isProcessing
                    )
                }
            }
        }

        // 底部按钮区域（固定在底部）
        if (selectedImages.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { processImages() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isProcessing && commonTitle.isNotBlank() && selectedImages.all {
                            it.latitude.isNotBlank() && it.longitude.isNotBlank() // 只验证必填字段
                        }
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("处理中...", fontSize = 16.sp)
                        } else {
                            Text("开始处理", fontSize = 16.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            selectedImages = emptyList()
                            commonTitle = ""
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        Text("清空所有", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 转换水印位置枚举
private fun convertWatermarkPosition(position: WatermarkPosition): com.lwr.watermarkcamera.data.WatermarkPosition {
    return when (position) {
        WatermarkPosition.TOP_LEFT -> com.lwr.watermarkcamera.data.WatermarkPosition.TOP_LEFT
        WatermarkPosition.TOP_RIGHT -> com.lwr.watermarkcamera.data.WatermarkPosition.TOP_RIGHT
        WatermarkPosition.BOTTOM_LEFT -> com.lwr.watermarkcamera.data.WatermarkPosition.BOTTOM_LEFT
        WatermarkPosition.BOTTOM_RIGHT -> com.lwr.watermarkcamera.data.WatermarkPosition.BOTTOM_RIGHT
        WatermarkPosition.CENTER -> com.lwr.watermarkcamera.data.WatermarkPosition.BOTTOM_RIGHT // CENTER映射为BOTTOM_RIGHT
    }
}

@Composable
private fun ImageConfigCard(
    index: Int,
    imageInfo: ImageInfo,
    onImageInfoChanged: (ImageInfo) -> Unit,
    onRemoveImage: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 图片标题和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "图片 ${index + 1}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier.size(32.dp),
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除图片",
                        tint = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图片预览
            AsyncImage(
                model = imageInfo.uri,
                contentDescription = "选择的图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            // EXIF信息提示
            if (imageInfo.hasExifGpsData) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "已从图片中提取GPS信息",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 纬度输入
            OutlinedTextField(
                value = imageInfo.latitude,
                onValueChange = { newValue ->
                    // 验证纬度范围
                    val isValid = try {
                        if (newValue.isNotBlank()) {
                            val double = newValue.toDouble()
                            double >= -90 && double <= 90
                        } else {
                            true
                        }
                    } catch (e: NumberFormatException) {
                        true // 允许用户输入过程中的格式错误
                    }
                    
                    if (isValid) {
                        onImageInfoChanged(imageInfo.copy(latitude = newValue))
                    }
                },
                label = { Text("纬度* (-90~90)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = enabled,
                isError = imageInfo.latitude.isNotBlank() && try {
                    val lat = imageInfo.latitude.toDouble()
                    lat < -90 || lat > 90
                } catch (e: NumberFormatException) {
                    true
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 经度输入
            OutlinedTextField(
                value = imageInfo.longitude,
                onValueChange = { newValue ->
                    // 验证经度范围
                    val isValid = try {
                        if (newValue.isNotBlank()) {
                            val double = newValue.toDouble()
                            double >= -180 && double <= 180
                        } else {
                            true
                        }
                    } catch (e: NumberFormatException) {
                        true // 允许用户输入过程中的格式错误
                    }
                    
                    if (isValid) {
                        onImageInfoChanged(imageInfo.copy(longitude = newValue))
                    }
                },
                label = { Text("经度* (-180~180)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = enabled,
                isError = imageInfo.longitude.isNotBlank() && try {
                    val lng = imageInfo.longitude.toDouble()
                    lng < -180 || lng > 180
                } catch (e: NumberFormatException) {
                    true
                }
            )

                        Spacer(modifier = Modifier.height(12.dp))
            
            // 地点显示开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "显示地点",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "在水印中显示地点信息",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = imageInfo.showLocation,
                    onCheckedChange = { newValue ->
                        onImageInfoChanged(imageInfo.copy(showLocation = newValue))
                    },
                    enabled = enabled
                )
            }
            
            // 地点输入 (可选)
            if (imageInfo.showLocation) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = imageInfo.location,
                    onValueChange = { 
                        onImageInfoChanged(imageInfo.copy(location = it))
                    },
                    label = { Text("地点名称 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = enabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 日期显示开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "显示日期",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "在水印中显示日期信息",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = imageInfo.showDate,
                    onCheckedChange = { newValue ->
                        onImageInfoChanged(imageInfo.copy(showDate = newValue))
                    },
                    enabled = enabled
                )
            }

            // 日期输入 (可选，年月日格式)
            if (imageInfo.showDate) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = imageInfo.date,
                    onValueChange = { newValue ->
                        // 简单的日期格式验证
                        val datePattern = "\\d{4}-\\d{2}-\\d{2}".toRegex()
                        if (newValue.isEmpty() || datePattern.matches(newValue) || newValue.length < 10) {
                            onImageInfoChanged(imageInfo.copy(date = newValue))
                        }
                    },
                    label = { Text("日期 (可选，格式：2025-01-01)") },
                    placeholder = { Text("yyyy-MM-dd") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = enabled,
                    isError = imageInfo.date.isNotBlank() && try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(imageInfo.date)
                        false
                    } catch (e: Exception) {
                        true
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图片名称显示开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "显示图片名称",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "在文件名中包含图片名称",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = imageInfo.showImageName,
                    onCheckedChange = { newValue ->
                        onImageInfoChanged(imageInfo.copy(showImageName = newValue))
                    },
                    enabled = enabled
                )
            }

            // 图片名称输入 (可选)
            if (imageInfo.showImageName) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = imageInfo.imageName,
                    onValueChange = { newValue ->
                        onImageInfoChanged(imageInfo.copy(imageName = newValue))
                    },
                    label = { Text("图片名称 (可选)") },
                    placeholder = { Text("输入图片名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = enabled
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 水印位置选择
            Column {
                Text(
                    text = "水印位置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WatermarkPosition.values()) { position ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    onImageInfoChanged(imageInfo.copy(watermarkPosition = position))
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (imageInfo.watermarkPosition == position) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (imageInfo.watermarkPosition == position) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Text(
                                text = position.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = if (imageInfo.watermarkPosition == position) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.38f
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}