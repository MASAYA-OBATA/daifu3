package com.example.daifu3.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun AnimatedPass(
    isPassing: Boolean,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(
        targetState = isPassing,
        label = "Pass Animation"
    )
    
    val offsetX by transition.animateInt(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300, easing = EaseOutQuad)
            } else {
                tween(durationMillis = 200, easing = EaseInQuad)
            }
        },
        label = "Pass Offset X"
    ) { passing ->
        if (passing) 100 else 0
    }
    
    val alpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300, easing = EaseOutQuad)
            } else {
                tween(durationMillis = 200, easing = EaseInQuad)
            }
        },
        label = "Pass Alpha"
    ) { passing ->
        if (passing) 0f else 1f
    }
    
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, 0) }
            .alpha(alpha)
    ) {
        content()
    }
}