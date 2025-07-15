package com.lwr.watermarkcamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lwr.watermarkcamera.data.WatermarkData
import com.lwr.watermarkcamera.ui.CameraScreen
import com.lwr.watermarkcamera.ui.FolderSelectionScreen
import com.lwr.watermarkcamera.ui.WatermarkSettingsScreen
import com.lwr.watermarkcamera.ui.ImageSelectionScreen
import com.lwr.watermarkcamera.ui.ImagePreviewScreen
import com.lwr.watermarkcamera.ui.theme.WatermarkCameraTheme
import com.lwr.watermarkcamera.utils.*
import kotlinx.coroutines.launch
import java.io.File
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.lwr.watermarkcamera.data.TimeFormat
import com.lwr.watermarkcamera.data.WatermarkPosition
import com.lwr.watermarkcamera.data.WatermarkStyle
import androidx.core.content.edit
import com.lwr.watermarkcamera.data.AppDatabase
import com.lwr.watermarkcamera.data.ProjectFolder
import kotlinx.coroutines.flow.map
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height

class MainActivity : ComponentActivity() {
    
    private lateinit var folderManager: FolderManager
    private lateinit var locationService: LocationService
    private lateinit var weatherService: WeatherService
    private lateinit var deviceInfoUtil: DeviceInfoUtil
    private lateinit var watermarkGenerator: WatermarkGenerator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: AppDatabase
    private val gson = Gson()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 权限已授予，可以继续操作
        } else {
            Toast.makeText(this, "需要所有权限才能正常使用应用", Toast.LENGTH_LONG).show()
        }
    }
    
    // 图片选择结果处理
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            // 将选择的图片URI传递给Composable
            // 这里需要通过状态管理来处理
        }
    }
    
    private fun getDatabaseInfo() {
        val dbFile = getDatabasePath("watermark_camera_db")
        val dbVersion = database.openHelper.readableDatabase.version
        val dbPath = dbFile.absolutePath
        
        Log.d("DatabaseInfo", "Database Version: $dbVersion")
        Log.d("DatabaseInfo", "Database Path: $dbPath")
        
        Toast.makeText(
            this,
            "数据库版本: $dbVersion\n数据库路径: $dbPath",
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化工具类
        folderManager = FolderManager(this)
        locationService = LocationService(this)
        weatherService = WeatherService()
        deviceInfoUtil = DeviceInfoUtil(this)
        watermarkGenerator = WatermarkGenerator()
        sharedPreferences = getSharedPreferences("watermark_settings", Context.MODE_PRIVATE)
        database = AppDatabase.getDatabase(this)
        
        // 显示数据库信息
//        getDatabaseInfo()
        
        // 请求权限
        requestPermissions()
        
        setContent {
            WatermarkCameraTheme {
                WatermarkCameraApp(
                    folderManager = folderManager,
                    locationService = locationService,
                    weatherService = weatherService,
                    deviceInfoUtil = deviceInfoUtil,
                    watermarkGenerator = watermarkGenerator,
                    sharedPreferences = sharedPreferences,
                    gson = gson,
                    database = database
                )
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = PermissionManager.getMissingPermissions(this)
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationService.stopLocationUpdates()
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun WatermarkCameraApp(
    folderManager: FolderManager,
    locationService: LocationService,
    weatherService: WeatherService,
    deviceInfoUtil: DeviceInfoUtil,
    watermarkGenerator: WatermarkGenerator,
    sharedPreferences: SharedPreferences,
    gson: Gson,
    database: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 试用期相关逻辑
    val TRIAL_PERIOD_MILLIS = 3 * 24 * 60 * 60 * 1000L // 3天
    val TRIAL_START_KEY = "trial_start_time"
    val trialStartTime = remember {
        val saved = sharedPreferences.getLong(TRIAL_START_KEY, 0L)
        if (saved == 0L) {
            val now = System.currentTimeMillis()
            sharedPreferences.edit { putLong(TRIAL_START_KEY, now) }
            now
        } else saved
    }
    var nowTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val trialEndTime = trialStartTime + TRIAL_PERIOD_MILLIS
    val remainingMillis = (trialEndTime - nowTime).coerceAtLeast(0L)
    val isTrialExpired = remainingMillis <= 0L

    // 倒计时刷新
    LaunchedEffect(Unit) {
        while (true) {
            nowTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    // 弹窗
    if (isTrialExpired) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            androidx.compose.material3.Card(
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("免费试用已到期", fontSize = 22.sp, color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    Text("感谢您的体验，如需继续使用请联系客服或购买正式版。", fontSize = 16.sp)
                }
            }
        }
    }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.FolderSelection) }
    var selectedFolder by remember { mutableStateOf<File?>(null) }
    var watermarkData by remember { mutableStateOf(WatermarkData()) }
    var existingFolders by remember { mutableStateOf<List<File>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 系统返回键处理
    fun handleBackPress() {
        when (currentScreen) {
            Screen.FolderSelection -> {
                // 在文件夹选择页面，退出应用
                android.os.Process.killProcess(android.os.Process.myPid())
            }
            Screen.WatermarkSettings -> {
                currentScreen = Screen.FolderSelection
            }
            Screen.Camera -> {
                currentScreen = Screen.WatermarkSettings
            }
            Screen.ImageSelection -> {
                currentScreen = Screen.WatermarkSettings
            }
            Screen.ImagePreview -> {
                currentScreen = Screen.ImageSelection
            }
        }
    }
    
    // 注册系统返回键回调
    BackHandler {
        handleBackPress()
    }
    
    // 获取文件夹列表
    val projectFolders = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            database.projectFolderDao().getAllFolders()
        } else {
            database.projectFolderDao().searchFolders(searchQuery)
        }
    }
    
    // 加载现有文件夹并同步到数据库
    LaunchedEffect(Unit) {
        existingFolders = folderManager.getAllProjectFolders()
        // 同步文件夹到数据库
        existingFolders.forEach { folder ->
            val existingFolder = database.projectFolderDao().getFolderByName(folder.name)
            if (existingFolder == null) {
                // 从SharedPreferences获取标题
                val settingsJson = sharedPreferences.getString(folder.name, null)
                val title = if (settingsJson != null) {
                    gson.fromJson(settingsJson, WatermarkSettings::class.java).title
                } else null
                
                database.projectFolderDao().insertFolder(
                    ProjectFolder(
                        name = folder.name,
                        title = title
                    )
                )
            }
        }
    }
    
    // 更新设备信息
    LaunchedEffect(Unit) {
        watermarkData = watermarkData.copy(deviceInfo = deviceInfoUtil.getDeviceInfo())
    }

    // 保存水印设置到 SharedPreferences
    fun saveWatermarkSettings(folderName: String, data: WatermarkData) {
        val settings = WatermarkSettings(
            title = data.title,
            imageName = data.imageName,
            watermarkStyle = data.watermarkStyle,
            watermarkPosition = data.watermarkPosition,
            timeFormat = data.timeFormat,
            showWeatherInfo = data.showWeatherInfo
        )
        sharedPreferences.edit { putString(folderName, gson.toJson(settings)) }
    }

    // 从 SharedPreferences 加载水印设置
    fun loadWatermarkSettings(folderName: String): WatermarkData {
        val settingsJson = sharedPreferences.getString(folderName, null)
        return if (settingsJson != null) {
            val settings = gson.fromJson(settingsJson, WatermarkSettings::class.java)
            watermarkData.copy(
                title = settings.title,
                imageName = settings.imageName,
                watermarkStyle = settings.watermarkStyle,
                watermarkPosition = settings.watermarkPosition,
                timeFormat = settings.timeFormat,
                showWeatherInfo = settings.showWeatherInfo
            )
        } else {
            watermarkData
        }
    }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column {
            // 顶部倒计时
            if (!isTrialExpired) {
                val days = remainingMillis / (24 * 60 * 60 * 1000)
                val hours = (remainingMillis / (60 * 60 * 1000)) % 24
                val minutes = (remainingMillis / (60 * 1000)) % 60
                val seconds = (remainingMillis / 1000) % 60
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAF0E6))
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = 8.dp + androidx.compose.ui.platform.LocalDensity.current.run { 
                                (context.resources.getIdentifier("status_bar_height", "dimen", "android") 
                                    .takeIf { it > 0 }?.let { context.resources.getDimensionPixelSize(it) } ?: 0).toDp() 
                            },
                            bottom = 8.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "免费试用剩余：${days}天${hours}小时${minutes}分${seconds}秒",
                        color = Color(0xFFB22222),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // 其余内容
            Box(modifier = Modifier.padding(vertical = 10.dp)) {
                when (currentScreen) {
                    Screen.FolderSelection -> {
                        FolderSelectionScreen(
                            existingFolders = existingFolders,
                            onCreateNewFolder = { folderName ->
                                val folder = folderManager.createProjectFolder(folderName)
                                if (folder != null) {
                                    selectedFolder = folder
                                    existingFolders = folderManager.getAllProjectFolders()
                                    // 添加到数据库
                                    scope.launch {
                                        database.projectFolderDao().insertFolder(
                                            ProjectFolder(
                                                name = folderName,
                                                title = null
                                            )
                                        )
                                    }
                                    currentScreen = Screen.WatermarkSettings
                                } else {
                                    Toast.makeText(context, "创建文件夹失败", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSelectExistingFolder = { folder, existingTitle ->
                                selectedFolder = folder
                                watermarkData = loadWatermarkSettings(folder.name)
                                currentScreen = Screen.WatermarkSettings
                            },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            projectFolders = projectFolders
                        )
                    }
                    
                    Screen.WatermarkSettings -> {
                        WatermarkSettingsScreen(
                            watermarkData = watermarkData,
                            onWatermarkDataChange = { newData -> 
                                watermarkData = newData
                                // 保存设置
                                selectedFolder?.let { folder ->
                                    saveWatermarkSettings(folder.name, newData)
                                }
                            },
                            onStartCamera = {
                                // 开始获取位置信息
                                locationService.startLocationUpdates { location ->
                                    scope.launch {
                                        val locationName = locationService.getAddressFromLocation(
                                            location.latitude,
                                            location.longitude
                                        )
                                        val weatherInfo = weatherService.getWeatherInfo(
                                            location.latitude,
                                            location.longitude
                                        )
                                        
                                        watermarkData = watermarkData.copy(
                                            latitude = location.latitude,
                                            longitude = location.longitude,
                                            locationName = locationName,
                                            weather = weatherInfo.weather,
                                            temperature = weatherInfo.temperature,
                                            altitude = location.altitude.toString(),
                                            direction = locationService.getDirection(location.bearing)
                                        )
                                    }
                                }
                                currentScreen = Screen.Camera
                            },
                            onStartImageSelection = {
                                currentScreen = Screen.ImageSelection
                            },
                            onBackToFolderSelection = {
                                handleBackPress()
                            }
                        )
                    }
                    
                    Screen.ImageSelection -> {
                        selectedFolder?.let { folder ->
                            ImageSelectionScreen(
                                selectedFolder = folder,
                                onImageSelected = { uri ->
                                    selectedImageUri = uri
                                    currentScreen = Screen.ImagePreview
                                },
                                onBackPressed = {
                                    handleBackPress()
                                },
                                onStartCamera = {
                                    // 开始获取位置信息
                                    locationService.startLocationUpdates { location ->
                                        scope.launch {
                                            val locationName = locationService.getAddressFromLocation(
                                                location.latitude,
                                                location.longitude
                                            )
                                            val weatherInfo = weatherService.getWeatherInfo(
                                                location.latitude,
                                                location.longitude
                                            )
                                            
                                            watermarkData = watermarkData.copy(
                                                latitude = location.latitude,
                                                longitude = location.longitude,
                                                locationName = locationName,
                                                weather = weatherInfo.weather,
                                                temperature = weatherInfo.temperature,
                                                altitude = location.altitude.toString(),
                                                direction = locationService.getDirection(location.bearing)
                                            )
                                        }
                                    }
                                    currentScreen = Screen.Camera
                                }
                            )
                        }
                    }
                    
                    Screen.ImagePreview -> {
                        selectedImageUri?.let { uri ->
                            ImagePreviewScreen(
                                selectedImageUri = uri,
                                watermarkData = watermarkData,
                                onWatermarkDataChange = { newData ->
                                    watermarkData = newData
                                    // 保存设置
                                    selectedFolder?.let { folder ->
                                        saveWatermarkSettings(folder.name, newData)
                                    }
                                },
                                onSaveImage = { bitmap, finalWatermarkData ->
                                    scope.launch {
                                        // 获取序列号
                                        val sequenceNumber = selectedFolder?.let { folder ->
                                            folderManager.getNextSequenceNumberFromFiles(folder.name)
                                        } ?: 1
                                        
                                        // 生成文件名：使用标题-图片名称的格式
                                        val fileName = folderManager.generateFileName(
                                            sequenceNumber = sequenceNumber,
                                            timestamp = finalWatermarkData.timestamp,
                                            title = finalWatermarkData.title,
                                            imageName = finalWatermarkData.imageName
                                        )
                                        
                                        // 获取文件夹名称（从路径中提取）
                                        val folderName = selectedFolder?.name ?: "default"
                                        
                                        // 更新水印数据
                                        val updatedWatermarkData = finalWatermarkData.copy(
                                            sequenceNumber = sequenceNumber,
                                            customSequence = "SN-${deviceInfoUtil.formatDate(finalWatermarkData.timestamp)}-${String.format("%03d", sequenceNumber)}"
                                        )
                                        
                                        // 添加水印并保存到Pictures目录（用户可见）
                                        val success = watermarkGenerator.addWatermarkToSelectedImageAndSave(
                                            context,
                                            uri,
                                            updatedWatermarkData,
                                            fileName,
                                            folderName
                                        )
                                        
                                        if (success) {
                                            Toast.makeText(context, "图片已保存到相册: $fileName", Toast.LENGTH_SHORT).show()
                                            currentScreen = Screen.ImageSelection
                                        } else {
                                            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onBackPressed = {
                                    handleBackPress()
                                },
                                locationService = locationService,
                                weatherService = weatherService
                            )
                        }
                    }
                    
                    Screen.Camera -> {
                        CameraScreen(
                            watermarkData = watermarkData,
                            onPhotoTaken = { bitmap, tempFile ->
                                scope.launch {
                                    // 验证标题和图片名称是否都已填写
                                    if (watermarkData.title.isEmpty() || watermarkData.imageName.isEmpty()) {
                                        Toast.makeText(context, "请先填写标题和图片名称", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    
                                    // 获取序列号
                                    val sequenceNumber = selectedFolder?.let { folder ->
                                        folderManager.getNextSequenceNumberFromFiles(folder.name)
                                    } ?: 1
                                    
                                    // 生成文件名：使用标题-图片名称的格式
                                    val fileName = folderManager.generateFileName(
                                        sequenceNumber = sequenceNumber,
                                        timestamp = watermarkData.timestamp,
                                        title = watermarkData.title,
                                        imageName = watermarkData.imageName
                                    )
                                    
                                    // 获取文件夹名称（从路径中提取）
                                    val folderName = selectedFolder?.name ?: "default"
                                    
                                    // 更新水印数据
                                    val finalWatermarkData = watermarkData.copy(
                                        sequenceNumber = sequenceNumber,
                                        customSequence = "SN-${deviceInfoUtil.formatDate(watermarkData.timestamp)}-${String.format("%03d", sequenceNumber)}"
                                    )
                                    
                                    // 添加水印并保存到Pictures目录（用户可见）
                                    val success = watermarkGenerator.addWatermarkToPhotoAndSaveToPictures(
                                        context,
                                        bitmap,
                                        finalWatermarkData,
                                        fileName,
                                        folderName
                                    )
                                    
                                    if (success) {
                                        // 删除临时文件
                                        tempFile.delete()
                                        Toast.makeText(context, "照片已保存到相册: $fileName", Toast.LENGTH_SHORT).show()
                                        // 不清空标题，保持所有设置不变
                                    } else {
                                        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBackPressed = {
                                handleBackPress()
                            },
                            onWatermarkDataChange = { newData ->
                                watermarkData = newData
                                // 保存设置
                                selectedFolder?.let { folder ->
                                    saveWatermarkSettings(folder.name, newData)
                                }
                            },
                            onBackToSettings = {
                                // 返回到设置页面，但保持其他设置不变
                                handleBackPress()
                            },
                            locationService = locationService,
                            weatherService = weatherService
                        )
                    }
                }
            }
        }
    }
}

// 用于保存水印设置的数据类
data class WatermarkSettings(
    val title: String = "",
    val imageName: String = "",
    val watermarkStyle: WatermarkStyle = WatermarkStyle.SIMPLE,
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val timeFormat: TimeFormat = TimeFormat.DATE_ONLY,
    val showWeatherInfo: Boolean = false
)

enum class Screen {
    FolderSelection,
    WatermarkSettings,
    Camera,
    ImageSelection,
    ImagePreview
}