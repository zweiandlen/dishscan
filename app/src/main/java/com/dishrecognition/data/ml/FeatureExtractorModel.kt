package com.dishrecognition.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
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
    private val interpreter: Interpreter? = null
    private val inputSize = 224
    private val outputSize = 1280

    fun extractFeatures(imagePath: String): FloatArray {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return FloatArray(outputSize)
        
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = preprocessBitmap(resizedBitmap)
        
        val outputBuffer = Array(1) { FloatArray(outputSize) }
        interpreter?.run(inputBuffer, outputBuffer)
        
        return outputBuffer[0]
    }

    fun extractFeaturesFromBitmap(bitmap: Bitmap): FloatArray {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = preprocessBitmap(resizedBitmap)
        
        val outputBuffer = Array(1) { FloatArray(outputSize) }
        interpreter?.run(inputBuffer, outputBuffer)
        
        return outputBuffer[0]
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        
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
    }
}
