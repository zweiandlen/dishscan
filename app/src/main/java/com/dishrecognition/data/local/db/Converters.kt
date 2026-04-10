package com.dishrecognition.data.local.db

import androidx.room.TypeConverter
import java.util.Arrays

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        return if (value.isEmpty()) {
            FloatArray(0)
        } else {
            value.split(",").map { it.toFloat() }.toFloatArray()
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString("|")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split("|")
        }
    }
}
