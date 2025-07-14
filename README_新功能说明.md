# 水印相机 - 图片选择功能说明

## 新增功能概述

在原有拍照添加水印功能的基础上，新增了**图片选择添加水印**功能，用户现在可以从相册中选择已有图片来添加水印。

## 功能模块

### 1. 核心功能模块

#### 现有模块
- **文件夹管理模块** (`FolderManager`) - 管理项目文件夹
- **水印生成模块** (`WatermarkGenerator`) - 处理水印添加逻辑
- **位置服务模块** (`LocationService`) - 获取GPS位置信息
- **天气服务模块** (`WeatherService`) - 获取天气信息
- **设备信息模块** (`DeviceInfoUtil`) - 获取设备信息
- **权限管理模块** (`PermissionManager`) - 管理应用权限

#### 新增模块
- **图片选择模块** (`ImageSelectionScreen`) - 从相册选择图片
- **图片预览模块** (`ImagePreviewScreen`) - 预览选择的图片并添加水印
- **图片处理模块** (`WatermarkGenerator`新增方法) - 处理选择的图片

### 2. UI界面模块

- `FolderSelectionScreen` - 文件夹选择界面
- `WatermarkSettingsScreen` - 水印设置界面（新增图片选择按钮）
- `CameraScreen` - 拍照界面
- `ImageSelectionScreen` - 图片选择界面（新增）
- `ImagePreviewScreen` - 图片预览界面（新增）

## 功能流程

### 拍照添加水印流程
1. 选择/创建文件夹
2. 设置水印参数（标题、图片名称等）
3. 点击"开始拍照"按钮
4. 拍照
5. 自动添加水印
6. 保存到相册

### 图片选择添加水印流程
1. 选择/创建文件夹
2. 设置水印参数（标题、图片名称等）
3. 点击"选择图片添加水印"按钮
4. 从相册选择图片
5. 预览并调整水印设置
6. 确认添加水印
7. 保存到相册

## 主要特性

### ✅ 核心功能
- 支持拍照添加水印
- 支持选择已有图片添加水印
- 实时预览水印效果
- 批量选择图片（一次处理一张）

### ✅ 用户体验
- 保持水印设置一致性
- 自动获取位置和天气信息
- 支持多种水印样式
- 文件自动命名和序列号管理

### ✅ 技术特性
- 使用MediaStore API访问图片
- 支持高分辨率图片处理
- 内存优化，及时释放资源
- 错误处理和用户提示

## 使用方法

### 1. 拍照添加水印
1. 打开应用，选择或创建项目文件夹
2. 填写标题和图片名称
3. 调整水印样式和位置
4. 点击"开始拍照"按钮
5. 拍照后自动添加水印并保存

### 2. 选择图片添加水印
1. 打开应用，选择或创建项目文件夹
2. 填写标题和图片名称
3. 调整水印样式和位置
4. 点击"选择图片添加水印"按钮
5. 从相册中选择要处理的图片
6. 在预览界面确认水印效果
7. 点击"保存图片"完成处理

## 技术实现

### 新增的核心方法

#### WatermarkGenerator类
```kotlin
// 为选择的图片添加水印并保存
fun addWatermarkToSelectedImageAndSave(
    context: Context,
    imageUri: Uri,
    watermarkData: WatermarkData,
    fileName: String,
    folderName: String
): Boolean

// 为选择的图片添加水印并返回Bitmap（用于预览）
fun addWatermarkToSelectedImage(
    context: Context,
    imageUri: Uri,
    watermarkData: WatermarkData
): Bitmap?
```

### 权限要求
- `READ_EXTERNAL_STORAGE` (Android 13以下)
- `READ_MEDIA_IMAGES` (Android 13及以上)
- `CAMERA` - 拍照功能
- `ACCESS_FINE_LOCATION` - 获取位置信息

## 文件结构

```
app/src/main/java/com/lwr/watermarkcamera/
├── MainActivity.kt                    # 主活动，集成所有功能
├── ui/
│   ├── FolderSelectionScreen.kt       # 文件夹选择界面
│   ├── WatermarkSettingsScreen.kt     # 水印设置界面（已更新）
│   ├── CameraScreen.kt               # 拍照界面
│   ├── ImageSelectionScreen.kt       # 图片选择界面（新增）
│   └── ImagePreviewScreen.kt         # 图片预览界面（新增）
├── utils/
│   ├── WatermarkGenerator.kt         # 水印生成器（已更新）
│   ├── FolderManager.kt              # 文件夹管理
│   ├── LocationService.kt            # 位置服务
│   ├── WeatherService.kt             # 天气服务
│   ├── DeviceInfoUtil.kt             # 设备信息
│   └── PermissionManager.kt          # 权限管理
└── data/
    ├── WatermarkData.kt              # 水印数据模型
    ├── AppDatabase.kt                # 数据库
    └── ProjectFolder.kt              # 项目文件夹模型
```

## 注意事项

1. **内存管理**: 处理大图片时注意内存使用，及时释放Bitmap资源
2. **权限处理**: 确保用户授予了必要的存储和相机权限
3. **错误处理**: 对图片加载失败、保存失败等情况进行适当处理
4. **用户体验**: 在处理过程中显示加载状态，避免界面卡顿

## 未来改进

1. **批量处理**: 支持同时处理多张图片
2. **水印模板**: 支持保存和加载水印模板
3. **图片编辑**: 支持简单的图片编辑功能（裁剪、旋转等）
4. **云端同步**: 支持将水印设置同步到云端 