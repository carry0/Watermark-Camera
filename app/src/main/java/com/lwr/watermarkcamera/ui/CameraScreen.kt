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
    onPhotoTaken: (Bitmap, File) -> Unit,
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
    
    // 预览状态
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewFile by remember { mutableStateOf<File?>(null) }

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

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // 处理拍照结果
    val handlePhotoTaken = { bitmap: Bitmap, file: File ->
        onPhotoTaken(bitmap, file)
        // 返回到设置页面重新输入标题
        onBackToSettings()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览
        var scale by remember { mutableStateOf(1f) }
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
            update = { previewView ->
                startCamera(
                    context,
                    previewView,
                    lifecycleOwner,
                    cameraExecutor
                ) { capture, cam ->
                    imageCapture = capture
                    camera = cam
                }
            }
        )

        // 水印预览 - 显示在相机预览上
        val rotationDegrees = when(rotation) {
            Surface.ROTATION_0 -> 0f
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> 270f
            else -> 0f
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = when (watermarkData.watermarkPosition) {
                WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
            }
        ) {
            WatermarkPreview(
                watermarkData = watermarkData,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = when (watermarkData.watermarkPosition) {
                            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> 16.dp
                            else -> 16.dp
                        },
                        bottom = when (watermarkData.watermarkPosition) {
                            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> 120.dp
                            else -> 16.dp
                        }
                    )
                    .graphicsLayer { rotationZ = rotationDegrees }
            )
        }

        // 顶部工具栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            // 刷新按钮
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

        // 底部拍照按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            FloatingActionButton(
                onClick = {
                    takePhoto(
                        imageCapture = imageCapture,
                        outputDirectory = getOutputDirectory(context),
                        executor = cameraExecutor
                    ) { bitmap, file ->
                        previewBitmap = bitmap
                        previewFile = file
                        showPreviewDialog = true
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

        // 预览对话框
        if (showPreviewDialog && previewBitmap != null && previewFile != null) {
            Dialog(
                onDismissRequest = { 
                    showPreviewDialog = false
                    previewBitmap = null
                    previewFile?.delete()
                    previewFile = null
                }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "预览照片",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(previewBitmap!!.width.toFloat() / previewBitmap!!.height)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    showPreviewDialog = false
                                    previewBitmap = null
                                    previewFile?.delete()
                                    previewFile = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "取消",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("重拍")
                            }
                            
                            Button(
                                onClick = {
                                    previewBitmap?.let { bitmap ->
                                        previewFile?.let { file ->
                                            handlePhotoTaken(bitmap, file)
                                            // 重置预览状态，准备继续拍照
                                            showPreviewDialog = false
                                            previewBitmap = null
                                            previewFile = null
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Green
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "确认",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
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
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )

            onCameraReady(imageCapture, camera)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun rotateBitmapIfRequired(file: File, bitmap: Bitmap): Bitmap {
    val exif = ExifInterface(file.absolutePath)
    val orientation =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    outputDirectory: File,
    executor: ExecutorService,
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
                        if (bitmap != null) rotateBitmapIfRequired(photoFile, bitmap) else null
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
    val paddingDp = 10.dp
    val lineSpacingDp = 4.dp
    val textSizeDp = 19.dp

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val paddingPx = with(density) { paddingDp.toPx() }
        val lineSpacingPx = with(density) { lineSpacingDp.toPx() }
        val textSizePx = with(density) { textSizeDp.toPx() }
        val contentList = mutableListOf<String>()

        if (watermarkData.title.isNotEmpty()) {
            contentList.add(watermarkData.title)
        }
        contentList.add("时间: ${formatTimestamp(watermarkData.timestamp, watermarkData.timeFormat)}")
        if (watermarkData.latitude != 0.0 && watermarkData.longitude != 0.0) {
            contentList.add("经纬度: ${String.format("%.6f°N,%.6f°E", watermarkData.latitude, watermarkData.longitude)}")
            if (watermarkData.locationName.isNotEmpty()) {
                contentList.add("地点: ${watermarkData.locationName}")
            }
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
                    setShadowLayer(5f, 2f, 2f, android.graphics.Color.BLACK)
                }
                WatermarkStyle.BORDERED -> {
                    strokeWidth = 2f
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
            val staticLayout = android.text.StaticLayout.Builder
                .obtain(text, 0, text.length, paint, (maxWidthPx - 2 * paddingPx).toInt())
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            totalHeight += staticLayout.height + lineSpacingPx
            staticLayout
        }
        totalHeight += 2 * paddingPx

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { totalHeight.toDp() })
        ) {
            // 绘制背景
            when (watermarkData.watermarkStyle) {
                WatermarkStyle.CARD -> {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        cornerRadius = CornerRadius(16f, 16f),
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

private fun formatTimestamp(timestamp: Long, timeFormat: TimeFormat): String {
    val pattern = when (timeFormat) {
        TimeFormat.DATE_ONLY -> "yyyy-MM-dd"
        TimeFormat.DATE_TIME -> "yyyy-MM-dd HH:mm:ss"
    }
    val dateFormat = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
} 