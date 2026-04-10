package com.dishrecognition.domain.model

data class Dish(
    val id: Long = 0,
    val name: String,
    val featureVector: FloatArray,
    val imagePaths: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Dish
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "Dish(id=$id, name='$name', imagePaths=$imagePaths)"
    }
}
