package com.dishrecognition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dishes")
data class DishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val featureVector: FloatArray,
    val imagePaths: List<String>,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DishEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "DishEntity(id=$id, name='$name', imagePaths=$imagePaths)"
    }
}
