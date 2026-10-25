package com.usesuno.mvstudio.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.data.db.converters.Converters

@Entity(tableName = "projects")
@TypeConverters(Converters::class)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val projectJson: String, // Full project serialized as JSON
    val audioId: String? = null,
    val audioDuration: Double,
    val template: String,
    val background: String,
    val aspectRatio: String,
    val resolution: String,
    val thumbnailPath: String? = null,
    val isDraft: Boolean = false,
)