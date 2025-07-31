package com.lwr.watermarkcamera.utils

import android.content.ContentResolver
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用于从图片中提取EXIF信息的工具类
 */
class ExifDataReader(private val context: Context) {

    /**
     * 从图片URI中提取EXIF信息
     */
    suspend fun extractExifData(uri: Uri): ExifData = withContext(Dispatchers.IO) {
        val contentResolver: ContentResolver = context.contentResolver
        var inputStream: InputStream? = null
        
        try {
            // 检查权限
            if (!PermissionManager.hasMediaPermission(context)) {
                Log.w("ExifDataReader", "缺少媒体访问权限")
                return@withContext ExifData(errorMessage = "缺少媒体访问权限")
            }
            
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.w("ExifDataReader", "无法打开图片输入流")
                return@withContext ExifData(errorMessage = "无法打开图片文件")
            }
            
            val exif = ExifInterface(inputStream)

            // 提取GPS信息
            val latLongArray = FloatArray(2)
            val hasLatLong = exif.getLatLong(latLongArray)
            val latitude = if (hasLatLong) latLongArray[0].toDouble() else 0.0
            val longitude = if (hasLatLong) latLongArray[1].toDouble() else 0.0
            
            // 提取拍摄日期
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
            val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            val dateTimeDigitized = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
            
            // 使用可用的日期时间，优先使用原始拍摄时间
            val dateStr = dateTimeOriginal ?: dateTime ?: dateTimeDigitized
            val timestamp = parseExifDate(dateStr)
            val formattedDate = formatDate(timestamp)
            
            // 提取图片方向
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val isLandscape = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270
            
            Log.d("ExifDataReader", "成功提取EXIF数据: GPS=$hasLatLong, 日期=$formattedDate, 方向=$orientation")
            
            return@withContext ExifData(
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp,
                formattedDate = formattedDate,
                isLandscape = isLandscape,
                hasGpsData = hasLatLong,
                errorMessage = null
            )
        } catch ( e:SecurityException) {
            Log.e("ExifDataReader", "权限不足，无法访问图片: ${e.message}")
            return@withContext ExifData(errorMessage = "权限不足，无法访问图片")
        } catch (e:Exception) {
            Log.e("ExifDataReader", "提取EXIF数据失败: ${e.message}")
            return@withContext ExifData(errorMessage = "提取图片信息失败: ${e.message}")
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                Log.w("ExifDataReader", "关闭输入流失败: ${e.message}")
            }
        }
    }
    
    /**
     * 解析EXIF日期格式（yyyy:MM:dd HH:mm:ss）
     */
    private fun parseExifDate(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) {
            return System.currentTimeMillis()
        }
        
        return try {
            val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w("ExifDataReader", "解析日期失败: $dateStr, 使用当前时间")
            System.currentTimeMillis()
        }
    }
    
    /**
     * 格式化日期为yyyy-MM-dd格式
     */
    private fun formatDate(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}

/**
 * 存储EXIF数据的数据类
 */
data class ExifData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = "",
    val isLandscape: Boolean = false,
    val hasGpsData: Boolean = false,
    val errorMessage: String? = null
) 