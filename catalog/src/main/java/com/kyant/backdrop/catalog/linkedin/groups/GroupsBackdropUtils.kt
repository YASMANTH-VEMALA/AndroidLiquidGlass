package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

/**
 * Simplified drawBackdrop extension for Groups/Circles screens with standard glass effect.
 * Uses a rounded rectangle shape and applies blur + vibrancy effects.
 */
fun Modifier.glassBackground(
    backdrop: LayerBackdrop,
    blurRadius: Float = 20f,
    vibrancyAlpha: Float = 0.1f,
    cornerRadius: Float = 16f,
    surfaceAlpha: Float = 0.1f
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedRectangle(cornerRadius.dp) },
    effects = {
        vibrancy()
        blur(blurRadius.dp.toPx())
    },
    onDrawSurface = {
        drawRect(Color.White.copy(alpha = surfaceAlpha))
    }
)

/**
 * Glass background with no rounded corners (for full-screen headers etc)
 */
fun Modifier.glassBackgroundFlat(
    backdrop: LayerBackdrop,
    blurRadius: Float = 20f,
    surfaceAlpha: Float = 0.1f
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = { RoundedRectangle(0f.dp) },
    effects = {
        vibrancy()
        blur(blurRadius.dp.toPx())
    },
    onDrawSurface = {
        drawRect(Color.White.copy(alpha = surfaceAlpha))
    }
)
