package com.dishrecognition.data.ml

import android.content.Context
import com.dishrecognition.domain.model.BoundingBox
import com.dishrecognition.domain.model.DetectionResult
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
class ObjectDetectionModel @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val interpreter: Interpreter? = null
    private val labels = listOf(
        "small_bowl", "medium_bowl", "large_bowl",
        "small_plate", "medium_plate", "large_plate", "long_plate"
    )

    fun detect(frame: ByteBuffer, width: Int, height: Int): List<DetectionResult> {
        val inputBuffer = preprocessFrame(frame, width, height)
        val outputBuffer = Array(1) { Array(2535) { FloatArray(7) } }
        
        interpreter?.run(inputBuffer, outputBuffer)
        
        return parseOutput(outputBuffer[0])
    }

    private fun preprocessFrame(frame: ByteBuffer, width: Int, height: Int): ByteBuffer {
        val inputSize = 640
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val scaledBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
        scaledBuffer.order(ByteOrder.nativeOrder())
        
        scaleImage(frame, width, height, scaledBuffer, inputSize)
        
        scaledBuffer.rewind()
        while (scaledBuffer.hasRemaining()) {
            val pixel = scaledBuffer.int
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        
        return inputBuffer
    }

    private fun scaleImage(
        input: ByteBuffer, inputW: Int, inputH: Int,
        output: ByteBuffer, outputSize: Int
    ) {
        val scaleX = outputSize.toFloat() / inputW
        val scaleY = outputSize.toFloat() / inputH
        
        for (y in 0 until outputSize) {
            for (x in 0 until outputSize) {
                val srcX = (x / scaleX).toInt().coerceIn(0, inputW - 1)
                val srcY = (y / scaleY).toInt().coerceIn(0, inputH - 1)
                
                val offset = (srcY * inputW + srcX) * 4
                output.putInt(input.getInt(offset))
            }
        }
    }

    private fun parseOutput(output: Array<FloatArray>): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val confidenceThreshold = 0.5f
        
        for (detection in output) {
            val confidence = detection[4]
            if (confidence < confidenceThreshold) continue
            
            val classId = detection[5].toInt()
            if (classId < 0 || classId >= labels.size) continue
            
            val x = detection[0]
            val y = detection[1]
            val w = detection[2]
            val h = detection[3]
            
            results.add(
                DetectionResult(
                    boundingBox = BoundingBox(
                        left = (x - w / 2) / 640f,
                        top = (y - h / 2) / 640f,
                        right = (x + w / 2) / 640f,
                        bottom = (y + h / 2) / 640f
                    ),
                    label = labels[classId],
                    confidence = confidence
                )
            )
        }
        
        return results
    }

    fun close() {
        interpreter?.close()
    }
}
