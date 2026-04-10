package com.dishrecognition.data.local.db

import androidx.room.*
import com.dishrecognition.data.local.entity.TablewareMappingEntity

@Dao
interface TablewareMappingDao {
    @Query("SELECT * FROM tableware_mapping WHERE tablewareType = :type")
    suspend fun getMappingByType(type: String): TablewareMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: TablewareMappingEntity)

    @Query("SELECT * FROM tableware_mapping")
    suspend fun getAllMappings(): List<TablewareMappingEntity>
}
