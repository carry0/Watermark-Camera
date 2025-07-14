package com.lwr.watermarkcamera.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.SizeF
import com.lwr.watermarkcamera.data.TimeFormat
import com.lwr.watermarkcamera.data.WatermarkData
import com.lwr.watermarkcamera.data.WatermarkPosition
import com.lwr.watermarkcamera.data.WatermarkStyle
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory

class WatermarkGenerator {
    
    companion object {
        // 调整水印参数，使用图片尺寸的比例来计算
        private const val WATERMARK_PADDING_RATIO = 0.02f // 边距为图片宽度的2%
        private const val TEXT_SIZE_RATIO = 0.03f // 文字大小为图片宽度的3%
        private const val TITLE_SIZE_RATIO = 0.035f // 标题大小为图片宽度的3.5%
        private const val LINE_SPACING_RATIO = 0.02f // 行间距为图片宽度的2%
        private const val BASE_FOLDER_NAME = "WatermarkCamera"

        /**
         * 通用水印绘制方法，预览和实际合成都可用
         */
        @JvmStatic
        fun drawWatermarkToCanvas(
            canvas: Canvas,
            watermarkData: WatermarkData,
            width: Int,
            height: Int,
            showBackground: Boolean = false
        ) {
            // 根据图片尺寸计算水印参数
            val paddingPx = width * WATERMARK_PADDING_RATIO
            val textSizePx = width * TEXT_SIZE_RATIO
            val titleSizePx = width * TITLE_SIZE_RATIO
            val lineSpacingPx = width * LINE_SPACING_RATIO

            // 创建画笔
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = titleSizePx
                isAntiAlias = true
                isFakeBoldText = true
                setShadowLayer(5f, 2f, 2f, Color.BLACK)
            }
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = textSizePx
                isAntiAlias = true
                setShadowLayer(5f, 2f, 2f, Color.BLACK)
            }

            // 计算水印区域大小
            val watermarkSize = calculateWatermarkSize(watermarkData, titlePaint, textPaint, paddingPx, lineSpacingPx)
            
