package com.usesuno.mvstudio.utils

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.usesuno.mvstudio.data.db.AssetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/** Loads downloadable Google Fonts and local font assets, with safe system fallbacks. */
class FontManager(private val context: Context) {
    private val fontCache = ConcurrentHashMap<String, Typeface>()
    private val mainHandler = Handler(Looper.getMainLooper())

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

    suspend fun loadGoogleFont(family: String, weight: Int = 400): Typeface? {
        val key = "$family-$weight"
        fontCache[key]?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            val request = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "$family:$weight",
                emptyList(),
            )
            val callback = object : FontsContractCompat.FontRequestCallback() {
                override fun onTypefaceRetrieved(typeface: Typeface) {
                    fontCache[key] = typeface
                    if (continuation.isActive) continuation.resume(typeface)
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }

            try {
                FontsContractCompat.requestFont(context, request, callback, mainHandler)
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    suspend fun loadLocalFont(asset: AssetEntity): Typeface? {
        val key = "local-${asset.id}"
        fontCache[key]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val fontsDir = File(context.cacheDir, "fonts").apply { mkdirs() }
                val file = File(fontsDir, "${asset.id}.${getFontExtension(asset.mimeType)}")
                FileOutputStream(file).use { it.write(asset.data) }
                Typeface.createFromFile(file).also { fontCache[key] = it }
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun getFont(family: String, weight: Int, assetId: String? = null): Typeface {
        val key = if (assetId != null) "local-$assetId" else "$family-$weight"
        fontCache[key]?.let { return it }

        // Asset lookup belongs to the caller; without an AssetEntity use a system fallback.
        val typeface = if (assetId == null) {
            loadGoogleFont(family, weight) ?: Typeface.create(family, weight, false)
        } else {
            Typeface.create(family, weight, false)
        }
        fontCache.putIfAbsent(key, typeface)
        return fontCache[key] ?: typeface
    }

    fun clearCache() {
        fontCache.clear()
    }

    private fun getFontExtension(mimeType: String): String = when (mimeType.lowercase()) {
        "font/ttf", "application/x-font-ttf" -> "ttf"
        "font/otf", "application/x-font-otf" -> "otf"
        else -> "ttf"
    }

    suspend fun preloadCommonFonts() {
        listOf(
            "Inter" to 400,
            "Inter" to 600,
            "Roboto" to 400,
            "Roboto" to 700,
            "Open Sans" to 400,
            "Montserrat" to 400,
            "Montserrat" to 700,
        ).forEach { (family, weight) -> loadGoogleFont(family, weight) }
    }
}
