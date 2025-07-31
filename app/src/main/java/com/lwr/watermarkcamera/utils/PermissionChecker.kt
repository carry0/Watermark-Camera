package com.lwr.watermarkcamera.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

/**
 * 详细的权限检查工具类
 */
class PermissionChecker(private val context: Context) {
    
    /**
     * 检查所有权限状态并返回详细报告
     */
    fun checkAllPermissions(): PermissionReport {
        return PermissionReport(
            camera = hasCameraPermission(),
            location = hasLocationPermission(),
            media = hasMediaPermission(),
            storage = hasStoragePermission(),
            androidVersion = Build.VERSION.SDK_INT,
            requiredPermissions = getRequiredPermissionsForVersion()
        )
    }
    
    /**
     * 检查相机权限
     */
    fun hasCameraPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查位置权限
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查媒体权限
     */
    fun hasMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 检查存储权限
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要WRITE_EXTERNAL_STORAGE
            true
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 根据Android版本获取所需的权限列表
     */
    fun getRequiredPermissionsForVersion(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        return permissions
    }
    
    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(): List<String> {
        return getRequiredPermissionsForVersion().filter { permission ->
            ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 获取权限状态描述
     */
    fun getPermissionStatusDescription(): String {
        val report = checkAllPermissions()
        val missingPermissions = getMissingPermissions()
        
        return buildString {
            appendLine("Android版本: ${report.androidVersion}")
            appendLine("相机权限: ${if (report.camera) "已授予" else "未授予"}")
            appendLine("位置权限: ${if (report.location) "已授予" else "未授予"}")
            appendLine("媒体权限: ${if (report.media) "已授予" else "未授予"}")
            appendLine("存储权限: ${if (report.storage) "已授予" else "未授予"}")
            
            if (missingPermissions.isNotEmpty()) {
                appendLine("缺失权限: ${missingPermissions.joinToString(", ")}")
            }
        }
    }
}

/**
 * 权限状态报告数据类
 */
data class PermissionReport(
    val camera: Boolean,
    val location: Boolean,
    val media: Boolean,
    val storage: Boolean,
    val androidVersion: Int,
    val requiredPermissions: List<String>
) 