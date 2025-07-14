package com.lwr.watermarkcamera.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class PermissionManager {
    
    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        /**
         * 检查所有必需权限是否已授予
         */
        fun hasAllPermissions(context: Context): Boolean {
            return REQUIRED_PERMISSIONS.all { permission ->
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
         * 获取缺失的权限列表
         */
        fun getMissingPermissions(context: Context): List<String> {
            return REQUIRED_PERMISSIONS.filter { permission ->
                ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }
    }
} 