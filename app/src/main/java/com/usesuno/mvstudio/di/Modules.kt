package com.usesuno.mvstudio.di

import android.content.Context
import androidx.room.Room
import com.usesuno.mvstudio.data.db.AppDatabase
import com.usesuno.mvstudio.data.db.ProjectDao
import com.usesuno.mvstudio.data.db.AssetDao
import com.usesuno.mvstudio.data.persistence.ProjectBundleSerializer
import com.usesuno.mvstudio.domain.audio.AudioAnalyzer
import com.usesuno.mvstudio.domain.audio.MediaCodecAudioDecoder
import com.usesuno.mvstudio.domain.audio.PlaybackClock
import com.usesuno.mvstudio.domain.audio.WaveformExtractor
import com.usesuno.mvstudio.domain.lyrics.EmbeddedLyricsExtractor
import com.usesuno.mvstudio.domain.lyrics.LyricsParser
import com.usesuno.mvstudio.domain.lyrics.LyricsSegmentation
import com.usesuno.mvstudio.render.engine.RendererFactory
import com.usesuno.mvstudio.render.engine.RenderEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "usesuno_mvstudio.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    @Singleton
    fun provideAssetDao(database: AppDatabase): AssetDao = database.assetDao()
}

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideAudioAnalyzer(): AudioAnalyzer = AudioAnalyzer()

    @Provides
    @Singleton
    fun provideMediaCodecAudioDecoder(): MediaCodecAudioDecoder = MediaCodecAudioDecoder()

    @Provides
    @Singleton
    fun provideWaveformExtractor(): WaveformExtractor = WaveformExtractor()

    @Provides
    @Singleton
    fun provideLyricsParser(): LyricsParser = LyricsParser

    @Provides
    @Singleton
    fun provideEmbeddedLyricsExtractor(): EmbeddedLyricsExtractor = EmbeddedLyricsExtractor()

    @Provides
    @Singleton
    fun provideLyricsSegmentation(): LyricsSegmentation = LyricsSegmentation

    @Provides
    @Singleton
    fun provideProjectBundleSerializer(@ApplicationContext context: Context): ProjectBundleSerializer =
        ProjectBundleSerializer(context)
}

@Module
@InstallIn(SingletonComponent::class)
object RenderModule {

    @Provides
    @Singleton
    fun provideRendererFactory(): RendererFactory = RendererFactory
}