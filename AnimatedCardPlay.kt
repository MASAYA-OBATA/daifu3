package com.example.daifu3.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun AnimatedCardPlay(
    isPlaying: Boolean,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(
        targetState = isPlaying,
        label = "Card Play Animation"
    )
    
    val offsetY by transition.animateInt(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300, easing = EaseOutQuad)
            } else {
                tween(durationMillis = 200, easing = EaseInQuad)
            }
        },
        label = "Card Offset Y"
    ) { playing ->
        if (playing) -50 else 0
    }
    
    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300, easing = EaseOutQuad)
            } else {
                tween(durationMillis = 200, easing = EaseInQuad)
            }
        },
        label = "Card Scale"
    ) { playing ->
        if (playing) 0.9f else 1f
    }
    
    Box(
        modifier = Modifier
            .offset { IntOffset(0, offsetY) }
            .scale(scale)
    ) {
        content()
    }
}