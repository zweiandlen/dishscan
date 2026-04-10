package com.dishrecognition.domain.model

enum class TablewareType(val displayName: String, val defaultWeight: Float) {
    SMALL_BOWL("小碗", 150f),
    MEDIUM_BOWL("中碗", 250f),
    LARGE_BOWL("大碗", 400f),
    SMALL_PLATE("小盘", 100f),
    MEDIUM_PLATE("中盘", 200f),
    LARGE_PLATE("大盘", 350f),
    LONG_PLATE("长盘", 300f);

    companion object {
        fun fromString(value: String): TablewareType {
            return entries.find { it.name == value } ?: MEDIUM_BOWL
        }
    }
}
