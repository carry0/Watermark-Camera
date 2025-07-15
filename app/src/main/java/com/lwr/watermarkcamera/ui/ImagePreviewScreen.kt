package com.lwr.watermarkcamera.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.lwr.watermarkcamera.data.WatermarkData
import com.lwr.watermarkcamera.utils.WatermarkGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewScreen(
    selectedImageUri: Uri,
    watermarkData: WatermarkData,
    onWatermarkDataChange: (WatermarkData) -> Unit,
    onSaveImage: (Bitmap, WatermarkData) -> Unit,
    onBackPressed: () -> Unit,
    locationService: com.lwr.watermarkcamera.utils.LocationService,
    weatherService: com.lwr.watermarkcamera.utils.WeatherService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var watermarkedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    
    // 加载原始图片
    LaunchedEffect(selectedImageUri) {
        isLoading = true
        try {
            originalBitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(selectedImageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        } catch (e: Exception) {
            // 处理错误
        } finally {
            isLoading = false
        }
    }
    
    // 生成水印预览
    LaunchedEffect(originalBitmap, watermarkData) {
        originalBitmap?.let { bitmap ->
            isProcessing = true
            try {
                watermarkedBitmap = withContext(Dispatchers.IO) {
                    val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = android.graphics.Canvas(newBitmap)
                    WatermarkGenerator.drawWatermarkToCanvas(
                        canvas = canvas,
                        watermarkData = watermarkData,
                        width = bitmap.width,
                        height = bitmap.height,
                        showBackground = watermarkData.watermarkStyle != com.lwr.watermarkcamera.data.WatermarkStyle.SIMPLE,
                        rotationDegrees = 0f,  // 预览时不需要旋转
                        adjustedPosition = watermarkData.watermarkPosition  // 预览时使用原始位置
                    )
                    newBitmap
                }
            } catch (e: Exception) {
                // 处理错误
            } finally {
                isProcessing = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                    text = "图片预览",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // 占位符，保持标题居中
                Box(modifier = Modifier.size(48.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 图片预览区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "图片预览",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "正在加载图片...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // 左右对比预览
                        Text(
                            text = "图片对比预览",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clickable { showPreviewDialog = true },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            val pagerState = rememberPagerState(
                                initialPage = 0
                            ) { 2 }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                pageSpacing = 8.dp
                            ) { page ->
                                when (page) {
                                    0 -> {
                                        // 原始图片
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AsyncImage(
                                                model = selectedImageUri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = "原始图片",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                    1 -> {
                                        // 水印预览
                                        if (isProcessing) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Text(
                                                        text = "生成中...",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else {
                                            watermarkedBitmap?.let { bitmap ->
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    androidx.compose.foundation.Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Text(
                                                        text = "水印预览",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )
                                                }
                                            } ?: run {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "暂无预览",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 页面指示器
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(2) { index ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pagerState.currentPage == index) 
                                                    MaterialTheme.colorScheme.primary
                                                else 
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 水印设置区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            text = "水印设置",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 必填项说明
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "必填项说明",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "以下字段为必填项，请确保填写完整：",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• 标题：用户自定义标题\n" +
                                      "• 经纬度：GPS坐标位置\n" +
                                      "• 地点名称：位置描述信息\n" +
                                      "• 图片名称：可选，影响文件保存格式",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 标题设置
                    OutlinedTextField(
                        value = watermarkData.title,
                        onValueChange = { 
                            onWatermarkDataChange(watermarkData.copy(title = it))
                        },
                        label = { Text("标题", fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        isError = watermarkData.title.isEmpty(),
                        supportingText = {
                            if (watermarkData.title.isEmpty()) {
                                Text("请输入标题", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    
                    // 图片名称设置
                    if (watermarkData.showImageName) {
                        OutlinedTextField(
                            value = watermarkData.imageName,
                            onValueChange = { 
                                onWatermarkDataChange(watermarkData.copy(imageName = it))
                            },
                            label = { Text("图片名称", fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            isError = watermarkData.imageName.isEmpty(),
                            supportingText = {
                                if (watermarkData.imageName.isEmpty()) {
                                    Text("请输入图片名称", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                                    text = "保存格式：标题-序号",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // 手动输入经纬度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = watermarkData.latitude.toString(),
                            onValueChange = { value ->
                                value.toDoubleOrNull()?.let { lat ->
                                    onWatermarkDataChange(watermarkData.copy(latitude = lat))
                                }
                            },
                            label = { Text("纬度", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { Text("例如：39.904202") },
                            isError = watermarkData.latitude == 0.0,
                            supportingText = {
                                if (watermarkData.latitude == 0.0) {
                                    Text("请输入纬度", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        
                        OutlinedTextField(
                            value = watermarkData.longitude.toString(),
                            onValueChange = { value ->
                                value.toDoubleOrNull()?.let { lng ->
                                    onWatermarkDataChange(watermarkData.copy(longitude = lng))
                                }
                            },
                            label = { Text("经度", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { Text("例如：116.407396") },
                            isError = watermarkData.longitude == 0.0,
                            supportingText = {
                                if (watermarkData.longitude == 0.0) {
                                    Text("请输入经度", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 手动选择时间
                    OutlinedTextField(
                        value = formatTimestampForInput(watermarkData.timestamp),
                        onValueChange = { value ->
                            // 这里可以添加时间格式验证
                            // 暂时保持原值，实际使用时需要解析时间字符串
                        },
                        label = { Text("拍摄时间", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        placeholder = { Text("格式：2024-01-20") }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 地点设置
                    Text(
                        text = "地点设置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 自动获取位置按钮
                    Button(
                        onClick = {
                            if (!isGettingLocation) {
                                isGettingLocation = true
                                locationService.startLocationUpdates { location ->
                                    scope.launch {
                                        try {
                                            val locationName = locationService.getAddressFromLocation(
                                                location.latitude,
                                                location.longitude
                                            )
                                            val weatherInfo = weatherService.getWeatherInfo(
                                                location.latitude,
                                                location.longitude
                                            )
                                            
                                            onWatermarkDataChange(watermarkData.copy(
                                                latitude = location.latitude,
                                                longitude = location.longitude,
                                                locationName = locationName,
                                                weather = weatherInfo.weather,
                                                temperature = weatherInfo.temperature,
                                                altitude = location.altitude.toString(),
                                                direction = locationService.getDirection(location.bearing)
                                            ))
                                            
                                            isGettingLocation = false
                                            locationService.stopLocationUpdates()
                                        } catch (e: Exception) {
                                            isGettingLocation = false
                                            locationService.stopLocationUpdates()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isGettingLocation,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (isGettingLocation) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在获取位置...", fontSize = 16.sp)
                            }
                        } else {
                            Text("自动获取当前位置", fontSize = 16.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 手动输入地址
                    OutlinedTextField(
                        value = watermarkData.locationName,
                        onValueChange = { 
                            onWatermarkDataChange(watermarkData.copy(locationName = it))
                        },
                        label = { Text("地点名称", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        placeholder = { Text("例如：北京市朝阳区") },
                        isError = watermarkData.locationName.isEmpty(),
                        supportingText = {
                            if (watermarkData.locationName.isEmpty()) {
                                Text("请输入地点名称", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 预览文件名
                    if (watermarkData.title.isNotEmpty() && (!watermarkData.showImageName || watermarkData.imageName.isNotEmpty())) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "保存文件名",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (watermarkData.showImageName && watermarkData.imageName.isNotEmpty()) {
                                        "${watermarkData.title}-${watermarkData.imageName}-1.jpg"
                                    } else {
                                        "${watermarkData.title}-1.jpg"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 水印显示设置部分已移除
                }
            }
            
            // 操作按钮
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "操作",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackPressed,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("返回", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        
                        Button(
                            onClick = {
                                watermarkedBitmap?.let { bitmap ->
                                    onSaveImage(bitmap, watermarkData)
                                }
                            },
                            enabled = watermarkedBitmap != null && 
                                     !isProcessing && 
                                     watermarkData.title.isNotEmpty() && 
                                     (!watermarkData.showImageName || watermarkData.imageName.isNotEmpty()) && 
                                     watermarkData.latitude != 0.0 && 
                                     watermarkData.longitude != 0.0 && 
                                     watermarkData.locationName.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "保存图片",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // 保存状态提示
                    if (watermarkedBitmap == null && !isProcessing) {
                        Text(
                            text = "请等待水印预览生成完成后再保存",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    } else if (watermarkData.title.isEmpty() || 
                               (watermarkData.showImageName && watermarkData.imageName.isEmpty()) || 
                               watermarkData.latitude == 0.0 || 
                               watermarkData.longitude == 0.0 || 
                               watermarkData.locationName.isEmpty()) {
                        Text(
                            text = "请填写所有必填项后再保存",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    }
                }
            }
            
            // 底部间距
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    
    // 原始图片大图预览对话框
    if (showPreviewDialog) {
        Dialog(
            onDismissRequest = { showPreviewDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val pagerState = rememberPagerState(
                    initialPage = 0
                ) { 2 }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 8.dp
                ) { page ->
                    when (page) {
                        0 -> {
                            // 原始图片
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        top = 80.dp,    // 为顶部工具栏留出空间
                                        bottom = 60.dp  // 为底部指示器留出空间
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                        1 -> {
                            // 水印预览
                            if (watermarkedBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = watermarkedBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = 80.dp,    // 为顶部工具栏留出空间
                                            bottom = 60.dp  // 为底部指示器留出空间
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = 80.dp,
                                            bottom = 60.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无水印预览",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 顶部工具栏（半透明黑色背景）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showPreviewDialog = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = if (pagerState.currentPage == 0) "原始图片" else "水印预览",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 占位，保持对称
                    Box(modifier = Modifier.size(48.dp))
                }

                // 底部页面指示器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(vertical = 12.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) 
                                        Color.White 
                                    else 
                                        Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
    
    // 移除单独的水印预览对话框，因为已经合并到上面的对话框中
}

// 格式化时间戳为输入框显示格式
private fun formatTimestampForInput(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(date)
} 