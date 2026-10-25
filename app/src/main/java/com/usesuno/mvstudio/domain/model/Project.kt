package com.usesuno.mvstudio.domain.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * Core project model matching UseSunoMVStudio's domain/model.ts
 */
@Serializable
data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Audio
    val audioId: String? = null,
    val audioDuration: Double = 0.0,
    val audioWaveform: List<Float> = emptyList(),

    // Lyrics
    val lines: List<LyricLine> = emptyList(),
    val globalOffset: Double = 0.0,

    // Template & Visual
    val template: Template = Template.FUME,
    val templateTuning: TemplateTuning = TemplateTuning(),
    val background: Background = Background.CUSTOM,
    val backgroundConfig: BackgroundConfig = BackgroundConfig(),
    val aspectRatio: AspectRatio = AspectRatio.RATIO_16_9,
    val resolution: Resolution = Resolution.P1080,

    // Typography
    val fontFamily: String = "Inter",
    val fontWeight: Int = 400,
    val fontSize: Float = 1.0f,
    val lineHeight: Float = 1.2f,
    val letterSpacing: Float = 0.0f,
    val textColor: String = "#FFFFFF",
    val strokeColor: String = "#000000",
    val strokeWidth: Float = 0.0f,
    val shadowColor: String = "#00000000",
    val shadowBlur: Float = 0.0f,
    val shadowOffsetX: Float = 0.0f,
    val shadowOffsetY: Float = 0.0f,

    // Intro
    val introEnabled: Boolean = true,
    val introDuration: Double = 3.0,
    val introStyle: IntroStyle = IntroStyle.FADE,

    // Audio Reactivity
    val audioReactivityEnabled: Boolean = true,
    val audioReactivityConfig: AudioReactivityConfig = AudioReactivityConfig(),

    // Export settings
    val exportFormat: ExportFormat = ExportFormat.MP4,
    val exportFps: Int = 30,
    val exportQuality: ExportQuality = ExportQuality.HIGH,

    // Assets (stored as base64 or file references)
    val coverImageId: String? = null,
    val customBackgroundId: String? = null,
    val monetImageId: String? = null,
    val fontFileId: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        name = parcel.readString()!!,
        version = parcel.readInt(),
        createdAt = parcel.readLong(),
        updatedAt = parcel.readLong(),
        audioId = parcel.readString(),
        audioDuration = parcel.readDouble(),
        audioWaveform = parcel.createFloatArray()?.toList() ?: emptyList(),
        lines = parcel.createTypedArrayList(LyricLine.CREATOR) as List<LyricLine>,
        globalOffset = parcel.readDouble(),
        template = Template.valueOf(parcel.readString()!!),
        templateTuning = (parcel.readParcelable<TemplateTuning>(TemplateTuning::class.java.classLoader)) ?: TemplateTuning(),
        background = Background.valueOf(parcel.readString()!!),
        backgroundConfig = (parcel.readParcelable<BackgroundConfig>(BackgroundConfig::class.java.classLoader)) ?: BackgroundConfig(),
        aspectRatio = AspectRatio.valueOf(parcel.readString()!!),
        resolution = Resolution.valueOf(parcel.readString()!!),
        fontFamily = parcel.readString()!!,
        fontWeight = parcel.readInt(),
        fontSize = parcel.readFloat(),
        lineHeight = parcel.readFloat(),
        letterSpacing = parcel.readFloat(),
        textColor = parcel.readString()!!,
        strokeColor = parcel.readString()!!,
        strokeWidth = parcel.readFloat(),
        shadowColor = parcel.readString()!!,
        shadowBlur = parcel.readFloat(),
        shadowOffsetX = parcel.readFloat(),
        shadowOffsetY = parcel.readFloat(),
        introEnabled = parcel.readByte() != 0.toByte(),
        introDuration = parcel.readDouble(),
        introStyle = IntroStyle.valueOf(parcel.readString()!!),
        audioReactivityEnabled = parcel.readByte() != 0.toByte(),
        audioReactivityConfig = (parcel.readParcelable<AudioReactivityConfig>(AudioReactivityConfig::class.java.classLoader)) ?: AudioReactivityConfig(),
        exportFormat = ExportFormat.valueOf(parcel.readString()!!),
        exportFps = parcel.readInt(),
        exportQuality = ExportQuality.valueOf(parcel.readString()!!),
        coverImageId = parcel.readString(),
        customBackgroundId = parcel.readString(),
        monetImageId = parcel.readString(),
        fontFileId = parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeInt(version)
        parcel.writeLong(createdAt)
        parcel.writeLong(updatedAt)
        parcel.writeString(audioId)
        parcel.writeDouble(audioDuration)
        parcel.writeFloatArray(audioWaveform.toFloatArray())
        parcel.writeTypedList(lines)
        parcel.writeDouble(globalOffset)
        parcel.writeString(template.name)
        parcel.writeParcelable(templateTuning, flags)
        parcel.writeString(background.name)
        parcel.writeParcelable(backgroundConfig, flags)
        parcel.writeString(aspectRatio.name)
        parcel.writeString(resolution.name)
        parcel.writeString(fontFamily)
        parcel.writeInt(fontWeight)
        parcel.writeFloat(fontSize)
        parcel.writeFloat(lineHeight)
        parcel.writeFloat(letterSpacing)
        parcel.writeString(textColor)
        parcel.writeString(strokeColor)
        parcel.writeFloat(strokeWidth)
        parcel.writeString(shadowColor)
        parcel.writeFloat(shadowBlur)
        parcel.writeFloat(shadowOffsetX)
        parcel.writeFloat(shadowOffsetY)
        parcel.writeByte(if (introEnabled) 1 else 0)
        parcel.writeDouble(introDuration)
        parcel.writeString(introStyle.name)
        parcel.writeByte(if (audioReactivityEnabled) 1 else 0)
        parcel.writeParcelable(audioReactivityConfig, flags)
        parcel.writeString(exportFormat.name)
        parcel.writeInt(exportFps)
        parcel.writeString(exportQuality.name)
        parcel.writeString(coverImageId)
        parcel.writeString(customBackgroundId)
        parcel.writeString(monetImageId)
        parcel.writeString(fontFileId)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Project> = object : Parcelable.Creator<Project> {
            override fun createFromParcel(parcel: Parcel): Project = Project(parcel)
            override fun newArray(size: Int): Array<Project?> = arrayOfNulls(size)
        }
    }
}

