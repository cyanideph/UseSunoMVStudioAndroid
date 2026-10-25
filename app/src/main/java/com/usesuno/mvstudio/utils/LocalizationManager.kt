package com.usesuno.mvstudio.utils

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

private val Context.localizationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "localization_prefs",
)

/** Localization manager for the languages supported by the web version. */
class LocalizationManager(private val context: Context) {
    private val dataStore = context.localizationDataStore
    private val localeKey = stringPreferencesKey("selected_locale")

    val supportedLocales = listOf(
        Locale("en"),
        Locale("zh", "CN"),
        Locale("zh", "TW"),
        Locale("id"),
        Locale("hi"),
        Locale("pt", "BR"),
        Locale("tl"),
        Locale("de"),
        Locale("ur"),
        Locale("ru"),
        Locale("vi"),
    )

    val supportedLocaleCodes = supportedLocales.map(Locale::toLanguageTag)

    suspend fun getCurrentLocale(): Locale {
        val preferences = dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .first()
        val localeTag = preferences[localeKey] ?: Locale.getDefault().toLanguageTag()
        return Locale.forLanguageTag(localeTag)
    }

    suspend fun setLocale(locale: Locale) {
        dataStore.edit { preferences ->
            preferences[localeKey] = locale.toLanguageTag()
        }
        applyLocale(locale)
    }

    fun applyLocale(locale: Locale) {
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun initialize(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch {
            applyLocale(getCurrentLocale())
        }
    }

    suspend fun getLocaleDisplayName(locale: Locale): String =
        locale.getDisplayName(getCurrentLocale())

    fun getLocaleFromCode(code: String): Locale? =
        supportedLocales.find { it.toLanguageTag() == code }
}

fun Context.getStringWithLocale(
    @Suppress("UNUSED_PARAMETER") locale: Locale,
    @Suppress("UNUSED_PARAMETER") resId: Int,
): String = getString(resId)
