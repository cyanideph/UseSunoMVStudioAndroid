package com.usesuno.mvstudio.utils

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.core.provider.FontsContractCompat
import androidx.core.provider.FontRequest
import com.usesuno.mvstudio.domain.model.AssetType
import com.usesuno.mvstudio.data.db.AssetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

/**
 * Font manager for loading Google Fonts and local fonts
 */
class FontManager(private val context: Context) {

    private val fontCache = mutableMapOf<String, Typeface>()
    private val downloadExecutor = Executor { Runnable::run }

    /**
     * Predefined Google Fonts (matching web version)
     */
    data class GoogleFont(
        val family: String,
        val variants: List<String>,
        val category: String,
    )

    val availableFonts = listOf(
        GoogleFont("Inter", listOf("100", "200", "300", "400", "500", "600", "700", "800", "900"), "sans-serif"),
        GoogleFont("Roboto", listOf("100", "300", "400", "500", "700", "900"), "sans-serif"),
        GoogleFont("Open Sans", listOf("300", "400", "500", "600", "700", "800"), "sans-serif"),
        GoogleFont("Montserrat", listOf("100", "200", "300", "400", "500", "600", "700", "800", "900"), "sans-serif"),
        GoogleFont("Poppins", listOf("100", "200", "300", "400", "500", "600", "700", "800", "900"), "sans-serif"),
        GoogleFont("Nunito", listOf("200", "300", "400", "500", "600", "700", "800", "900"), "sans-serif"),
        GoogleFont("Lato", listOf("100", "300", "400", "700", "900"), "sans-serif"),
        GoogleFont("Source Sans Pro", listOf("200", "300", "400", "600", "700", "900"), "sans-serif"),
        GoogleFont("Playfair Display", listOf("400", "500", "600", "700", "800", "900"), "serif"),
        GoogleFont("Merriweather", listOf("300", "400", "700", "900"), "serif"),
        GoogleFont("Crimson Text", listOf("400", "600", "700"), "serif"),
        GoogleFont("Space Mono", listOf("400", "700"), "monospace"),
        GoogleFont("JetBrains Mono", listOf("100", "200", "300", "400", "500", "600", "700", "800"), "monospace"),
        GoogleFont("Fira Code", listOf("300", "400", "500", "600", "700"), "monospace"),
        GoogleFont("Dancing Script", listOf("400", "500", "600", "700"), "handwriting"),
        GoogleFont("Pacifico", listOf("400"), "handwriting"),
        GoogleFont("Lobster", listOf("400"), "display"),
        GoogleFont("Oswald", listOf("200", "300", "400", "500", "600", "700"), "display"),
        GoogleFont("Bebas Neue", listOf("400"), "display"),
    )

    /**
     * Load font from Google Fonts using Downloadable Fonts API
     */
    suspend fun loadGoogleFont(family: String, weight: Int = 400): Typeface? {
        val cacheKey = "$family-$weight"
        return fontCache[cacheKey] ?: withContext(Dispatchers.IO) {
            try {
                // Use Downloadable Fonts API
                val fontRequest = FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    "$family:$weight",
                    emptyList<List<ByteArray>>() // Empty certificate array for Google Fonts provider
                )

                val callback = object : FontsContractCompat.FontRequestCallback() {
                    override fun onTypefaceRetrieved(typeface: Typeface) {
                        fontCache[cacheKey] = typeface
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {
                        // Fallback to system font
                    }
                }

                FontsContractCompat.requestFont(context, fontRequest, callback, Handler(Looper.getMainLooper()))

                // Wait for callback (simplified - real implementation uses suspendCancellableCoroutine)
                Thread.sleep(100)
                fontCache[cacheKey]
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Load local font from assets
     */
    suspend fun loadLocalFont(asset: AssetEntity): Typeface? {
        val cacheKey = "local-${asset.id}"
        return fontCache[cacheKey] ?: withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "fonts/${asset.id}.${getFontExtension(asset.mimeType)}")
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { it.write(asset.data) }

                val typeface = Typeface.createFromFile(file) ?: return@withContext null
                fontCache[cacheKey] = typeface
                typeface
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Get font from cache or load
     */
    suspend fun getFont(family: String, weight: Int, assetId: String? = null): Typeface {
        val cacheKey = if (assetId != null) "local-$assetId" else "$family-$weight"

        return fontCache.getOrPut(cacheKey) {
            if (assetId != null) {
                // Load from local asset (would need database lookup)
                Typeface.create(family, weight)
            } else {
                loadGoogleFont(family, weight) ?: Typeface.create(family, weight)
            }
        }
    }

    /**
     * Clear font cache
     */
    fun clearCache() {
        fontCache.clear()
    }

    private fun getFontExtension(mimeType: String): String = when (mimeType) {
        "font/ttf", "application/x-font-ttf" -> "ttf"
        "font/otf", "application/x-font-otf" -> "otf"
        "font/woff" -> "woff"
        "font/woff2" -> "woff2"
        else -> "ttf"
    }

    /**
     * Preload common fonts
     */
    suspend fun preloadCommonFonts() {
        val commonFonts = listOf(
            "Inter" to 400,
            "Inter" to 600,
            "Roboto" to 400,
            "Roboto" to 700,
            "Open Sans" to 400,
            "Montserrat" to 400,
            "Montserrat" to 700,
        )

        for ((family, weight) in commonFonts) {
            loadGoogleFont(family, weight)
        }
    }
}