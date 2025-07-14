package com.lwr.watermarkcamera.ui

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.lwr.watermarkcamera.data.ProjectFolder
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderSelectionScreen(
    existingFolders: List<File>,
    onCreateNewFolder: (String) -> Unit,
    onSelectExistingFolder: (File, String?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    projectFolders: Flow<List<ProjectFolder>>
) {
    val context = LocalContext.current
    var newFolderName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var selectedFolderForPreview by remember { mutableStateOf<File?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var currentFolder by remember { mutableStateOf<File?>(null) }
    
    // 获取每个文件夹的照片信息
    val folderPhotos = remember(existingFolders) {
        existingFolders.associate { folder ->
            // 第一步：查找序号为1的照片来获取标题
            var folderTitle: String? = null
            val titleSelection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            val titleSelectionArgs = arrayOf(
                "%Pictures/WatermarkCamera/${folder.name}%",
                "%-1.jpg"
            )
            
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                titleSelection,
                titleSelectionArgs,
                null
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(nameColumn)
                    folderTitle = name.substringBeforeLast("-1.jpg")
                }
            }

            // 第二步：查询文件夹中的所有照片
            val imageUris = mutableListOf<Uri>()
            val allPhotosSelection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val allPhotosSelectionArgs = arrayOf("%Pictures/WatermarkCamera/${folder.name}%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME
                ),
                allPhotosSelection,
                allPhotosSelectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    imageUris.add(contentUri)
                }
            }
            
            folder to Pair(imageUris, folderTitle)
        }
    }
    
    // 预览对话框
    selectedFolderForPreview?.let { folder ->
        if (showPreviewDialog) {
            Dialog(
                onDismissRequest = { showPreviewDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 标题栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = folder.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                folderPhotos[folder]?.second?.let { title ->
                                    Text(
                                        text = "标题：$title",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = { showPreviewDialog = false }) {
                                Text("关闭")
                            }
                        }
                        
                        // 图片网格
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(folderPhotos[folder]?.first ?: emptyList()) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedImageIndex = (folderPhotos[folder]?.first ?: emptyList()).indexOf(uri)
                                            currentFolder = folder
                                            showPreview = true
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        // 底部按钮
                        Button(
                            onClick = { 
                                showPreviewDialog = false
                                onSelectExistingFolder(folder, folderPhotos[folder]?.second)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("使用此文件夹")
                        }
                    }
                }
            }
        }
    }
    
    if (showPreview && currentFolder != null) {
        Dialog(
            onDismissRequest = { showPreview = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val pagerState = rememberPagerState(
                    initialPage = selectedImageIndex
                ) { folderPhotos[currentFolder]?.first?.size ?: 0 }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 8.dp // 减小页面间距
                ) { page ->
                    AsyncImage(
                        model = folderPhotos[currentFolder]?.first?.get(page),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // 顶部工具栏（半透明黑色背景）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showPreview = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "${pagerState.currentPage + 1}/${folderPhotos[currentFolder]?.first?.size ?: 0}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // 占位，保持对称
                        Box(modifier = Modifier.size(48.dp))
                    }
                }

                // 底部页面指示器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(vertical = 16.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val totalPages = folderPhotos[currentFolder]?.first?.size ?: 0
                    repeat(totalPages) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) 
                                        Color.White 
                                    else 
                                        Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "水印相机",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("搜索文件夹") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            singleLine = true
        )
        
        // 创建新文件夹
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "创建新项目文件夹",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { 
                        newFolderName = it
                        showError = false
                    },
                    label = { Text("输入文件夹名称") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError
                )
                
                if (showError) {
                    Text(
                        text = "请输入有效的文件夹名称",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Button(
                    onClick = {
                        if (newFolderName.trim().isNotEmpty()) {
                            onCreateNewFolder(newFolderName.trim())
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("创建文件夹")
                }
            }
        }
        
        // 现有文件夹列表
        if (existingFolders.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "选择现有项目文件夹",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val folders by projectFolders.collectAsState(initial = emptyList())
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(folders) { projectFolder ->
                            val folder = File(context.getExternalFilesDir(null), "WatermarkCamera/${projectFolder.name}")
                            ListItem(
                                headlineContent = { Text(projectFolder.name) },
                                supportingContent = { 
                                    Column {
                                        projectFolder.title?.let { title ->
                                            Text(
                                                "标题: $title",
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            "照片数量: ${folderPhotos[folder]?.first?.size ?: 0}",
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    val photoCount = folderPhotos[folder]?.first?.size ?: 0
                                    if (photoCount == 0) {
                                        onSelectExistingFolder(folder, null)
                                    } else {
                                        selectedFolderForPreview = folder
                                        showPreviewDialog = true
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
} 