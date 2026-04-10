package com.dishrecognition.data.ml

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureExtractorModel @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    
    companion object {
        private const val MODEL_FILE = "efficientnetb0_feature.tflite"
        private const val INPUT_SIZE = 224
        private const val OUTPUT_SIZE = 1280
    }

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

    fun extractFeatures(imagePath: String): FloatArray {
        val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            ?: return FloatArray(OUTPUT_SIZE)
        
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        return extractFeaturesFromBitmap(resizedBitmap)
    }

    fun extractFeaturesFromBitmap(bitmap: Bitmap): FloatArray {
        val interpreter = this.interpreter ?: return FloatArray(OUTPUT_SIZE)
        
        val inputBuffer = preprocessBitmap(bitmap)
        val outputBuffer = Array(1) { FloatArray(OUTPUT_SIZE) }
        
        interpreter.run(inputBuffer, outputBuffer)
        
        return outputBuffer[0]
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        // EfficientNet 使用 RGB 格式，范围 [0, 1]
        // ImageNet 预训练模型通常使用 normalize = (pixel - mean) / std
        // 这里使用简单的归一化
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        
        return inputBuffer
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
