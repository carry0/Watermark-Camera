package com.lwr.watermarkcamera.data

import android.graphics.Bitmap
import java.io.Serializable

enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

enum class TimeFormat {
    DATE_ONLY,    // 仅显示年月日
    DATE_TIME     // 显示年月日时分秒
}

enum class WatermarkStyle {
    SIMPLE,      // 简约样式：白色文字+阴影
    CARD,        // 卡片样式：半透明背景+圆角
    GRADIENT,    // 渐变样式：渐变背景
    BORDERED     // 描边样式：文字描边
}

data class WatermarkData(
    val title: String = "", // 用户自定义标题
    val imageName: String = "", // 图片名称
    val timestamp: Long = System.currentTimeMillis(), // 拍摄时间
    val latitude: Double = 0.0, // 纬度
    val longitude: Double = 0.0, // 经度
    val latitudeString: String = "", // 纬度字符串（保持原始精度）
    val longitudeString: String = "", // 经度字符串（保持原始精度）
    val locationName: String = "", // 位置名称
    val weather: String = "", // 天气情况
    val deviceInfo: String = "", // 设备信息
    val remark: String = "", // 备注
    val logoBitmap: Bitmap? = null, // 公司Logo/个人头像
    val projectName: String = "", // 项目名称
    val projectCode: String = "", // 项目编号
    val temperature: String = "", // 当前温度
    val altitude: String = "", // 海拔高度
    val direction: String = "", // 拍摄方向
    val reviewStatus: String = "未审核", // 审核状态
    val securityLevel: String = "内部使用", // 保密等级
    val sequenceNumber: Int = 0, // 序列号
    val customSequence: String = "", // 自定义序列号

    // 水印显示配置
    val showTitle: Boolean = true, // 显示标题
    val showImageName: Boolean = true, // 显示图片名称（影响保存文件名格式）
    val showTime: Boolean = true, // 显示时间
    val showDate: Boolean = true, // 显示日期
    val showLocation: Boolean = true, // 显示地点信息
    val showCoordinates: Boolean = true, // 显示经纬度
    val showProjectInfo: Boolean = true, // 显示项目信息
    val showWeatherInfo: Boolean = true, // 显示天气信息
    val showDeviceInfo: Boolean = true, // 显示设备信息
    val showReviewStatus: Boolean = true, // 显示审核状态
    val showSecurityLevel: Boolean = true, // 显示保密等级
    val showDirection: Boolean = true, // 显示拍摄方向
    val showRemark: Boolean = true, // 显示备注
    val showLogo: Boolean = true, // 显示Logo
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,  // 默认右下角
    val timeFormat: TimeFormat = TimeFormat.DATE_ONLY,
    val watermarkStyle: WatermarkStyle = WatermarkStyle.SIMPLE,
    val isLandscape: Boolean = false // 是否为横屏图片，用于水印方向调整
    // 注意：序列号是必须显示的，不能关闭
) : Serializable 