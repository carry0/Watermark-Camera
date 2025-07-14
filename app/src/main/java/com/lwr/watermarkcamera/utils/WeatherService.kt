package com.lwr.watermarkcamera.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class WeatherService {
    
    private val client = OkHttpClient()
    
    // 使用WeatherAPI.com的API
    private val weatherApiUrl = "http://api.weatherapi.com/v1/current.json"
    private val apiKey = "7a09a0a435af44ddb0e32351251407"
    
    data class WeatherInfo(
        val weather: String = "未知",
        val temperature: String = "未知"
    )
    
    /**
     * 获取天气信息
     */
    suspend fun getWeatherInfo(latitude: Double, longitude: Double): WeatherInfo {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$weatherApiUrl?key=$apiKey&q=$latitude,$longitude&lang=zh"
                val request = Request.Builder().url(url).build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonString = response.body?.string()
                        parseWeatherJson(jsonString)
                    } else {
                        WeatherInfo()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                WeatherInfo()
            }
        }
    }
    
    /**
     * 解析天气JSON数据
     */
    private fun parseWeatherJson(jsonString: String?): WeatherInfo {
        return try {
            if (jsonString.isNullOrEmpty()) {
                return WeatherInfo()
            }
            
            val json = JSONObject(jsonString)
            val current = json.getJSONObject("current")
            val condition = current.getJSONObject("condition")
            
            val weather = condition.getString("text")
            val temperature = "${current.getDouble("temp_c").toInt()}℃"
            
            WeatherInfo(weather, temperature)
        } catch (e: Exception) {
            e.printStackTrace()
            WeatherInfo()
        }
    }
    
    /**
     * 获取模拟天气信息（用于测试）
     */
    fun getMockWeatherInfo(): WeatherInfo {
        return WeatherInfo(
            weather = "阴天",
            temperature = "26℃"
        )
    }
} 