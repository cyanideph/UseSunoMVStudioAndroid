package com.usesuno.mvstudio.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.usesuno.mvstudio.domain.model.AssetType
import com.usesuno.mvstudio.data.db.converters.Converters

@Entity(tableName = "assets")
@TypeConverters(Converters::class)
data class AssetEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val type: AssetType,
    val mimeType: String,
    val data: ByteArray,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)