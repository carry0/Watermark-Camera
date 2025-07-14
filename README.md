# 水印相机应用

一个功能丰富的Android水印相机应用，支持自定义水印内容、GPS定位、天气信息、设备信息等多种水印元素。

## 功能特性

### 核心功能
- 📁 **文件夹管理**: 用户输入文件夹名创建专用文件夹，支持选择现有文件夹
- 📸 **相机拍照**: 集成CameraX，提供高质量的拍照体验
- 🏷️ **智能水印**: 自动添加丰富的水印信息
- 🔢 **序列编号**: 自动生成顺序编号，支持项目特定编码
- 💾 **自动保存**: 照片自动保存到指定文件夹

### 水印内容
水印包含以下信息：

#### 基本信息
- **标题**: 用户自定义的标题（例如：工程检查、会议记录）
- **时间**: 照片拍摄的精确时间（年月日时分秒）
- **项目名称/编号**: 用于工程管理

#### 位置信息
- **经纬度**: GPS坐标（格式：经度, 纬度）
- **位置名称**: 通过经纬度反解析得到的地址
- **海拔高度**: GPS提供的高度信息
- **方向**: 拍摄时的手机朝向（北、东南等）

#### 环境信息
- **天气情况**: 当时的天气（例如：晴、多云、小雨等）
- **当前温度**: 结合天气情况
- **气压/湿度**: 详细的气象数据

#### 设备信息
- **设备信息**: 手机型号、系统版本
- **序列号**: 除了顺序编号，还可以有项目特定的编码

#### 管理信息
- **审核状态**: 用于工作流（例如：已审核、未审核）
- **保密等级**: 水印上显示（例如：内部使用、机密）
- **备注**: 用户临时输入的文字备注
- **公司Logo/个人头像**: 图片水印（可选）

## 技术架构

### 主要组件
- **MainActivity**: 主界面控制器，管理应用状态和导航
- **FolderManager**: 文件夹管理工具类
- **LocationService**: 位置服务，获取GPS和地址信息
- **WeatherService**: 天气服务，获取天气数据
- **DeviceInfoUtil**: 设备信息工具类
- **WatermarkGenerator**: 水印生成器
- **PermissionManager**: 权限管理器

### UI界面
- **FolderSelectionScreen**: 文件夹选择界面
- **WatermarkSettingsScreen**: 水印设置界面
- **CameraScreen**: 相机拍照界面

### 数据模型
- **WatermarkData**: 水印数据模型，包含所有水印信息字段

## 权限要求

应用需要以下权限：
- `CAMERA`: 相机权限，用于拍照
- `WRITE_EXTERNAL_STORAGE`: 写入外部存储权限
- `READ_EXTERNAL_STORAGE`: 读取外部存储权限
- `ACCESS_FINE_LOCATION`: 精确位置权限
- `ACCESS_COARSE_LOCATION`: 粗略位置权限
- `INTERNET`: 网络权限，用于获取天气信息
- `ACCESS_NETWORK_STATE`: 网络状态权限

## 使用流程

1. **启动应用**: 应用启动后显示文件夹选择界面
2. **创建/选择文件夹**: 
   - 输入新文件夹名称创建新项目文件夹
   - 或选择现有的项目文件夹
3. **配置水印**: 在水印设置界面配置各种水印参数
4. **开始拍照**: 点击"开始拍照"按钮进入相机界面
5. **拍照**: 点击拍照按钮进行拍照
6. **自动处理**: 系统自动添加水印、生成编号、保存到指定文件夹

## 文件结构

```
app/src/main/java/com/lwr/watermarkcamera/
├── MainActivity.kt                    # 主Activity
├── data/
│   └── WatermarkData.kt              # 水印数据模型
├── ui/
│   ├── CameraScreen.kt               # 相机界面
│   ├── FolderSelectionScreen.kt      # 文件夹选择界面
│   └── WatermarkSettingsScreen.kt    # 水印设置界面
└── utils/
    ├── DeviceInfoUtil.kt             # 设备信息工具
    ├── FolderManager.kt              # 文件夹管理
    ├── LocationService.kt            # 位置服务
    ├── PermissionManager.kt          # 权限管理
    ├── WeatherService.kt             # 天气服务
    └── WatermarkGenerator.kt         # 水印生成器
```

## 依赖库

- **CameraX**: 相机功能
- **Jetpack Compose**: UI框架
- **OkHttp**: 网络请求
- **Retrofit**: HTTP客户端
- **Kotlin Coroutines**: 异步编程

## 开发环境

- Android Studio Hedgehog | 2023.1.1
- Kotlin 1.9.0
- Android SDK 35
- Minimum SDK 34

## 构建和运行

1. 克隆项目到本地
2. 在Android Studio中打开项目
3. 确保已安装必要的SDK和工具
4. 连接Android设备或启动模拟器
5. 点击运行按钮构建并安装应用

## 注意事项

1. **权限**: 首次运行时需要授予所有必要权限
2. **存储**: 照片保存在 `Pictures/WatermarkCamera/` 目录下
3. **天气API**: 当前使用模拟天气数据，实际使用时需要配置真实的天气API密钥
4. **网络**: 获取地址信息需要网络连接

## 未来改进

- [ ] 支持自定义水印样式和位置
- [ ] 添加水印模板功能
- [ ] 支持批量处理照片
- [ ] 添加照片预览和编辑功能
- [ ] 支持云存储同步
- [ ] 添加照片管理功能
- [ ] 支持多种水印格式
- [ ] 添加水印预览功能

## 许可证

本项目采用 MIT 许可证。 