/**
 * Lyric line with word-level timing
 */
@Serializable
data class LyricLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val start: Double = 0.0,
    val end: Double = 0.0,
    val precision: Precision = Precision.LINE,
    val words: List<LyricWord> = emptyList(),
    val isBackground: Boolean = false,
    val translation: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        text = parcel.readString()!!,
        start = parcel.readDouble(),
        end = parcel.readDouble(),
        precision = Precision.valueOf(parcel.readString()!!),
        words = parcel.createTypedArrayList(LyricWord.CREATOR) as List<LyricWord>,
        isBackground = parcel.readByte() != 0.toByte(),
        translation = parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(text)
        parcel.writeDouble(start)
        parcel.writeDouble(end)
        parcel.writeString(precision.name)
        parcel.writeTypedList(words)
        parcel.writeByte(if (isBackground) 1 else 0)
        parcel.writeString(translation)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LyricLine> = object : Parcelable.Creator<LyricLine> {
            override fun createFromParcel(parcel: Parcel): LyricLine = LyricLine(parcel)
            override fun newArray(size: Int): Array<LyricLine?> = arrayOfNulls(size)
        }
    }
}

@Serializable
data class LyricWord(
    val text: String,
    val start: Double,
    val end: Double,
    val syllableCount: Int = 1,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        text = parcel.readString()!!,
        start = parcel.readDouble(),
        end = parcel.readDouble(),
        syllableCount = parcel.readInt(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeDouble(start)
        parcel.writeDouble(end)
        parcel.writeInt(syllableCount)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<LyricWord> = object : Parcelable.Creator<LyricWord> {
            override fun createFromParcel(parcel: Parcel): LyricWord = LyricWord(parcel)
            override fun newArray(size: Int): Array<LyricWord?> = arrayOfNulls(size)
        }
    }
}

