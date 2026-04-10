package com.dishrecognition.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tableware_mapping")
data class TablewareMappingEntity(
    @PrimaryKey val tablewareType: String,
    val defaultWeight: Float
)
