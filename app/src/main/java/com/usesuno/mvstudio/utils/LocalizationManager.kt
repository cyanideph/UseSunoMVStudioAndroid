package com.usesuno.mvstudio.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.usesuno.mvstudio.di.UseSunoMVStudioApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Localization manager for 11 languages
 * Matches UseSunoMVStudio's i18n.ts (EN, ZH-CN, ZH-TW, ID, HI, PT-BR, TL, DE, UR, RU, VI)
 */
class LocalizationManager(private val context: Context) {

    private val dataStore: androidx.datastore.preferences.DataStore<Preferences> = context.preferencesDataStore("localization_prefs")
    private val LOCALE_KEY = stringPreferencesKey("selected_locale")

    // Supported languages matching web version
    val supportedLocales = listOf(
        Locale("en"),       // English
        Locale("zh", "CN"), // Chinese Simplified
        Locale("zh", "TW"), // Chinese Traditional
        Locale("id"),       // Indonesian
        Locale("hi"),       // Hindi
        Locale("pt", "BR"), // Portuguese (Brazil)
        Locale("tl"),       // Tagalog
        Locale("de"),       // German
        Locale("ur"),       // Urdu
        Locale("ru"),       // Russian
        Locale("vi"),       // Vietnamese
    )

    val supportedLocaleCodes = supportedLocales.map { it.toLanguageTag() }

    /**
     * Get current locale (from DataStore or system default)
     */
    suspend fun getCurrentLocale(): Locale {
        val prefs = dataStore.data.firstOrNull() ?: emptyPreferences()
        val localeTag = prefs[LOCALE_KEY] ?: Locale.getDefault().toLanguageTag()
        return Locale.forLanguageTag(localeTag)
    }

    /**
     * Set locale and persist
     */
    suspend fun setLocale(locale: Locale) {
        dataStore.edit { prefs ->
            prefs[LOCALE_KEY] = locale.toLanguageTag()
        }
        applyLocale(locale)
    }

    /**
     * Apply locale to app context
     */
    fun applyLocale(locale: Locale) {
        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        res.updateConfiguration(config, res.displayMetrics)
    }

    /**
     * Initialize locale on app start
     */
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            val locale = getCurrentLocale()
            applyLocale(locale)
        }
    }

    /**
     * Get display name for locale in current locale
     */
    suspend fun getLocaleDisplayName(locale: Locale): String {
        val currentLocale = getCurrentLocale()
        return locale.getDisplayName(currentLocale)
    }

    /**
     * Get locale from language code
     */
    fun getLocaleFromCode(code: String): Locale? {
        return supportedLocales.find { it.toLanguageTag() == code }
    }
}

/**
 * Extension to get string resources with locale
 */
fun Context.getStringWithLocale(@Suppress("UNUSED_PARAMETER") locale: Locale, @Suppress("UNUSED_PARAMETER") resId: Int): String {
    // Implementation would use Resources.getString with locale-specific configuration
    return getString(resId)
}