package ir.dastranj.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import ir.dastranj.app.ui.theme.Dastranj

/**
 * The product's one press interaction: a brief scale-down, no ripple.
 *
 * `tokens/motion.css` specifies `--press-scale: .97` over `--dur-instant` (90ms) with an ease-out
 * curve, and the DS guide is explicit that nothing overshoots — hence [tween] rather than a spring.
 *
 * Material's ripple is deliberately not used. Every interactive surface in the design signals a
 * press by scaling, and a ripple on a white card reads as a different product.
 *
 * @param scaleTo defaults to the DS press scale; cards use the gentler `--card-press-scale` (.99),
 *   because a full 3% shrink on a large surface reads as a jolt.
 */
fun Modifier.pressScaleClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    scaleTo: Float? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val target = scaleTo ?: Dastranj.motion.pressScale

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) target else 1f,
        animationSpec = tween(Dastranj.motion.instant, easing = Dastranj.motion.easeOut),
        label = "pressScale",
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}

/** Card-sized surfaces scale less than buttons do. */
@Composable
fun Modifier.cardPressClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    onClickLabel: String? = null,
): Modifier = pressScaleClickable(
    onClick = onClick,
    enabled = enabled,
    onClickLabel = onClickLabel,
    scaleTo = Dastranj.motion.cardPressScale,
)