enum class Precision {
    LINE, WORD, SYLLABLE
}

enum class Template {
    FUME, CLASSIC, CADENZA, PARTITA, TILT, CLADDAGH, MONET, CAPPELLA,
    DIORAMA, AURORA_TRAVERSE, PENDOLO, TEMPERA, SONNET, STILL,
    ASTRAL, KYOTO, CHROMA, ELYSIUM, SYZYGY, LUMIERE
}

enum class Background {
    CUSTOM, THEME_SPACE, FLUID_LIGHT, AURORA_NEBULA,
    AURORA_CURTAIN, LIGHT_TIDE, SPACE_STARLIGHT
}

enum class AspectRatio {
    RATIO_16_9, RATIO_9_16, RATIO_1_1, RATIO_4_3, RATIO_3_4
}

enum class Resolution(val width: Int, val height: Int) {
    P720(1280, 720), P1080(1920, 1080), P1440(2560, 1440), P2160(3840, 2160)
}

enum class ExportFormat {
    MP4, WEBM
}

enum class ExportQuality {
    LOW, MEDIUM, HIGH, LOSSLESS
}

enum class IntroStyle {
    FADE, SLIDE_UP, SLIDE_DOWN, ZOOM, TYPEWRITER, NONE
}

@Serializable
data class TemplateTuning(
    val speed: Float = 1.0f,
    val intensity: Float = 1.0f,
    val complexity: Float = 0.5f,
    val colorShift: Float = 0.0f,
    val rotation: Float = 0.0f,
    val scale: Float = 1.0f,
    val customParams: Map<String, Float> = emptyMap(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        speed = parcel.readFloat(),
        intensity = parcel.readFloat(),
        complexity = parcel.readFloat(),
        colorShift = parcel.readFloat(),
        rotation = parcel.readFloat(),
        scale = parcel.readFloat(),
        customParams = parcel.readHashMap(String::class.java.classLoader, String::class.java, Float::class.java) as Map<String, Float>,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(speed)
        parcel.writeFloat(intensity)
        parcel.writeFloat(complexity)
        parcel.writeFloat(colorShift)
        parcel.writeFloat(rotation)
        parcel.writeFloat(scale)
        parcel.writeMap(customParams)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TemplateTuning> = object : Parcelable.Creator<TemplateTuning> {
            override fun createFromParcel(parcel: Parcel): TemplateTuning = TemplateTuning(parcel)
            override fun newArray(size: Int): Array<TemplateTuning?> = arrayOfNulls(size)
        }
    }
}

@Serializable
data class BackgroundConfig(
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val color: String = "#000000",
    val opacity: Float = 1.0f,
    val blur: Float = 0.0f,
    val scale: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val animationSpeed: Float = 1.0f,
    val customParams: Map<String, Float> = emptyMap(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        imageUrl = parcel.readString(),
        videoUrl = parcel.readString(),
        color = parcel.readString()!!,
        opacity = parcel.readFloat(),
        blur = parcel.readFloat(),
        scale = parcel.readFloat(),
        offsetX = parcel.readFloat(),
        offsetY = parcel.readFloat(),
        animationSpeed = parcel.readFloat(),
        customParams = parcel.readHashMap(String::class.java.classLoader, String::class.java, Float::class.java) as Map<String, Float>,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imageUrl)
        parcel.writeString(videoUrl)
        parcel.writeString(color)
        parcel.writeFloat(opacity)
        parcel.writeFloat(blur)
        parcel.writeFloat(scale)
        parcel.writeFloat(offsetX)
        parcel.writeFloat(offsetY)
        parcel.writeFloat(animationSpeed)
        parcel.writeMap(customParams)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BackgroundConfig> = object : Parcelable.Creator<BackgroundConfig> {
            override fun createFromParcel(parcel: Parcel): BackgroundConfig = BackgroundConfig(parcel)
            override fun newArray(size: Int): Array<BackgroundConfig?> = arrayOfNulls(size)
        }
    }
}

