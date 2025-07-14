package com.lwr.watermarkcamera.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FolderManager(private val context: Context) {
    
    companion object {
        private const val BASE_FOLDER_NAME = "WatermarkCamera"
        private const val SEQUENCE_FILE_NAME = "sequence.txt"
    }
    
    /**
     * 创建项目文件夹（在应用私有目录中存储序列号文件）
     */
    fun createProjectFolder(folderName: String): File? {
        return try {
            // 序列号文件存储在应用私有目录
            val baseDir = File(context.getExternalFilesDir(null), BASE_FOLDER_NAME)
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            
            val projectFolder = File(baseDir, folderName)
            if (!projectFolder.exists()) {
                projectFolder.mkdirs()
                // 初始化序列号文件
                initSequenceFile(projectFolder)
            }
            
            projectFolder
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 初始化序列号文件
     */
    private fun initSequenceFile(folder: File) {
        val sequenceFile = File(folder, SEQUENCE_FILE_NAME)
        if (!sequenceFile.exists()) {
            sequenceFile.writeText("0")
        }
    }
    
    /**
     * 获取下一个序列号
     */
    fun getNextSequenceNumber(folder: File): Int {
        val sequenceFile = File(folder, SEQUENCE_FILE_NAME)
        return try {
            val currentSequence = sequenceFile.readText().toIntOrNull() ?: 0
            val nextSequence = currentSequence + 1
            sequenceFile.writeText(nextSequence.toString())
            nextSequence
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }
    
    /**
     * 获取下一个序列号（基于现有文件数量）
     */
    fun getNextSequenceNumberFromFiles(folderName: String): Int {
        val files = getImageFiles(folderName)
        return files.size + 1
    }
    
    /**
     * 生成文件名
     */
    fun generateFileName(sequenceNumber: Int, timestamp: Long, title: String = "", imageName: String = ""): String {
        // 标题和图片名称都是必填的，所以直接组合
        val baseName = "${title}-${imageName}"
        return "${baseName}.jpg"
    }
    
    /**
     * 保存照片到Pictures目录（用户可见）
     */
    fun savePhotoToPictures(bitmap: android.graphics.Bitmap, fileName: String, folderName: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$BASE_FOLDER_NAME/$folderName")
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { imageUri ->
                context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
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
     * 获取所有项目文件夹（从应用私有目录读取）
     */
    fun getAllProjectFolders(): List<File> {
        val baseDir = File(context.getExternalFilesDir(null), BASE_FOLDER_NAME)
        return if (baseDir.exists()) {
            baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 获取指定文件夹中的所有图片文件
     */
    fun getImageFiles(folderName: String): List<File> {
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Pictures/$BASE_FOLDER_NAME/$folderName%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        val imageList = mutableListOf<File>()
        
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                imageList.add(File(name))
            }
        }
        
        return imageList
    }
} 