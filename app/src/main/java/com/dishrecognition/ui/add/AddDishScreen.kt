package com.dishrecognition.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDishScreen(
    onDishAdded: () -> Unit,
    viewModel: AddDishViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showCamera by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCamera = true
        }
    }
    
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onDishAdded()
        }
    }
    
    if (showCamera) {
        CameraDialogContent(
            onDismiss = { showCamera = false },
            onImageCaptured = { bitmap ->
                viewModel.addImage(bitmap)
                showCamera = false
            },
            imageCapture = imageCapture
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加新菜品") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "菜品照片",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(uiState.capturedImages) { index, bitmap ->
                        ImageThumbnail(
                            bitmap = bitmap,
                            onRemove = { viewModel.removeImage(index) }
                        )
                    }
                    
                    if (uiState.capturedImages.size < 5) {
                        item {
                            AddPhotoButton(
                                onClick = {
                                    val permission = Manifest.permission.CAMERA
                                    if (ContextCompat.checkSelfPermission(context, permission) 
                                        == PackageManager.PERMISSION_GRANTED) {
                                        showCamera = true
                                    } else {
                                        launcher.launch(permission)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "建议拍摄 3-5 张不同角度的照片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Divider()
            
            OutlinedTextField(
                value = uiState.dishName,
                onValueChange = { viewModel.updateDishName(it) },
                label = { Text("菜品名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Divider()
            
            Text(
                text = "营养成分 (每100g)",
                style = MaterialTheme.typography.titleMedium
            )
            
            NutritionInputField(
                label = "热量 (kcal)",
                value = uiState.calories,
                onValueChange = { viewModel.updateCalories(it) }
            )
            
            NutritionInputField(
                label = "脂肪 (g)",
                value = uiState.fat,
                onValueChange = { viewModel.updateFat(it) }
            )
            
            NutritionInputField(
                label = "蛋白质 (g)",
                value = uiState.protein,
                onValueChange = { viewModel.updateProtein(it) }
            )
            
            NutritionInputField(
                label = "碳水化合物 (g)",
                value = uiState.carbohydrate,
                onValueChange = { viewModel.updateCarbohydrate(it) }
            )
            
            NutritionInputField(
                label = "维生素 (mg)",
                value = uiState.vitamin,
                onValueChange = { viewModel.updateVitamin(it) }
            )
            
            NutritionInputField(
                label = "钠 (mg)",
                value = uiState.sodium,
                onValueChange = { viewModel.updateSodium(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.saveDish() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存菜品")
                }
            }
            
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ImageThumbnail(
    bitmap: Bitmap,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "菜品照片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AddPhotoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "添加照片",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun NutritionInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.toFloatOrNull() != null) {
                onValueChange(newValue)
            }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
fun CameraDialogContent(
    onDismiss: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit,
    imageCapture: ImageCapture
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("拍摄菜品照片") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    imageCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
                                onImageCaptured(rotatedBitmap)
                                image.close()
                            }
                            
                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                }
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("拍照")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
