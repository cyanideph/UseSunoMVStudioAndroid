package com.usesuno.mvstudio.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.usesuno.mvstudio.domain.model.AssetType
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assets: List<AssetEntity>): List<Long>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getById(id: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE projectId = :projectId")
    fun getByProjectId(projectId: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE projectId = :projectId AND type = :type")
    suspend fun getByProjectIdAndType(projectId: String, type: AssetType): List<AssetEntity>

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM assets WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: String): Int
}