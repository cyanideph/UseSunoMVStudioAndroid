package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import com.usesuno.mvstudio.render.engine.FoliaLyricStyle

/**
 * Still template - Swiss minimalist typographic presentation with pure focus on lyric layout
 */
class StillRenderer : Canvas2DRenderer(Template.STILL) {

    override val foliaStyle = object : FoliaLyricStyle {
        // Still template keeps clean, unadorned typography with smooth Folia vertical gliding
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        // Minimalist presentation, rendered via FoliaLyricEngine
    }
}