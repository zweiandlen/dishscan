package com.dishrecognition.domain.model

data class DetectionResult(
    val boundingBox: BoundingBox,
    val label: String,
    val confidence: Float
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
