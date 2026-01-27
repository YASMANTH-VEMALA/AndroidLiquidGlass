package com.kyant.backdrop.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.RoundedRectangle

@Composable
fun MainContent() {
    Box(
        Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val backdrop = rememberLayerBackdrop()

        Box(
            Modifier
                .layerBackdrop(backdrop)
                .drawBehind {
                    drawRect(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF6D6D),
                                Color(0xFFFFD86D),
                                Color(0xFF6DFF8E),
                                Color(0xFF6DE1FF),
                                Color(0xFFFF6D6D),
                            )
                        )
                    )
                    drawRect(Color.Black.copy(alpha = 0.5f))
                }
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "This is the content behind the backdrop!",
                style = TextStyle(fontSize = 20f.sp)
            )
        }

        Box(
            Modifier
                .drawBackdrop(
                    backdrop,
                    { RoundedRectangle(32f.dp) },
                    {
                        colorControls(brightness = 0.2f, contrast = 1.5f, saturation = 1.5f)
                        blur(1f.dp.toPx())
                        lens(32f.dp.toPx(), 64f.dp.toPx())
                    }
                )
                .size(256f.dp)
        )
    }
}
