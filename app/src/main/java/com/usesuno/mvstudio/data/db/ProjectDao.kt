package com.usesuno.mvstudio.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity): Int

    @Delete
    suspend fun delete(project: ProjectEntity): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isDraft = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDraft(): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isDraft = 0 ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isDraft = :isDraft ORDER BY updatedAt DESC")
    fun getProjectsByDraft(isDraft: Boolean): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects WHERE isDraft = 0")
    suspend fun getProjectCount(): Int

    @Transaction
    suspend fun upsertWithAssets(project: ProjectEntity, assets: List<AssetEntity>) {
        insert(project)
        if (assets.isNotEmpty()) {
            assetDao.insertAll(assets)
        }
    }
}