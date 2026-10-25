package com.usesuno.mvstudio.data.db.converters

import androidx.room.TypeConverter
import com.usesuno.mvstudio.domain.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTemplate(value: String?): Template? = value?.let { Template.valueOf(it) }

    @TypeConverter
    fun toTemplate(value: Template?): String? = value?.name

    @TypeConverter
    fun fromBackground(value: String?): Background? = value?.let { Background.valueOf(it) }

    @TypeConverter
    fun toBackground(value: Background?): String? = value?.name

    @TypeConverter
    fun fromAspectRatio(value: String?): AspectRatio? = value?.let { AspectRatio.valueOf(it) }

    @TypeConverter
    fun toAspectRatio(value: AspectRatio?): String? = value?.name

    @TypeConverter
    fun fromResolution(value: String?): Resolution? = value?.let { Resolution.valueOf(it) }

    @TypeConverter
    fun toResolution(value: Resolution?): String? = value?.name

    @TypeConverter
    fun fromExportFormat(value: String?): ExportFormat? = value?.let { ExportFormat.valueOf(it) }

    @TypeConverter
    fun toExportFormat(value: ExportFormat?): String? = value?.name

    @TypeConverter
    fun fromExportQuality(value: String?): ExportQuality? = value?.let { ExportQuality.valueOf(it) }

    @TypeConverter
    fun toExportQuality(value: ExportQuality?): String? = value?.name

    @TypeConverter
    fun fromIntArray(value: String?): List<Int>? = value?.let { json.decodeFromString<List<Int>>(it) }

    @TypeConverter
    fun toIntArray(value: List<Int>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromFloatArray(value: String?): List<Float>? = value?.let { json.decodeFromString<List<Float>>(it) }

    @TypeConverter
    fun toFloatArray(value: List<Float>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? = value?.let { json.decodeFromString<List<String>>(it) }

    @TypeConverter
    fun toStringList(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromMapStringFloat(value: String?): Map<String, Float>? = value?.let { json.decodeFromString<Map<String, Float>>(it) }

    @TypeConverter
    fun toMapStringFloat(value: Map<String, Float>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromAudioReactivityConfig(value: String?): AudioReactivityConfig? = value?.let { json.decodeFromString<AudioReactivityConfig>(it) }

    @TypeConverter
    fun toAudioReactivityConfig(value: AudioReactivityConfig?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromBackgroundConfig(value: String?): BackgroundConfig? = value?.let { json.decodeFromString<BackgroundConfig>(it) }

    @TypeConverter
    fun toBackgroundConfig(value: BackgroundConfig?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromTemplateTuning(value: String?): TemplateTuning? = value?.let { json.decodeFromString<TemplateTuning>(it) }

    @TypeConverter
    fun toTemplateTuning(value: TemplateTuning?): String? = value?.let { json.encodeToString(it) }
}