            // 计算起始位置，根据水印位置设置
            val startX = when (watermarkData.watermarkPosition) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> paddingPx
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> width - watermarkSize.width - paddingPx
            }
            val startY = when (watermarkData.watermarkPosition) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> paddingPx
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> height - watermarkSize.height - paddingPx
            }

            // 如果需要显示背景
            if (showBackground) {
                val bgPaint = Paint().apply {
                    color = Color.BLACK
                    alpha = 128
                }
                canvas.drawRect(
                    startX,
                    startY,
                    startX + watermarkSize.width + paddingPx,
                    startY + watermarkSize.height + paddingPx,
                    bgPaint
                )
            }

            // 开始绘制水印内容
            drawWatermarkContent(
                canvas,
                watermarkData,
                startX,
                startY,
                titlePaint,
                textPaint,
                paddingPx,
                lineSpacingPx
            )
        }

        /**
         * 计算水印区域大小
         */
        @JvmStatic
        private fun calculateWatermarkSize(
            watermarkData: WatermarkData,
            titlePaint: Paint,
            textPaint: Paint,
            paddingPx: Float,
            lineSpacingPx: Float
        ): SizeF {
            var maxWidth = 0f
            var totalHeight = paddingPx * 2

            // 标题（如果有）
            if (watermarkData.title.isNotEmpty()) {
                maxWidth = maxOf(maxWidth, titlePaint.measureText(watermarkData.title))
                totalHeight += titlePaint.textSize + lineSpacingPx
            }

            // 时间
            val timeText = "时间: ${formatTimestamp(watermarkData.timestamp, watermarkData.timeFormat)}"
            maxWidth = maxOf(maxWidth, textPaint.measureText(timeText))
            totalHeight += textPaint.textSize + lineSpacingPx

            // 位置信息
            if (watermarkData.latitude != 0.0 && watermarkData.longitude != 0.0) {
                val locationText = "经纬度: ${String.format("%.6f°N,%.6f°E", watermarkData.latitude, watermarkData.longitude)}"
                maxWidth = maxOf(maxWidth, textPaint.measureText(locationText))
                totalHeight += textPaint.textSize + lineSpacingPx

                if (watermarkData.locationName.isNotEmpty()) {
                    val addressText = "地点: ${watermarkData.locationName}"
                    maxWidth = maxOf(maxWidth, textPaint.measureText(addressText))
                    totalHeight += textPaint.textSize + lineSpacingPx
                }
            }

            // 天气信息
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
                    maxWidth = maxOf(maxWidth, textPaint.measureText(weatherInfo))
                    totalHeight += textPaint.textSize + lineSpacingPx
                }
            }

            // 添加左右边距
            maxWidth += paddingPx * 2

            return SizeF(maxWidth, totalHeight)
        }

        @JvmStatic
        private fun formatTimestamp(timestamp: Long, timeFormat: TimeFormat): String {
            val pattern = when (timeFormat) {
                TimeFormat.DATE_ONLY -> "yyyy-MM-dd"
                TimeFormat.DATE_TIME -> "yyyy-MM-dd HH:mm:ss"
            }
            val dateFormat = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
            return dateFormat.format(java.util.Date(timestamp))
        }

        @JvmStatic
        private fun createBackgroundPaint(watermarkStyle: WatermarkStyle): Paint? {
            return when (watermarkStyle) {
                WatermarkStyle.CARD -> Paint().apply {
                    color = Color.BLACK
                    alpha = 128
                }
                WatermarkStyle.GRADIENT -> Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, 1000f, 0f,
                        Color.BLACK, Color.argb(76, 0, 0, 0),
                        Shader.TileMode.CLAMP
                    )
                }
                else -> null
            }
        }

        @JvmStatic
        private fun createTextPaint(textSize: Float, watermarkStyle: WatermarkStyle, isTitle: Boolean = false): Paint {
            return Paint().apply {
                color = Color.WHITE
                this.textSize = if (isTitle) textSize * 1.2f else textSize
                isAntiAlias = true
                
                when (watermarkStyle) {
                    WatermarkStyle.SIMPLE -> {
                        setShadowLayer(5f, 2f, 2f, Color.BLACK)
                    }
                    WatermarkStyle.BORDERED -> {
                        strokeWidth = 2f
                        style = Paint.Style.STROKE
                        color = Color.BLACK
                    }
                    else -> {
                        // CARD 和 GRADIENT 样式不需要特殊的文字效果
                    }
                }
            }
        }

        @JvmStatic
        private fun drawWatermarkContent(
            canvas: Canvas,
            watermarkData: WatermarkData,
            startX: Float,
            startY: Float,
            titlePaint: Paint,
            textPaint: Paint,
            paddingPx: Float,
            lineSpacingPx: Float
        ) {
            // 计算总高度
            var totalHeight = paddingPx * 2
            if (watermarkData.title.isNotEmpty()) {
                totalHeight += titlePaint.textSize + lineSpacingPx
            }
            totalHeight += textPaint.textSize + lineSpacingPx // 时间
            if (watermarkData.latitude != 0.0 && watermarkData.longitude != 0.0) {
                totalHeight += textPaint.textSize + lineSpacingPx // 经纬度
                if (watermarkData.locationName.isNotEmpty()) {
                    totalHeight += textPaint.textSize + lineSpacingPx // 地点
                }
            }
            if (watermarkData.showWeatherInfo) {
                val hasWeatherInfo = watermarkData.weather.isNotEmpty() || watermarkData.temperature.isNotEmpty()
                if (hasWeatherInfo) {
                    totalHeight += textPaint.textSize + lineSpacingPx
                }
            }

            var currentY = startY + titlePaint.textSize

            // 如果是卡片样式，绘制圆角矩形背景
            if (watermarkData.watermarkStyle == WatermarkStyle.CARD) {
                val bgPaint = createBackgroundPaint(WatermarkStyle.CARD)!!
                val rect = RectF(
                    startX - paddingPx,
                    startY - paddingPx,
                    startX + canvas.width * 0.7f + paddingPx,
                    startY + totalHeight
                )
                canvas.drawRoundRect(rect, 16f, 16f, bgPaint)
            }
            
            // 如果是渐变样式，绘制渐变背景
            if (watermarkData.watermarkStyle == WatermarkStyle.GRADIENT) {
                val bgPaint = createBackgroundPaint(WatermarkStyle.GRADIENT)!!
                canvas.drawRect(
                    startX - paddingPx,
                    startY - paddingPx,
                    startX + canvas.width * 0.7f + paddingPx,
                    startY + totalHeight,
                    bgPaint
                )
            }
            
            // 绘制标题（如果有）
            if (watermarkData.title.isNotEmpty()) {
                if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                    // 描边样式需要先绘制填充
                    val fillPaint = Paint(titlePaint).apply {
                        style = Paint.Style.FILL
                        color = Color.WHITE
                    }
                    canvas.drawText(watermarkData.title, startX + paddingPx, currentY, fillPaint)
                }
                canvas.drawText(watermarkData.title, startX + paddingPx, currentY, titlePaint)
                currentY += titlePaint.textSize + lineSpacingPx
            }
            
            // 绘制时间
            val timeText = "时间: ${formatTimestamp(watermarkData.timestamp, watermarkData.timeFormat)}"
            if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                val fillPaint = Paint(textPaint).apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
                canvas.drawText(timeText, startX + paddingPx, currentY, fillPaint)
            }
            canvas.drawText(timeText, startX + paddingPx, currentY, textPaint)
            currentY += textPaint.textSize + lineSpacingPx
            
            // 绘制位置信息
            if (watermarkData.latitude != 0.0 && watermarkData.longitude != 0.0) {
                val locationText = "经纬度: ${String.format("%.6f°N,%.6f°E", watermarkData.latitude, watermarkData.longitude)}"
                if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                    val fillPaint = Paint(textPaint).apply {
                        style = Paint.Style.FILL
                        color = Color.WHITE
                    }
                    canvas.drawText(locationText, startX + paddingPx, currentY, fillPaint)
                }
                canvas.drawText(locationText, startX + paddingPx, currentY, textPaint)
                currentY += textPaint.textSize + lineSpacingPx
                
                if (watermarkData.locationName.isNotEmpty()) {
                    val addressText = "地点: ${watermarkData.locationName}"
                    if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                        val fillPaint = Paint(textPaint).apply {
                            style = Paint.Style.FILL
                            color = Color.WHITE
                        }
                        canvas.drawText(addressText, startX + paddingPx, currentY, fillPaint)
                    }
                    canvas.drawText(addressText, startX + paddingPx, currentY, textPaint)
                    currentY += textPaint.textSize + lineSpacingPx
                }
            }
            
            // 绘制天气信息
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
                    if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                        val fillPaint = Paint(textPaint).apply {
                            style = Paint.Style.FILL
                            color = Color.WHITE
                        }
                        canvas.drawText(weatherInfo, startX + paddingPx, currentY, fillPaint)
                    }
                    canvas.drawText(weatherInfo, startX + paddingPx, currentY, textPaint)
                    currentY += textPaint.textSize + lineSpacingPx
                }
            }
        }
    }
    
    /**
     * 在照片上添加水印并保存到Pictures目录（用户可见）
     */
    fun addWatermarkToPhotoAndSaveToPictures(
        context: Context,
        originalBitmap: Bitmap,
        watermarkData: WatermarkData,
        fileName: String,
        folderName: String
    ): Boolean {
        return try {
            val watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(watermarkedBitmap)

            drawWatermarkToCanvas(
                canvas,
                watermarkData,
                watermarkedBitmap.width,
                watermarkedBitmap.height
            )
            
            saveBitmapToPictures(context, watermarkedBitmap, fileName, folderName)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 在照片上添加水印并保存（原有方法，保存到应用私有目录）
     */
    fun addWatermarkToPhoto(
        originalBitmap: Bitmap,
        watermarkData: WatermarkData,
        outputFile: File
    ): Boolean {
        return try {
            val watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(watermarkedBitmap)
            
            drawWatermarkToCanvas(
                canvas,
                watermarkData,
                watermarkedBitmap.width,
                watermarkedBitmap.height
            )
            
            saveBitmapToFile(watermarkedBitmap, outputFile)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 使用MediaStore API保存位图到Pictures目录
     */
    private fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String, folderName: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$BASE_FOLDER_NAME/$folderName")
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { imageUri ->
                context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                return true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 保存位图到文件
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 将Drawable转换为Bitmap
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 为选择的图片添加水印并保存
     */
    fun addWatermarkToSelectedImageAndSave(
        context: Context,
        imageUri: Uri,
        watermarkData: WatermarkData,
        fileName: String,
        folderName: String
    ): Boolean {
        return try {
            // 从URI加载图片
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                return false
            }
            
            // 创建可编辑的位图副本
            val editableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(editableBitmap)
            
            // 添加水印
            drawWatermarkToCanvas(
                canvas = canvas,
                watermarkData = watermarkData,
                width = editableBitmap.width,
                height = editableBitmap.height,
                showBackground = watermarkData.watermarkStyle != WatermarkStyle.SIMPLE
            )
            
            // 保存到Pictures目录
            val success = saveBitmapToPictures(context, editableBitmap, fileName, folderName)
            
            // 清理资源
            originalBitmap.recycle()
            editableBitmap.recycle()
            
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 为选择的图片添加水印并返回Bitmap（用于预览）
     */
    fun addWatermarkToSelectedImage(
        context: Context,
        imageUri: Uri,
        watermarkData: WatermarkData
    ): Bitmap? {
        return try {
            // 从URI加载图片
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                return null
            }
            
            // 创建可编辑的位图副本
            val editableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(editableBitmap)
            
            // 添加水印
            drawWatermarkToCanvas(
                canvas = canvas,
                watermarkData = watermarkData,
                width = editableBitmap.width,
                height = editableBitmap.height,
                showBackground = watermarkData.watermarkStyle != WatermarkStyle.SIMPLE
            )
            
            // 清理原始位图
            originalBitmap.recycle()
            
            editableBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 