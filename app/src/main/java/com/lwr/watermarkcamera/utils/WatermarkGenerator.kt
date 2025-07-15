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
        private const val WATERMARK_PADDING_RATIO = 0.03f // 边距为图片宽度的3%（增加边距）
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
            showBackground: Boolean = false,
            rotationDegrees: Float = 0f,
            adjustedPosition: WatermarkPosition = watermarkData.watermarkPosition
        ) {
            // 根据图片尺寸计算水印参数
            val basePaddingPx = width * WATERMARK_PADDING_RATIO
            // 分别计算X轴和Y轴的边距
            val paddingX = 20f  // X轴边距保持基础值
            val paddingY = if (rotationDegrees != 0f) basePaddingPx * 2f else basePaddingPx  // Y轴在旋转时稍微增加
            val textSizePx = width * TEXT_SIZE_RATIO
            val titleSizePx = width * TITLE_SIZE_RATIO
            val lineSpacingPx = width * LINE_SPACING_RATIO

            Log.d("WatermarkGenerator", "=== 边距计算调试信息 ===")
            Log.d("WatermarkGenerator", "WATERMARK_PADDING_RATIO: ${WATERMARK_PADDING_RATIO}")
            Log.d("WatermarkGenerator", "图片宽度: ${width}")
            Log.d("WatermarkGenerator", "基础边距: ${basePaddingPx}")
            Log.d("WatermarkGenerator", "是否旋转: ${rotationDegrees != 0f}")
            Log.d("WatermarkGenerator", "X轴边距: ${paddingX}")
            Log.d("WatermarkGenerator", "Y轴边距: ${paddingY}")
            Log.d("WatermarkGenerator", "X轴边距占比: ${paddingX / width * 100}%")
            Log.d("WatermarkGenerator", "Y轴边距占比: ${paddingY / width * 100}%")
            Log.d("WatermarkGenerator", "=========================")

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
            val watermarkSize = calculateWatermarkSize(watermarkData, titlePaint, textPaint, paddingX, lineSpacingPx)
            
            // 如果有旋转，需要考虑旋转后的实际占用空间
            val actualWidth: Float
            val actualHeight: Float
            
            if (rotationDegrees == 90f || rotationDegrees == 270f) {
                // 90度或270度旋转：宽高交换
                actualWidth = watermarkSize.height
                actualHeight = watermarkSize.width
                Log.d("WatermarkGenerator", "旋转90/270度，宽高交换")
                Log.d("WatermarkGenerator", "原始尺寸: ${watermarkSize.width}x${watermarkSize.height}")
                Log.d("WatermarkGenerator", "旋转后尺寸: ${actualWidth}x${actualHeight}")
            } else {
                // 0度或180度旋转：尺寸不变
                actualWidth = watermarkSize.width
                actualHeight = watermarkSize.height
            }

            Log.d("WatermarkGenerator", "=== 位置计算调试信息 ===")
            Log.d("WatermarkGenerator", "图片尺寸: ${width}x${height}")
            Log.d("WatermarkGenerator", "旋转角度: ${rotationDegrees}°")
            Log.d("WatermarkGenerator", "原始位置: ${watermarkData.watermarkPosition}")
            Log.d("WatermarkGenerator", "调整后位置: ${adjustedPosition}")
            Log.d("WatermarkGenerator", "水印原始尺寸: ${watermarkSize.width}x${watermarkSize.height}")
            Log.d("WatermarkGenerator", "水印实际占用尺寸: ${actualWidth}x${actualHeight}")
            Log.d("WatermarkGenerator", "边距: ${paddingX}")

            // 根据传入的调整后位置和实际占用尺寸计算坐标
            val startX = when (adjustedPosition) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> paddingX
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> width - actualWidth - paddingX
            }
            val startY = when (adjustedPosition) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> paddingY
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> height - actualHeight - paddingY
            }

            Log.d("WatermarkGenerator", "计算的起始坐标: (${startX}, ${startY})")

            // 确保水印不会超出边界（使用实际占用尺寸）
            val safeStartX = startX.coerceIn(paddingX, width - actualWidth - paddingX)
            val safeStartY = startY.coerceIn(paddingY, height - actualHeight - paddingY)

            Log.d("WatermarkGenerator", "安全坐标: (${safeStartX}, ${safeStartY})")
            Log.d("WatermarkGenerator", "水印区域: (${safeStartX}, ${safeStartY}) 到 (${safeStartX + actualWidth}, ${safeStartY + actualHeight})")
            Log.d("WatermarkGenerator", "图片范围: (0, 0) 到 (${width}, ${height})")
            
            // 检查是否超出边界（使用实际占用尺寸）
            val rightEdge = safeStartX + actualWidth
            val bottomEdge = safeStartY + actualHeight
            val isWithinBounds = rightEdge <= width && bottomEdge <= height
            
            Log.d("WatermarkGenerator", "右边缘: ${rightEdge} (图片宽度: ${width}) ${if (rightEdge <= width) "✅" else "❌超出"}")
            Log.d("WatermarkGenerator", "底边缘: ${bottomEdge} (图片高度: ${height}) ${if (bottomEdge <= height) "✅" else "❌超出"}")
            Log.d("WatermarkGenerator", "是否在边界内: ${if (isWithinBounds) "✅ 是" else "❌ 否"}")
            Log.d("WatermarkGenerator", "=========================")

            // 如果需要显示背景
            if (showBackground) {
                val bgPaint = Paint().apply {
                    color = Color.BLACK
                    alpha = 128
                }
                canvas.drawRect(
                    safeStartX,
                    safeStartY,
                    safeStartX + actualWidth + paddingX,
                    safeStartY + actualHeight + paddingY,
                    bgPaint
                )
            }

            // 开始绘制水印内容
            drawWatermarkContent(
                canvas,
                watermarkData,
                safeStartX,
                safeStartY,
                titlePaint,
                textPaint,
                paddingX,
                lineSpacingPx,
                rotationDegrees
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
                val titleWidth = titlePaint.measureText(watermarkData.title)
                maxWidth = maxOf(maxWidth, titleWidth)
                totalHeight += titlePaint.textSize + lineSpacingPx
            }

            // 时间（根据显示设置）
            if (watermarkData.showTime) {
                val timeText = "时间: ${formatTimestamp(watermarkData.timestamp, watermarkData.timeFormat)}"
                val timeWidth = textPaint.measureText(timeText)
                maxWidth = maxOf(maxWidth, timeWidth)
                totalHeight += textPaint.textSize + lineSpacingPx
            }

            // 位置信息（根据显示设置）
            if (watermarkData.showLocation && watermarkData.latitude != 0.0 && watermarkData.longitude != 0.0) {
                val locationText = "经纬度: ${String.format("%.6f°N,%.6f°E", watermarkData.latitude, watermarkData.longitude)}"
                val locationWidth = textPaint.measureText(locationText)
                maxWidth = maxOf(maxWidth, locationWidth)
                totalHeight += textPaint.textSize + lineSpacingPx

                if (watermarkData.locationName.isNotEmpty()) {
                    val addressText = "地点: ${watermarkData.locationName}"
                    val addressWidth = textPaint.measureText(addressText)
                    maxWidth = maxOf(maxWidth, addressWidth)
                    totalHeight += textPaint.textSize + lineSpacingPx
                }
            }

            // 天气信息（根据显示设置）
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
                    val weatherWidth = textPaint.measureText(weatherInfo)
                    maxWidth = maxOf(maxWidth, weatherWidth)
                    totalHeight += textPaint.textSize + lineSpacingPx
                }
            }

            // 添加左右边距，确保有足够的空间
            maxWidth += paddingPx * 2
            
            // 确保最小宽度
            maxWidth = maxOf(maxWidth, 200f)

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
            lineSpacingPx: Float,
            rotationDegrees: Float,
            adjustedPosition: WatermarkPosition = watermarkData.watermarkPosition
        ) {
            // 计算水印区域的中心点
            val watermarkSize = calculateWatermarkSize(watermarkData, titlePaint, textPaint, paddingPx, lineSpacingPx)
            
            // 根据旋转角度调整中心点计算
            val centerX: Float
            val centerY: Float
            val adjustedStartX: Float
            val adjustedStartY: Float
            
            when (rotationDegrees.toInt()) {
                90, 270 -> {
                    // 90度或270度旋转时，需要调整中心点和起始位置
                    // 旋转后的实际占用空间是高度变宽度，宽度变高度
                    val actualWidth = watermarkSize.height
                    val actualHeight = watermarkSize.width
                    
                    // 重新计算旋转后的中心点
                    centerX = startX + actualWidth / 2
                    centerY = startY + actualHeight / 2
                    
                    // 调整起始位置，使旋转后的文字显示在正确位置
                    adjustedStartX = startX + (actualWidth - watermarkSize.width) / 2
                    adjustedStartY = startY + (actualHeight - watermarkSize.height) / 2
                    
                    Log.d("WatermarkGenerator", "=== 旋转调整信息 ===")
                    Log.d("WatermarkGenerator", "原始中心点: (${startX + watermarkSize.width / 2}, ${startY + watermarkSize.height / 2})")
                    Log.d("WatermarkGenerator", "调整后中心点: (${centerX}, ${centerY})")
                    Log.d("WatermarkGenerator", "原始起始点: (${startX}, ${startY})")
                    Log.d("WatermarkGenerator", "调整后起始点: (${adjustedStartX}, ${adjustedStartY})")
                    Log.d("WatermarkGenerator", "实际占用尺寸: ${actualWidth}x${actualHeight}")
                    Log.d("WatermarkGenerator", "==================")
                }
                else -> {
                    // 0度或180度旋转，使用原始计算
                    centerX = startX + watermarkSize.width / 2
                    centerY = startY + watermarkSize.height / 2
                    adjustedStartX = startX
                    adjustedStartY = startY
                }
            }

            // 保存画布状态并进行整体旋转
            canvas.save()
            if (rotationDegrees != 0f) {
                canvas.rotate(rotationDegrees, centerX, centerY)
            }

            // 根据位置类型调整currentY的初始值
            var currentY = when (adjustedPosition) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> {
                    // 顶部位置：从调整后的startY开始向下绘制
                    adjustedStartY + paddingPx + titlePaint.textSize
                }
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> {
                    // 底部位置：从调整后的startY开始向下绘制
                    adjustedStartY + paddingPx + titlePaint.textSize
                }
            }

            // 如果是卡片样式，绘制圆角矩形背景
            if (watermarkData.watermarkStyle == WatermarkStyle.CARD) {
                val bgPaint = createBackgroundPaint(WatermarkStyle.CARD)!!
                val rect = RectF(
                    adjustedStartX - paddingPx,
                    adjustedStartY - paddingPx,
                    adjustedStartX + canvas.width * 0.7f + paddingPx,
                    adjustedStartY + watermarkSize.height
                )
                canvas.drawRoundRect(rect, 16f, 16f, bgPaint)
            }
            
            // 如果是渐变样式，绘制渐变背景
            if (watermarkData.watermarkStyle == WatermarkStyle.GRADIENT) {
                val bgPaint = createBackgroundPaint(WatermarkStyle.GRADIENT)!!
                canvas.drawRect(
                    adjustedStartX - paddingPx,
                    adjustedStartY - paddingPx,
                    adjustedStartX + canvas.width * 0.7f + paddingPx,
                    adjustedStartY + watermarkSize.height,
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
                    canvas.drawText(watermarkData.title, adjustedStartX + paddingPx, currentY, fillPaint)
                }
                canvas.drawText(watermarkData.title, adjustedStartX + paddingPx, currentY, titlePaint)
                currentY += titlePaint.textSize + lineSpacingPx
            }
            
            // 绘制时间（根据显示设置）
            if (watermarkData.showTime) {
                val timeText = "时间: ${formatTimestamp(watermarkData.timestamp, watermarkData.timeFormat)}"
                if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                    val fillPaint = Paint(textPaint).apply {
                        style = Paint.Style.FILL
                        color = Color.WHITE
                    }
                    canvas.drawText(timeText, adjustedStartX + paddingPx, currentY, fillPaint)
                }
                canvas.drawText(timeText, adjustedStartX + paddingPx, currentY, textPaint)
                currentY += textPaint.textSize + lineSpacingPx
            }
            
            // 绘制位置信息（根据显示设置）
            if (watermarkData.showLocation && watermarkData.latitude != 0.0 && watermarkData.longitude != 0.0) {
                val locationText = "经纬度: ${String.format("%.6f°N,%.6f°E", watermarkData.latitude, watermarkData.longitude)}"
                if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                    val fillPaint = Paint(textPaint).apply {
                        style = Paint.Style.FILL
                        color = Color.WHITE
                    }
                    canvas.drawText(locationText, adjustedStartX + paddingPx, currentY, fillPaint)
                }
                canvas.drawText(locationText, adjustedStartX + paddingPx, currentY, textPaint)
                currentY += textPaint.textSize + lineSpacingPx
                
                if (watermarkData.locationName.isNotEmpty()) {
                    val addressText = "地点: ${watermarkData.locationName}"
                    if (watermarkData.watermarkStyle == WatermarkStyle.BORDERED) {
                        val fillPaint = Paint(textPaint).apply {
                            style = Paint.Style.FILL
                            color = Color.WHITE
                        }
                        canvas.drawText(addressText, adjustedStartX + paddingPx, currentY, fillPaint)
                    }
                    canvas.drawText(addressText, adjustedStartX + paddingPx, currentY, textPaint)
                    currentY += textPaint.textSize + lineSpacingPx
                }
            }
            
            // 绘制天气信息（根据显示设置）
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
                        canvas.drawText(weatherInfo, adjustedStartX + paddingPx, currentY, fillPaint)
                    }
                    canvas.drawText(weatherInfo, adjustedStartX + paddingPx, currentY, textPaint)
                    currentY += textPaint.textSize + lineSpacingPx
                }
            }

            // 恢复画布状态
            canvas.restore()
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
        folderName: String,
        rotationDegrees: Float = 0f
    ): Boolean {
        return try {
            val watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(watermarkedBitmap)

            // 根据旋转角度调整水印位置映射（与CameraScreen.kt保持一致）
            val adjustedPosition = when (rotationDegrees.toInt()) {
                0 -> watermarkData.watermarkPosition
                90 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_LEFT
                }
                180 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_RIGHT
                }
                270 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                }
                else -> watermarkData.watermarkPosition
            }

            drawWatermarkToCanvas(
                canvas = canvas,
                watermarkData = watermarkData,
                width = watermarkedBitmap.width,
                height = watermarkedBitmap.height,
                showBackground = watermarkData.watermarkStyle != WatermarkStyle.SIMPLE,
                rotationDegrees = rotationDegrees,
                adjustedPosition = adjustedPosition
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
        outputFile: File,
        rotationDegrees: Float = 0f
    ): Boolean {
        return try {
            val watermarkedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(watermarkedBitmap)
            
            // 根据旋转角度调整水印位置映射（与CameraScreen.kt保持一致）
            val adjustedPosition = when (rotationDegrees.toInt()) {
                0 -> watermarkData.watermarkPosition
                90 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_LEFT
                }
                180 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_RIGHT
                }
                270 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                }
                else -> watermarkData.watermarkPosition
            }
            
            drawWatermarkToCanvas(
                canvas = canvas,
                watermarkData = watermarkData,
                width = watermarkedBitmap.width,
                height = watermarkedBitmap.height,
                showBackground = watermarkData.watermarkStyle != WatermarkStyle.SIMPLE,
                rotationDegrees = rotationDegrees,
                adjustedPosition = adjustedPosition
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
        folderName: String,
        rotationDegrees: Float = 0f
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
            
            // 根据旋转角度调整水印位置映射（与CameraScreen.kt保持一致）
            val adjustedPosition = when (rotationDegrees.toInt()) {
                0 -> watermarkData.watermarkPosition
                90 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_LEFT
                }
                180 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_RIGHT
                }
                270 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                }
                else -> watermarkData.watermarkPosition
            }
            
            // 添加水印
            drawWatermarkToCanvas(
                canvas = canvas,
                watermarkData = watermarkData,
                width = editableBitmap.width,
                height = editableBitmap.height,
                showBackground = watermarkData.watermarkStyle != WatermarkStyle.SIMPLE,
                rotationDegrees = rotationDegrees,
                adjustedPosition = adjustedPosition
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
        watermarkData: WatermarkData,
        rotationDegrees: Float = 0f
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
            
            // 根据旋转角度调整水印位置映射（与CameraScreen.kt保持一致）
            val adjustedPosition = when (rotationDegrees.toInt()) {
                0 -> watermarkData.watermarkPosition
                90 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_LEFT
                }
                180 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_RIGHT
                }
                270 -> when (watermarkData.watermarkPosition) {
                    WatermarkPosition.TOP_LEFT -> WatermarkPosition.BOTTOM_LEFT
                    WatermarkPosition.TOP_RIGHT -> WatermarkPosition.TOP_LEFT
                    WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.TOP_RIGHT
                    WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.BOTTOM_RIGHT
                }
                else -> watermarkData.watermarkPosition
            }
            
            // 添加水印
            drawWatermarkToCanvas(
                canvas = canvas,
                watermarkData = watermarkData,
                width = editableBitmap.width,
                height = editableBitmap.height,
                showBackground = watermarkData.watermarkStyle != WatermarkStyle.SIMPLE,
                rotationDegrees = rotationDegrees,
                adjustedPosition = adjustedPosition
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