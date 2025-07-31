package com.lwr.watermarkcamera.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlin.math.hypot

/**
 * 管理后置摄像头的变焦倍数范围
 */
class CameraZoomManager(context: Context) {

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /**
     * 获取所有后置摄像头的变焦范围
     * @return Pair<Float, Float> 最小倍数和最大倍数，若无后置摄像头则返回 null
     */
    fun getBackCamerasZoomRange(): Pair<Float, Float>? {
        val zoomRanges = mutableListOf<Float>()

        try {
            // 遍历所有摄像头
            cameraManager.cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                // 筛选后置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK) {

                    // 获取物理焦距和传感器尺寸
                    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

                    if (focalLengths != null && sensorSize != null) {
                        // 计算最小/最大倍数（焦距 / 传感器对角线尺寸）
                        val sensorDiagonal =
                            hypot(sensorSize.width.toDouble(), sensorSize.height.toDouble())
                        focalLengths.forEach { focalLength ->
                            val zoom = (focalLength / sensorDiagonal).toFloat()
                            zoomRanges.add(zoom)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CameraZoomManager", "Error accessing cameras: ${e.message}")
        }

        return if (zoomRanges.isNotEmpty()) {
            Pair(zoomRanges.min(), zoomRanges.max())
        } else {
            null // 无后置摄像头
        }
    }
}