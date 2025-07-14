package com.lwr.watermarkcamera.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class LocationService(private val context: Context) {
    
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val geocoder = Geocoder(context, Locale.getDefault())
    
    private var currentLocation: Location? = null
    private var locationCallback: ((Location) -> Unit)? = null
    
    /**
     * 检查位置权限
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 开始获取位置
     */
    fun startLocationUpdates(callback: (Location) -> Unit) {
        if (!hasLocationPermission()) {
            return
        }
        
        locationCallback = callback
        
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentLocation = location
                locationCallback?.invoke(location)
            }
            
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // 1秒更新一次
                    1f, // 1米精度
                    locationListener
                )
            }
            
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    locationListener
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    /**
     * 停止位置更新
     */
    fun stopLocationUpdates() {
        locationManager.removeUpdates(object : LocationListener {
            override fun onLocationChanged(location: Location) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        })
    }
    
    /**
     * 获取当前位置
     */
    fun getCurrentLocation(): Location? = currentLocation
    
    /**
     * 根据坐标获取地址信息
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val addressParts = mutableListOf<String>()
                    
                    address.locality?.let { addressParts.add(it) } // 城市
                    address.subLocality?.let { addressParts.add(it) } // 区县
                    address.thoroughfare?.let { addressParts.add(it) } // 街道
                    
                    if (addressParts.isNotEmpty()) {
                        addressParts.joinToString("")
                    } else {
                        "未知位置"
                    }
                } else {
                    "未知位置"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "未知位置"
            }
        }
    }
    
    /**
     * 获取方向信息
     */
    fun getDirection(azimuth: Float): String {
        return when {
            azimuth in 337.5..360.0 || azimuth in 0.0..22.5 -> "北"
            azimuth in 22.5..67.5 -> "东北"
            azimuth in 67.5..112.5 -> "东"
            azimuth in 112.5..157.5 -> "东南"
            azimuth in 157.5..202.5 -> "南"
            azimuth in 202.5..247.5 -> "西南"
            azimuth in 247.5..292.5 -> "西"
            azimuth in 292.5..337.5 -> "西北"
            else -> "未知"
        }
    }
} 