package com.lwr.watermarkcamera.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class PermissionManager {
    
    companion object {
        /**
         * 获取所有必需的权限列表（根据Android版本动态调整）
         */
        fun getRequiredPermissions(context: Context): Array<String> {
            val permissions = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            
            // Android 13+ 需要新的媒体权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                // Android 13以下使用旧权限
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            
            return permissions.toTypedArray()
        }
        
        /**
         * 检查所有必需权限是否已授予
         */
        fun hasAllPermissions(context: Context): Boolean {
            return getRequiredPermissions(context).all { permission ->
                ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        
        /**
         * 检查相机权限
         */
        fun hasCameraPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
        
        /**
         * 检查位置权限
         */
        fun hasLocationPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        
        /**
         * 检查媒体访问权限
         */
        fun hasMediaPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
        
        /**
         * 获取缺失的权限列表
         */
        fun getMissingPermissions(context: Context): List<String> {
            return getRequiredPermissions(context).filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }
        
        /**
         * 获取权限说明文本
         */
        fun getPermissionDescription(permission: String): String {
            return when (permission) {
                Manifest.permission.CAMERA -> "相机权限用于拍摄照片"
                Manifest.permission.ACCESS_FINE_LOCATION -> "精确位置权限用于获取GPS坐标"
                Manifest.permission.ACCESS_COARSE_LOCATION -> "粗略位置权限用于获取位置信息"
                Manifest.permission.READ_MEDIA_IMAGES -> "图片访问权限用于读取相册中的图片"
                Manifest.permission.READ_MEDIA_VIDEO -> "视频访问权限用于读取视频文件"
                Manifest.permission.READ_MEDIA_AUDIO -> "音频访问权限用于读取音频文件"
                Manifest.permission.READ_EXTERNAL_STORAGE -> "存储权限用于读取图片文件"
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> "存储权限用于保存处理后的图片"
                else -> "未知权限"
            }
        }
    }
} 