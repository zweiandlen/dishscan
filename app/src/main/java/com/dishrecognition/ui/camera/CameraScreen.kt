@file:OptIn(ExperimentalMaterial3Api::class)

package com.dishrecognition.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.dishrecognition.domain.model.TablewareType
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                onFrameAnalyzed = { bitmap ->
                    viewModel.processFrame(bitmap)
                }
            )
            
            RecognitionOverlay(
                recognitionResult = uiState.recognitionResult,
                isProcessing = uiState.isProcessing,
                onTablewareTypeChange = { viewModel.updateTablewareType(it) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("需要相机权限才能进行菜品识别")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("请求权限")
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onFrameAnalyzed: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                            onFrameAnalyzed(scaledBitmap)
                            imageProxy.close()
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
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

@Composable
fun RecognitionOverlay(
    recognitionResult: com.dishrecognition.domain.model.RecognitionResult?,
    isProcessing: Boolean,
    onTablewareTypeChange: (TablewareType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        
        recognitionResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = result.dish?.name ?: "未识别到菜品",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "置信度: ${(result.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = "餐具: ${result.tablewareType.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = "估算重量: ${result.detectedWeight.toInt()}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    result.nutritionInfo?.let { nutrition ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.White.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "营养成分 (估算)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        
                        NutritionRow("热量", "${nutrition.calories.toInt()} kcal")
                        NutritionRow("脂肪", "${nutrition.fat.toInt()} g")
                        NutritionRow("蛋白质", "${nutrition.protein.toInt()} g")
                        NutritionRow("碳水", "${nutrition.carbohydrate.toInt()} g")
                        NutritionRow("维生素", "${nutrition.vitamin.toInt()} mg")
                        NutritionRow("钠", "${nutrition.sodium.toInt()} mg")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "选择餐具类型",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TablewareType.entries.take(4).forEach { type ->
                            FilterChip(
                                selected = result.tablewareType == type,
                                onClick = { onTablewareTypeChange(type) },
                                label = { Text(type.displayName) }
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TablewareType.entries.drop(4).forEach { type ->
                            FilterChip(
                                selected = result.tablewareType == type,
                                onClick = { onTablewareTypeChange(type) },
                                label = { Text(type.displayName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
