package com.dishrecognition.data.ml

import android.content.Context
import android.graphics.Bitmap
import com.dishrecognition.domain.model.BoundingBox
import com.dishrecognition.domain.model.DetectionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class ObjectDetectionModel @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    
    companion object {
        private const val MODEL_FILE = "yolov8n.tflite"
        private const val INPUT_SIZE = 640
        private const val NUM_CLASSES = 8
    }
    
    private val labels = listOf(
        "dish",
        "small_bowl", "medium_bowl", "large_bowl",
        "small_plate", "medium_plate", "large_plate", "long_plate"
    )

    init {
        try {
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.length
        )
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interpreter = this.interpreter ?: return emptyList()
        
        val inputBuffer = preprocessBitmap(bitmap)
        
        // YOLOv8 TFLite 输出格式: [1, 84, 8400] 
        // 84 = 4 (x, y, w, h) + 80 (classes, 或自定义 num_classes)
        // 8400 = 80*80 + 40*40 + 20*20 (多尺度特征图)
        val outputBuffer = Array(1) { Array(INPUT_SIZE / 8 * INPUT_SIZE / 8 + INPUT_SIZE / 16 * INPUT_SIZE / 16 + INPUT_SIZE / 32 * INPUT_SIZE / 32) { FloatArray(4 + NUM_CLASSES) } }
        
        interpreter.run(inputBuffer, outputBuffer)
        
        return parseYOLOv8Output(outputBuffer[0], bitmap.width, bitmap.height)
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (pixel in pixels) {
            // YOLOv8 使用 RGB 格式，范围 [0, 1]
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        
        return inputBuffer
    }

    private fun parseYOLOv8Output(
        output: Array<FloatArray>,
        imgWidth: Int,
        imgHeight: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val confidenceThreshold = 0.5f
        
        // YOLOv8 输出格式: [num_predictions, 4 + num_classes]
        // 每行: [x, y, w, h, class0_score, class1_score, ...]
        for (detection in output) {
            // 找到最大类别分数
            var maxClassScore = 0f
            var maxClassId = 0
            for (i in 0 until NUM_CLASSES) {
                if (detection[4 + i] > maxClassScore) {
                    maxClassScore = detection[4 + i]
                    maxClassId = i
                }
            }
            
            // 物体置信度已经包含在 x, y, w, h 之后
            // 这里假设 detection[4] 是物体置信度，detection[5:] 是类别分数
            // 实际上 YOLOv8 的输出可能是 [x, y, w, h, obj_conf, ...class_scores]
            val objConfidence = detection[4]
            val classConfidence = maxClassScore
            val confidence = objConfidence * classConfidence
            
            if (confidence < confidenceThreshold) continue
            
            val cx = detection[0]
            val cy = detection[1]
            val w = detection[2]
            val h = detection[3]
            
            // 转换为像素坐标
            val left = ((cx - w / 2) / INPUT_SIZE * imgWidth).coerceIn(0f, imgWidth.toFloat())
            val top = ((cy - h / 2) / INPUT_SIZE * imgHeight).coerceIn(0f, imgHeight.toFloat())
            val right = ((cx + w / 2) / INPUT_SIZE * imgWidth).coerceIn(0f, imgWidth.toFloat())
            val bottom = ((cy + h / 2) / INPUT_SIZE * imgHeight).coerceIn(0f, imgHeight.toFloat())
            
            if (right <= left || bottom <= top) continue
            
            results.add(
                DetectionResult(
                    boundingBox = BoundingBox(left, top, right, bottom),
                    label = labels[maxClassId],
                    confidence = confidence
                )
            )
        }
        
        // 按置信度排序
        return results.sortedByDescending { it.confidence }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