@Serializable
data class AudioReactivityConfig(
    val bassGain: Float = 1.0f,
    val lowMidGain: Float = 1.0f,
    val midGain: Float = 1.0f,
    val vocalGain: Float = 1.0f,
    val trebleGain: Float = 1.0f,
    val smoothing: Float = 0.5f,
    val threshold: Float = 0.1f,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        bassGain = parcel.readFloat(),
        lowMidGain = parcel.readFloat(),
        midGain = parcel.readFloat(),
        vocalGain = parcel.readFloat(),
        trebleGain = parcel.readFloat(),
        smoothing = parcel.readFloat(),
        threshold = parcel.readFloat(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(bassGain)
        parcel.writeFloat(lowMidGain)
        parcel.writeFloat(midGain)
        parcel.writeFloat(vocalGain)
        parcel.writeFloat(trebleGain)
        parcel.writeFloat(smoothing)
        parcel.writeFloat(threshold)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AudioReactivityConfig> = object : Parcelable.Creator<AudioReactivityConfig> {
            override fun createFromParcel(parcel: Parcel): AudioReactivityConfig = AudioReactivityConfig(parcel)
            override fun newArray(size: Int): Array<AudioReactivityConfig?> = arrayOfNulls(size)
        }
    }
}

/**
 * Audio analysis result (5 bands at ~30fps)
 */
@Serializable
data class AudioAnalysis(
    val sampleRate: Int,
    val duration: Double,
    val frames: List<AudioFrame>,
    val waveform: List<Float>,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        sampleRate = parcel.readInt(),
        duration = parcel.readDouble(),
        frames = parcel.createTypedArrayList(AudioFrame.CREATOR) as List<AudioFrame>,
        waveform = parcel.createFloatArray()?.toList() ?: emptyList(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(sampleRate)
        parcel.writeDouble(duration)
        parcel.writeTypedList(frames)
        parcel.writeFloatArray(waveform.toFloatArray())
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AudioAnalysis> = object : Parcelable.Creator<AudioAnalysis> {
            override fun createFromParcel(parcel: Parcel): AudioAnalysis = AudioAnalysis(parcel)
            override fun newArray(size: Int): Array<AudioAnalysis?> = arrayOfNulls(size)
        }
    }
}

@Serializable
data class AudioFrame(
    val time: Double,
    val bass: Float,
    val lowMid: Float,
    val mid: Float,
    val vocal: Float,
    val treble: Float,
    val power: Float,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        time = parcel.readDouble(),
        bass = parcel.readFloat(),
        lowMid = parcel.readFloat(),
        mid = parcel.readFloat(),
        vocal = parcel.readFloat(),
        treble = parcel.readFloat(),
        power = parcel.readFloat(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(time)
        parcel.writeFloat(bass)
        parcel.writeFloat(lowMid)
        parcel.writeFloat(mid)
        parcel.writeFloat(vocal)
        parcel.writeFloat(treble)
        parcel.writeFloat(power)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<AudioFrame> = object : Parcelable.Creator<AudioFrame> {
            override fun createFromParcel(parcel: Parcel): AudioFrame = AudioFrame(parcel)
            override fun newArray(size: Int): Array<AudioFrame?> = arrayOfNulls(size)
        }
    }
}

/**
 * Asset stored in project bundle
 */
@Serializable
data class ProjectAsset(
    val id: String,
    val type: AssetType,
    val mimeType: String,
    val data: ByteArray,
    val name: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        type = AssetType.valueOf(parcel.readString()!!),
        mimeType = parcel.readString()!!,
        data = parcel.createByteArray()!!,
        name = parcel.readString()!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(type.name)
        parcel.writeString(mimeType)
        parcel.writeByteArray(data)
        parcel.writeString(name)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ProjectAsset> = object : Parcelable.Creator<ProjectAsset> {
            override fun createFromParcel(parcel: Parcel): ProjectAsset = ProjectAsset(parcel)
            override fun newArray(size: Int): Array<ProjectAsset?> = arrayOfNulls(size)
        }
    }
}

enum class AssetType {
    AUDIO, IMAGE, VIDEO, FONT, COVER, MONET, CUSTOM_BACKGROUND
}