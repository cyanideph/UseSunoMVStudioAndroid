package com.usesuno.mvstudio.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UseSunoMVStudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any global state
    }
}