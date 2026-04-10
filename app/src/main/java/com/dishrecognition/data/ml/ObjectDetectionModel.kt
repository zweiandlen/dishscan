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

@Singleton
class ObjectDetectionModel @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val labels = listOf(
        "dish",
        "small_bowl", "medium_bowl", "large_bowl",
        "small_plate", "medium_plate", "large_plate", "long_plate"
    )
    
    companion object {
        private const val MODEL_FILE = "yolov5s.tflite"
        private const val INPUT_SIZE = 640
        private const val NUM_DETECTIONS = 2535
        private const val NUM_CLASSES = 8
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
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.length)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interpreter = this.interpreter ?: return emptyList()
        
        val inputBuffer = preprocessBitmap(bitmap)
        val outputArray = Array(1) { Array(NUM_DETECTIONS) { FloatArray(5 + NUM_CLASSES) } }
        
        interpreter.run(inputBuffer, outputArray)
        
        return parseOutput(outputArray[0], bitmap.width, bitmap.height)
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
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

    private fun parseOutput(output: Array<FloatArray>, imgWidth: Int, imgHeight: Int): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val confidenceThreshold = 0.5f
        
        for (detection in output) {
            val objConfidence = detection[4]
            if (objConfidence < confidenceThreshold) continue
            
            val classProbs = detection.copyOfRange(5, 5 + NUM_CLASSES)
            var maxClassProb = Float.MIN_VALUE
            var classId = 0
            for (i in classProbs.indices) {
                if (classProbs[i] > maxClassProb) {
                    maxClassProb = classProbs[i]
                    classId = i
                }
            }
            
            val confidence = objConfidence * maxClassProb
            if (confidence < confidenceThreshold) continue
            if (classId < 0 || classId >= labels.size) continue
            
            val cx = detection[0]
            val cy = detection[1]
            val w = detection[2]
            val h = detection[3]
            
            val left = ((cx - w / 2) / INPUT_SIZE * imgWidth).coerceIn(0f, imgWidth.toFloat())
            val top = ((cy - h / 2) / INPUT_SIZE * imgHeight).coerceIn(0f, imgHeight.toFloat())
            val right = ((cx + w / 2) / INPUT_SIZE * imgWidth).coerceIn(0f, imgWidth.toFloat())
            val bottom = ((cy + h / 2) / INPUT_SIZE * imgHeight).coerceIn(0f, imgHeight.toFloat())
            
            results.add(
                DetectionResult(
                    boundingBox = BoundingBox(left, top, right, bottom),
                    label = labels[classId],
                    confidence = confidence
                )
            )
        }
        
        return results.sortedByDescending { it.confidence }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
