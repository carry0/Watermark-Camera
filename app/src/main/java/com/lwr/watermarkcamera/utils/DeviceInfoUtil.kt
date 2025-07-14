package com.lwr.watermarkcamera.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

class DeviceInfoUtil(private val context: Context) {
    
    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val version = Build.VERSION.RELEASE
        val sdkVersion = Build.VERSION.SDK_INT
        
        return "$manufacturer $model, Android $version (API $sdkVersion)"
    }
    
    /**
     * 获取设备ID
     */
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
    
    /**
     * 格式化时间
     */
    fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化日期
     */
    fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化时间
     */
    fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
} 