package ir.dastranj.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.dastranj.app.ui.theme.Dastranj

/**
 * The inset-ring form field surface used across the add screens.
 *
 * The design draws these with `box-shadow: inset 0 0 0 1px …`, which has no direct Compose
 * equivalent; a 1dp border on the same rounded shape is visually identical because the field is
 * already clipped. Focus and error thicken it to 2dp and change its colour, matching the design's
 * `--border-focus` and `--ac-danger` states.
 *
 * Apply **after** `clip()` and before any click handling, so the ring follows the clipped shape and
 * sits under the press feedback.
 */
@Composable
fun Modifier.fieldSurface(
    error: Boolean = false,
    focused: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
): Modifier {
    val colors = Dastranj.colors
    val shape = RoundedCornerShape(cornerRadius)

    val ringColor by animateColorAsState(
        targetValue = when {
            error -> colors.danger
            focused -> colors.brand
            else -> colors.hairline
        },
        animationSpec = tween(Dastranj.motion.fast, easing = Dastranj.motion.easeOut),
        label = "fieldRing",
    )

    return this
        .background(colors.card, shape)
        .border(
            width = if (error || focused) 2.dp else 1.dp,
            color = ringColor,
            shape = shape,
        )
}
