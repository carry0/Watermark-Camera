@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.lwr.watermarkcamera.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import android.graphics.Paint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.compose.foundation.Canvas
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.CornerRadius
import com.lwr.watermarkcamera.data.WatermarkPosition
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import com.lwr.watermarkcamera.data.TimeFormat
import com.lwr.watermarkcamera.data.WatermarkStyle
import com.lwr.watermarkcamera.utils.LocationService
import com.lwr.watermarkcamera.utils.WeatherService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.painterResource
import coil.size.Size
import com.lwr.watermarkcamera.R

@Composable
fun CameraScreenTest(
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
    var currentZoomRatio by remember { mutableFloatStateOf(1.0f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1.0f) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var currentCameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // 新增：多摄像头支持
    data class CameraInfo(
        val cameraId: String,
        val lensFacing: Int,
        val focalLength: Float,
        val isUltraWide: Boolean = false
    )
    var availableCameras by remember { mutableStateOf<List<CameraInfo>>(emptyList()) }
    var currentCameraIndex by remember { mutableIntStateOf(0) }

    // 获取可用摄像头类型
    LaunchedEffect(Unit) {

    }

    // 更新相机选择器
    LaunchedEffect(currentCameraIndex) {
        if (availableCameras.isNotEmpty() && currentCameraIndex < availableCameras.size) {
            val selectedCamera = availableCameras[currentCameraIndex]
            currentCameraSelector = CameraSelector.Builder()
                .requireLensFacing(selectedCamera.lensFacing)
                .build()

            // 可以在这里添加其他相机配置，比如设置特定的焦距等
            Log.d("CameraSwitch", "切换到摄像头: 焦距=${selectedCamera.focalLength}mm${if (selectedCamera.isUltraWide) ", 超广角" else ""}")
        }
    }

    // 打印相机缩放状态初始值
    LaunchedEffect(Unit) {
        Log.d("CameraZoom", "初始缩放状态: 当前缩放=$currentZoomRatio, 最大缩放=$maxZoomRatio")
    }

    // 监控缩放比例变化
    LaunchedEffect(currentZoomRatio) {
        Log.d("CameraZoom", "缩放比例变化: 当前缩放=$currentZoomRatio, 最大缩放=$maxZoomRatio")
    }

    // 预览状态
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var photoRotationDegrees by remember { mutableFloatStateOf(0f) }  // 保存拍照时的旋转角度

    // 手势调试状态
    var lastGestureZoom by remember { mutableFloatStateOf(1.0f) }
    var showGestureInfo by remember { mutableStateOf(false) }
    var gestureInfoText by remember { mutableStateOf("") }

    // 隐藏手势信息的协程
    val hideGestureInfoJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 方向监听
    var rotation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    DisposableEffect(context) {
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        orientationListener.enable()
        onDispose { orientationListener.disable() }
    }

    // 相机预览引用
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // 监听相机选择器变化
    LaunchedEffect(currentCameraSelector, previewBitmap) {
        if (previewBitmap == null && !showPreviewDialog && previewView != null) {
            // 当相机选择器变化且处于相机预览状态时，重新启动相机
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    Log.d("CameraScreen", "重新绑定相机")

                    // 解绑所有用例
                    cameraProvider.unbindAll()

                    // 创建预览用例
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView!!.surfaceProvider)
                        }

                    // 创建图片捕获用例
                    val newImageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    try {
                        // 绑定用例到生命周期
                        val newCamera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            currentCameraSelector,
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
                            Log.d(
                                "CameraZoom",
                                "缩放状态更新: 当前=${zoomState.zoomRatio}, 最大=${zoomState.maxZoomRatio}, 线性缩放=${zoomState.linearZoom}, 最小缩放=${zoomState.minZoomRatio}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "相机绑定失败: ${e.message}")
                        e.printStackTrace()
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
        val rotationDegrees = 0f
        Log.d("PhotoRotation", "拍照时设备方向: rotation=$rotation, 计算旋转角度=$rotationDegrees")
        onPhotoTaken(bitmap, file, rotationDegrees)
        // 继续留在相机页面，不清空标题，保持所有设置不变
    }

    // 新增：广角摄像头支持
    var currentLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var availableCameraLenses by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 获取可用摄像头类型
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                // 获取所有可用的相机信息
                val availableCameras = cameraProvider.availableCameraInfos

                // 创建一个临时列表来存储可用的镜头类型
                val lenses = mutableListOf<Int>()

                // 遍历并收集所有可用的镜头类型
                availableCameras.forEach { cameraInfo ->
                    cameraInfo.lensFacing.let { lensFacing ->
                        if (!lenses.contains(lensFacing)) {
                            lenses.add(lensFacing)
                        }
                        val lensFacingName = when (lensFacing) {
                            CameraSelector.LENS_FACING_BACK -> "后置"
                            CameraSelector.LENS_FACING_FRONT -> "前置"
                            else -> "特殊($lensFacing)"
                        }
                        Log.d("CameraInfo", "发现镜头类型: $lensFacingName")
                    }
                }

                // 更新可用镜头列表
                availableCameraLenses = lenses
                Log.d("CameraInfo", "可用镜头数量: ${lenses.size}")
            } catch (e: Exception) {
                Log.e("CameraInfo", "获取相机信息失败", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // 更新相机选择器
    LaunchedEffect(currentLensFacing) {
        currentCameraSelector = CameraSelector.Builder()
            .requireLensFacing(currentLensFacing)
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主体布局
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部工具栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }

                // 中间标题
                if (previewBitmap == null && !showPreviewDialog) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                        Text(
                            text = "最大: ${String.format("%.1f", maxZoomRatio)}x",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else if (previewBitmap != null && !showPreviewDialog) {
                    Text(
                        text = "预览照片",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // 右侧按钮
                if (previewBitmap == null && !showPreviewDialog) {
                    IconButton(
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                locationService.startLocationUpdates { location ->
                                    scope.launch {
                                        try {
                                            val locationName =
                                                locationService.getAddressFromLocation(
                                                    location.latitude,
                                                    location.longitude
                                                )
                                            val weatherInfo = weatherService.getWeatherInfo(
                                                location.latitude,
                                                location.longitude
                                            )

                                            onWatermarkDataChange(
                                                watermarkData.copy(
                                                    latitude = location.latitude,
                                                    longitude = location.longitude,
                                                    locationName = locationName,
                                                    weather = weatherInfo.weather,
                                                    temperature = weatherInfo.temperature,
                                                    altitude = location.altitude.toString(),
                                                    direction = locationService.getDirection(
                                                        location.bearing
                                                    )
                                                )
                                            )
                                        } finally {
                                            isLoading = false
                                            locationService.stopLocationUpdates()
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
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
                } else {
                    // 占位，保持布局对称
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            // 中间相机预览区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (previewBitmap != null && !showPreviewDialog) {
                    // 预览图片和水印显示代码
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 显示图片
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else if (!showPreviewDialog) {
                    // 相机预览
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
                                        val currentZoom =
                                            cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                                        val maxZoom =
                                            cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8f
                                        val newZoom = (currentZoom * zoom).coerceIn(1f, maxZoom)
                                        Log.d(
                                            "CameraZoom",
                                            "手势缩放: 原始值=$currentZoom, 手势缩放=$zoom, 新值=$newZoom, 最大=$maxZoom"
                                        )

                                        // 更新手势调试信息
                                        lastGestureZoom = zoom
                                        showGestureInfo = true
                                        gestureInfoText =
                                            "缩放比例: ${String.format("%.2f", zoom)}\n" +
                                                    "当前值: ${
                                                        String.format(
                                                            "%.1f",
                                                            currentZoom
                                                        )
                                                    }x → ${String.format("%.1f", newZoom)}x"

                                        // 启动延时隐藏任务
                                        hideGestureInfoJob.value?.cancel()
                                        hideGestureInfoJob.value = scope.launch {
                                            kotlinx.coroutines.delay(2000) // 2秒后隐藏信息
                                            showGestureInfo = false
                                        }

                                        cam.cameraControl.setZoomRatio(newZoom)
                                    }
                                }
                            },
                        update = { view ->
                            // 保存PreviewView引用
                            previewView = view

                            // 初始相机设置会在LaunchedEffect中处理
                            if (camera == null) {
                                try {
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
                                            Log.d(
                                                "CameraZoom",
                                                "缩放状态更新: 当前=${zoomState.zoomRatio}, 最大=${zoomState.maxZoomRatio}"
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CameraScreen", "初始相机绑定失败: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        }
                    )

                    // 相机预览时的水印显示
                    val rotationDegrees = when (rotation) {
                        Surface.ROTATION_0 -> 0f
                        Surface.ROTATION_90 -> 90f
                        Surface.ROTATION_180 -> 180f
                        Surface.ROTATION_270 -> 270f
                        else -> 0f
                    }

                    Log.d("WatermarkPosition", "Surface.ROTATION值: $rotation")

                    // 根据旋转角度调整水印位置
                    val adjustedPosition = when (rotation) {
                        Surface.ROTATION_0 -> watermarkData.watermarkPosition  // 不旋转时保持原始位置
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

                    Log.d(
                        "WatermarkPosition",
                        "原始位置: ${watermarkData.watermarkPosition}, 旋转角度: $rotationDegrees, 调整后位置: $adjustedPosition"
                    )

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
                                .fillMaxWidth(0.5f)  // 减少宽度比例，避免超出屏幕
                                .padding(
                                    start = when (adjustedPosition) {
                                        WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> 20.dp
                                        else -> 8.dp
                                    },
                                    end = when (adjustedPosition) {
                                        WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> 20.dp
                                        else -> 8.dp
                                    },
                                    top = when (adjustedPosition) {
                                        WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> 20.dp  // 避免被顶部工具栏遮挡
                                        else -> 8.dp
                                    },
                                    bottom = when (adjustedPosition) {
                                        WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> 20.dp  // 避免被底部按钮遮挡
                                        else -> 8.dp
                                    }
                                )
                                .graphicsLayer {
                                    rotationZ = rotationDegrees
                                    Log.d("WatermarkTransform", "水印旋转角度: $rotationDegrees")
                                }
                                .also {
                                    Log.d(
                                        "WatermarkModifier",
                                        "水印位置: $adjustedPosition, 对齐方式: ${
                                            when (adjustedPosition) {
                                                WatermarkPosition.TOP_LEFT -> "TopStart"
                                                WatermarkPosition.TOP_RIGHT -> "TopEnd"
                                                WatermarkPosition.BOTTOM_LEFT -> "BottomStart"
                                                WatermarkPosition.BOTTOM_RIGHT -> "BottomEnd"
                                            }
                                        }"
                                    )
                                }
                        )
                    }
                }
            }

            // 底部控制栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 20.dp)
            ) {
                if (previewBitmap != null && !showPreviewDialog) {
                    // 预览时的底部按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 重拍按钮
                        FloatingActionButton(
                            onClick = {
                                Log.d("CameraScreen", "点击重拍按钮")
                                previewBitmap = null
                                previewFile?.delete()
                                previewFile = null
                                photoRotationDegrees = 0f

                                // 确保相机预览视图已经准备好
                                previewView?.let { view ->
                                    // 重新启动相机
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
                                            Log.d(
                                                "CameraZoom",
                                                "缩放状态更新: 当前=${zoomState.zoomRatio}, 最大=${zoomState.maxZoomRatio}"
                                            )
                                        }
                                    }
                                }
                            },
                            containerColor = Color.Red,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "重拍",
                                tint = Color.White
                            )
                        }

                        // 保存按钮
                        FloatingActionButton(
                            onClick = {
                                previewBitmap?.let { bitmap ->
                                    previewFile?.let { file ->
                                        handlePhotoTaken(bitmap, file)
                                        previewBitmap = null
                                        previewFile = null
                                        photoRotationDegrees = 0f
                                    }
                                }
                            },
                            containerColor = Color.Green,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "保存",
                                tint = Color.White
                            )
                        }
                    }
                } else if (!showPreviewDialog) {
                    // 相机操作按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 新增：广角切换按钮
                        if (availableCameras.size > 1) {
                            IconButton(
                                onClick = {
                                    // 循环切换可用摄像头
                                    currentCameraIndex = (currentCameraIndex + 1) % availableCameras.size
                                    val selectedCamera = availableCameras[currentCameraIndex]
                                    Log.d("CameraLens", "切换到摄像头: 焦距=${selectedCamera.focalLength}mm${if (selectedCamera.isUltraWide) ", 超广角" else ""}")
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_wide_angle),
                                    contentDescription = "切换镜头",
                                    tint = Color.White
                                )
                            }
                        } else {
                            // 占位，保持布局对称
                            Spacer(modifier = Modifier.size(48.dp))
                        }

                        // 拍照按钮
                        FloatingActionButton(
                            onClick = {
                                Log.d("CameraScreen", "rotation : $rotation")
                                photoRotationDegrees = when (rotation) {
                                    Surface.ROTATION_0 -> 0f
                                    Surface.ROTATION_90 -> 270f
                                    Surface.ROTATION_180 -> 180f
                                    Surface.ROTATION_270 -> 90f
                                    else -> 0f
                                }

                                takePhoto(
                                    imageCapture = imageCapture,
                                    outputDirectory = getOutputDirectory(context),
                                    executor = cameraExecutor,
                                    rotationDegrees = photoRotationDegrees
                                ) { bitmap, file ->
                                    previewBitmap = bitmap
                                    previewFile = file
                                }
                            },
                            modifier = Modifier.size(72.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_camera),
                                contentDescription = "拍照",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 切换相机按钮
                        IconButton(
                            onClick = {
                                isFrontCamera = !isFrontCamera
                                currentCameraSelector = if (isFrontCamera) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_flip_camera),
                                contentDescription = "切换相机",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // 手势调试信息 - 在用户缩放时显示
    AnimatedVisibility(
        visible = showGestureInfo,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 160.dp),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = gestureInfoText,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(12.dp)
            )
        }
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
        try {
            val cameraProvider = cameraProviderFuture.get()

            // 解绑所有现有用例
            cameraProvider.unbindAll()

            // 创建预览用例
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // 创建图片捕获用例
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                // 绑定用例到生命周期
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                onCameraReady(imageCapture, camera)
            } catch (e: Exception) {
                Log.e("CameraScreen", "相机绑定失败: ${e.message}")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e("CameraScreen", "相机启动失败: ${e.message}")
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

    Log.d("BitmapRotation", "EXIF方向=$orientation, EXIF旋转=$exifRotation, 设备旋转=$rotationDegrees, 总旋转角度=$totalRotation")

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

        Log.d("TakePhoto", "准备拍照: 旋转角度=$rotationDegrees")

        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("TakePhoto", "照片已保存: ${outputFileResults.savedUri}")
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    Log.d("TakePhoto", "原始图片尺寸: ${bitmap?.width}x${bitmap?.height}")

                    val rotatedBitmap =
                        if (bitmap != null) rotateBitmapIfRequired(photoFile, bitmap, rotationDegrees) else null
                    Log.d("TakePhoto", "旋转后图片尺寸: ${rotatedBitmap?.width}x${rotatedBitmap?.height}")

                    if (rotatedBitmap != null) {
                        onPhotoTaken(rotatedBitmap, photoFile)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("TakePhoto", "拍照失败", exception)
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







