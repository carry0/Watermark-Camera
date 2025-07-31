@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.lwr.watermarkcamera.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.lwr.watermarkcamera.data.WatermarkData
import android.media.ExifInterface
import android.graphics.Matrix
import android.util.Log
import androidx.compose.foundation.Canvas
import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.CornerRadius
import com.lwr.watermarkcamera.data.WatermarkPosition
import androidx.compose.ui.graphics.Brush
import com.lwr.watermarkcamera.data.TimeFormat
import com.lwr.watermarkcamera.data.WatermarkStyle
import com.lwr.watermarkcamera.utils.LocationService
import com.lwr.watermarkcamera.utils.WeatherService
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    watermarkData: WatermarkData,
    onPhotoTaken: (Bitmap, File, Float) -> Unit,  // 添加旋转角度参数
    onBackPressed: () -> Unit,
    onWatermarkDataChange: (WatermarkData) -> Unit,
    onBackToSettings: () -> Unit,
    locationService: LocationService,
    weatherService: WeatherService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // 相机控制相关状态
    var currentZoomRatio by remember { mutableStateOf(1.0f) }
    var maxZoomRatio by remember { mutableStateOf(1.0f) }
    var hasUltraWideCamera by remember { mutableStateOf(false) }
    var isUltraWideLens by remember { mutableStateOf(false) }
    var currentCameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var availableCameras by remember { mutableStateOf<List<Pair<CameraSelector, String>>>(listOf()) }
    var currentCameraIndex by remember { mutableStateOf(0) }

    // 预览状态
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var photoRotationDegrees by remember { mutableStateOf(0f) }  // 保存拍照时的旋转角度

    // 方向监听
    var rotation by remember { mutableStateOf(Surface.ROTATION_0) }
    DisposableEffect(context) {
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                rotation = when {
                    orientation in 45..134 -> Surface.ROTATION_270
                    orientation in 135..224 -> Surface.ROTATION_180
                    orientation in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        orientationListener.enable()
        onDispose { orientationListener.disable() }
    }

    // 检测设备是否有广角镜头
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            // 获取所有可用相机
            val cameraInfoList = cameraProvider.availableCameraInfos
            Log.d("CameraScreen", "检测到 ${cameraInfoList.size} 个相机")
            
            // 创建可用的相机选择器列表
            val cameraSelectors = mutableListOf<Pair<CameraSelector, String>>()
            
            // 遍历所有相机并获取它们的ID
            cameraInfoList.forEachIndexed { index, cameraInfo ->
                // 获取相机ID
                val cameraId = cameraInfo.toString()
                Log.d("CameraScreen", "相机 $index ID: $cameraId")
                
                // 尝试确定相机类型
                val isBackCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA
                ).cameraInfo == cameraInfo
                
                // 创建相机选择器
                val selector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    // 尝试创建一个自定义选择器
                    try {
                        CameraSelector.Builder()
                            .addCameraFilter { cameras ->
                                cameras.filter { camera ->
                                    camera.toString() == cameraId
                                }
                            }
                            .build()
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "创建相机选择器失败: ${e.message}")
                        null
                    }
                }
                
                // 添加到列表
                if (selector != null) {
                    val cameraName = if (index == 0) "主摄像头" else "辅助摄像头 $index"
                    cameraSelectors.add(Pair(selector, cameraName))
                }
            }
            
            // 确保至少有一个相机
            if (cameraSelectors.isEmpty()) {
                cameraSelectors.add(Pair(CameraSelector.DEFAULT_BACK_CAMERA, "默认相机"))
            }
            
            // 更新可用相机列表和广角相机状态
            availableCameras = cameraSelectors
            hasUltraWideCamera = cameraSelectors.size > 1
            Log.d("CameraScreen", "检测到 ${cameraSelectors.size} 个可用相机，hasUltraWideCamera=$hasUltraWideCamera")
            
            // 解绑相机
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("CameraScreen", "检测广角相机失败", e)
            e.printStackTrace()
        }
    }

    // 相机预览引用
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // 监听相机选择器变化
    LaunchedEffect(currentCameraSelector) {
        if (previewBitmap == null && !showPreviewDialog && previewView != null) {
            // 当相机选择器变化且处于相机预览状态时，重新启动相机
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    Log.d("CameraScreen", "重新绑定相机，当前索引: $currentCameraIndex, 是否广角: $isUltraWideLens")
                    
                    // 解绑所有用例
                    cameraProvider.unbindAll()
                    
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView!!.surfaceProvider)
                    
                    val newImageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    
                    // 使用当前选择的相机选择器
                    val selector = currentCameraSelector
                    Log.d("CameraScreen", "使用相机选择器: $selector")
                    
                    val newCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        newImageCapture
                    )
                    
                    // 更新外部变量
                    imageCapture = newImageCapture
                    camera = newCamera
                    
                    // 监听缩放状态变化
                    newCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                        currentZoomRatio = zoomState.zoomRatio
                        maxZoomRatio = zoomState.maxZoomRatio
                        Log.d("CameraScreen", "缩放状态更新: 当前=${currentZoomRatio}, 最大=${maxZoomRatio}")
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "重新绑定相机失败", e)
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // 处理拍照结果
    val handlePhotoTaken = { bitmap: Bitmap, file: File ->
        val rotationDegrees = when(rotation) {
            Surface.ROTATION_0 -> 0f
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> 270f
            else -> 0f
        }
        onPhotoTaken(bitmap, file, rotationDegrees)
        // 继续留在相机页面，不清空标题，保持所有设置不变
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览或预览图片 - 根据状态切换显示
        if (previewBitmap != null && !showPreviewDialog) {
            // 显示预览图片
            Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else if (!showPreviewDialog) {
            // 显示相机预览
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            camera?.let { cam ->
                                val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                                val newZoom = (currentZoom * zoom).coerceIn(1f, cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8f)
                                cam.cameraControl.setZoomRatio(newZoom)
                            }
                        }
                    },
                update = { view ->
                    // 保存PreviewView引用
                    previewView = view
                    
                    // 初始相机设置会在LaunchedEffect中处理
                    if (camera == null) {
                        startCamera(
                            context,
                            view,
                            lifecycleOwner,
                            cameraExecutor,
                            currentCameraSelector
                        ) { capture, cam ->
                            imageCapture = capture
                            camera = cam
                            
                            // 监听缩放状态变化
                            cam.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                                currentZoomRatio = zoomState.zoomRatio
                                maxZoomRatio = zoomState.maxZoomRatio
                            }
                        }
                    }
                }
            )
        }

        // 水印预览 - 只在相机预览时显示
        if (previewBitmap == null && !showPreviewDialog) {
            val rotationDegrees = when(rotation) {
                Surface.ROTATION_0 -> 0f
                Surface.ROTATION_90 -> 90f
                Surface.ROTATION_180 -> 180f
                Surface.ROTATION_270 -> 270f
                else -> 0f
            }
            
            // 根据旋转角度调整水印位置
            val adjustedPosition = when (rotation) {
                Surface.ROTATION_0 -> watermarkData.watermarkPosition
                Surface.ROTATION_90 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_LEFT
                }
                Surface.ROTATION_180 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_RIGHT
                }
                Surface.ROTATION_270 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                }
                else -> watermarkData.watermarkPosition
            }
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = when (adjustedPosition) {
                    WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                    WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                    WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                }
            ) {
                WatermarkPreview(
                    watermarkData = watermarkData,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)  // 减少宽度比例，避免超出屏幕
                        .padding(
                            start = when (adjustedPosition) {
                                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> 16.dp
                                else -> 8.dp
                            },
                            end = when (adjustedPosition) {
                                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> 16.dp
                                else -> 8.dp
                            },
                            top = when (adjustedPosition) {
                                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> 80.dp  // 避免被顶部工具栏遮挡
                                else -> 8.dp
                            },
                            bottom = when (adjustedPosition) {
                                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> 140.dp  // 避免被底部按钮遮挡
                                else -> 8.dp
                            }
                        )
                        .graphicsLayer { rotationZ = rotationDegrees }
                )
            }
        }

        // 顶部工具栏 - 始终显示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            // 中间部分 - 显示当前缩放倍数（仅在相机预览时显示）
            if (previewBitmap == null && !showPreviewDialog) {
                Text(
                    text = "${currentZoomRatio.roundToInt()}x",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else if (previewBitmap != null && !showPreviewDialog) {
                // 预览图片时显示标题
                Text(
                    text = "预览照片",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                // 预览对话框时显示占位符
                Box(modifier = Modifier.size(48.dp))
            }

            // 右侧按钮 - 根据状态显示不同的按钮
            if (previewBitmap == null && !showPreviewDialog) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 广角切换按钮 - 仅在设备支持广角镜头时显示
                    if (hasUltraWideCamera) {
                        Button(
                            onClick = {
                                // 切换到下一个可用相机
                                if (availableCameras.isNotEmpty()) {
                                    currentCameraIndex = (currentCameraIndex + 1) % availableCameras.size
                                    isUltraWideLens = currentCameraIndex != 0
                                    currentCameraSelector = availableCameras[currentCameraIndex].first
                                    Log.d("CameraScreen", "切换到相机 ${availableCameras[currentCameraIndex].second}, isUltraWideLens: $isUltraWideLens")
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Text(
                                text = if (availableCameras.isNotEmpty()) {
                                    val nextIndex = (currentCameraIndex + 1) % availableCameras.size
                                    "切换到${availableCameras[nextIndex].second}"
                                } else {
                                    "切换相机"
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // 刷新位置和天气按钮
                    IconButton(
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
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
                                        } finally {
                                            isLoading = false
                                            locationService.stopLocationUpdates()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新位置和天气",
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                // 预览对话框时显示占位符
                Box(modifier = Modifier.size(48.dp))
            }
        }

        // 底部按钮区域 - 根据状态显示不同按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (previewBitmap != null && !showPreviewDialog) {
                // 预览图片时显示重拍和保存按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // 重拍按钮
                    Button(
                        onClick = {
                            previewBitmap = null
                            previewFile?.delete()
                            previewFile = null
                            photoRotationDegrees = 0f  // 重置旋转角度
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "重拍",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "重拍",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // 保存按钮
                    Button(
                        onClick = {
                            previewBitmap?.let { bitmap ->
                                previewFile?.let { file ->
                                    handlePhotoTaken(bitmap, file)
                                    // 重置预览状态，准备继续拍照
                                    previewBitmap = null
                                    previewFile = null
                                    photoRotationDegrees = 0f  // 重置旋转角度
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "保存",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "保存",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (!showPreviewDialog) {
                // 相机预览时显示拍照按钮
                FloatingActionButton(
                    onClick = {
                        Log.d("CameraScreen","rotation : $rotation" )
                        // 保存拍照时的旋转角度
                        photoRotationDegrees = when(rotation) {
                            Surface.ROTATION_0 -> 0f
                            Surface.ROTATION_90 -> 90f
                            Surface.ROTATION_180 -> 180f
                            Surface.ROTATION_270 -> 270f
                            else -> 0f
                        }
                        
                        takePhoto(
                            imageCapture = imageCapture,
                            outputDirectory = getOutputDirectory(context),
                            executor = cameraExecutor,
                            rotationDegrees = photoRotationDegrees  // 使用保存的旋转角度
                        ) { bitmap, file ->
                            previewBitmap = bitmap
                            previewFile = file
                            // 直接显示预览图片，不需要showPreviewDialog
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "拍照",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 移除原来的预览对话框，预览功能已集成到主界面中
        // 保留showPreviewDialog状态用于其他逻辑控制
    }
}

private fun startCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    cameraSelector: CameraSelector,
    onCameraReady: (ImageCapture, Camera) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            onCameraReady(imageCapture, camera)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun rotateBitmapIfRequired(file: File, bitmap: Bitmap, rotationDegrees: Float): Bitmap {
    val exif = ExifInterface(file.absolutePath)
    val orientation =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    
    // 计算需要旋转的角度
    val exifRotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    
    // 结合EXIF方向和屏幕旋转角度
    val totalRotation = (exifRotation + rotationDegrees) % 360f
    
    // 如果不需要旋转，直接返回原图
    if (totalRotation == 0f) {
        return bitmap
    }
    
    val matrix = Matrix()
    matrix.postRotate(totalRotation)
    
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    outputDirectory: File,
    executor: ExecutorService,
    rotationDegrees: Float,
    onPhotoTaken: (Bitmap, File) -> Unit
) {
    imageCapture?.let { capture ->
        val photoFile = File(
            outputDirectory,
            "temp_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                    val rotatedBitmap =
                        if (bitmap != null) rotateBitmapIfRequired(photoFile, bitmap, rotationDegrees) else null
                    if (rotatedBitmap != null) {
                        onPhotoTaken(rotatedBitmap, photoFile)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }
}

private fun getOutputDirectory(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, "WatermarkCamera").apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}

@Composable
fun WatermarkPreview(
    watermarkData: WatermarkData,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val paddingDp = 8.dp  // 减少内边距
    val lineSpacingDp = 3.dp  // 减少行间距
    val textSizeDp = 16.dp  // 减少文字大小

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val paddingPx = with(density) { paddingDp.toPx() }
        val lineSpacingPx = with(density) { lineSpacingDp.toPx() }
        val textSizePx = with(density) { textSizeDp.toPx() }
        val contentList = mutableListOf<String>()

        if (watermarkData.title.isNotEmpty()) {
            contentList.add(watermarkData.title)
        }
        
        if (watermarkData.showTime && watermarkData.showDate) {
            contentList.add("时间: ${formatTimestamp(watermarkData.timestamp, watermarkData.timeFormat)}")
        }
        
        // 经纬度信息（根据showCoordinates控制）
        if (watermarkData.showCoordinates && watermarkData.latitude != 0.0 && watermarkData.longitude != 0.0) {
            contentList.add("经纬度: ${String.format("%.6f°N,%.6f°E", watermarkData.latitude, watermarkData.longitude)}")
        }
        
        // 地点信息（根据showLocation控制）
        if (watermarkData.showLocation && watermarkData.locationName.isNotEmpty()) {
            contentList.add("地点: ${watermarkData.locationName}")
        }
        
        if (watermarkData.showWeatherInfo) {
            val weatherInfo = buildString {
                append("天气: ")
                if (watermarkData.weather.isNotEmpty()) {
                    append(watermarkData.weather)
                }
                if (watermarkData.temperature.isNotEmpty()) {
                    if (watermarkData.weather.isNotEmpty()) append(" ")
                    append(watermarkData.temperature)
                }
            }
            if (weatherInfo.length > "天气: ".length) {
                contentList.add(weatherInfo)
            }
        }

        val paint = android.text.TextPaint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            textSize = textSizePx
            
            when (watermarkData.watermarkStyle) {
                WatermarkStyle.SIMPLE -> {
                    setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)  // 减少阴影效果
                }
                WatermarkStyle.BORDERED -> {
                    strokeWidth = 1.5f  // 减少描边宽度
                    style = android.graphics.Paint.Style.STROKE
                    color = android.graphics.Color.BLACK
                }
                else -> {
                    // CARD 和 GRADIENT 样式不需要特殊的文字效果
                }
            }
        }

        var totalHeight = 0f
        val layoutList = contentList.map { text ->
            val availableWidth = (maxWidthPx - 2 * paddingPx).toInt()
            val staticLayout = android.text.StaticLayout.Builder
                .obtain(text, 0, text.length, paint, availableWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            totalHeight += staticLayout.height + lineSpacingPx
            staticLayout
        }
        totalHeight += 2 * paddingPx

        // 确保水印高度不超过可用高度
        val finalHeight = minOf(totalHeight, maxHeightPx)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { finalHeight.toDp() })
        ) {
            // 绘制背景
            when (watermarkData.watermarkStyle) {
                WatermarkStyle.CARD -> {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        cornerRadius = CornerRadius(12f, 12f),  // 减少圆角半径
                        size = size
                    )
                }
                WatermarkStyle.GRADIENT -> {
                    val gradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                    drawRect(brush = gradient, size = size)
                }
                else -> {
                    drawRect(color = Color.Transparent, size = size)
                }
            }

            var currentY = paddingPx
            layoutList.forEach { layout ->
                // 检查是否还有足够空间绘制
                if (currentY + layout.height <= finalHeight - paddingPx) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.translate(paddingPx, currentY)
                        
                        // 如果是描边样式，需要额外绘制一层填充文字
                        if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                            val fillPaint = android.text.TextPaint().apply {
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                                textSize = textSizePx
                                style = android.graphics.Paint.Style.FILL
                            }
                            val fillLayout = android.text.StaticLayout.Builder
                                .obtain(layout.text, 0, layout.text.length, fillPaint, layout.width)
                                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(0f, 1f)
                                .setIncludePad(false)
                                .build()
                            fillLayout.draw(canvas.nativeCanvas)
                        }
                        
                        layout.draw(canvas.nativeCanvas)
                        canvas.nativeCanvas.restore()
                    }
                    currentY += layout.height + lineSpacingPx
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long, timeFormat: TimeFormat): String {
    val pattern = when (timeFormat) {
        TimeFormat.DATE_ONLY -> "yyyy-MM-dd"
        TimeFormat.DATE_TIME -> "yyyy-MM-dd HH:mm:ss"
    }
    val dateFormat = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
} 