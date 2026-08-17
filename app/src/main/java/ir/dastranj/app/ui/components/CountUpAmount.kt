package ir.dastranj.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import ir.dastranj.app.ui.theme.Dastranj
import ir.dastranj.app.ui.util.PersianNumbers

/**
 * A money figure that counts up to its value once, on first paint.
 *
 * `tokens/motion.css` gives this its own duration (`--dur-count`, 900ms) and the DS guide is
 * explicit that it happens **once and never again** — a balance that re-animates on every refresh
 * reads as a slot machine rather than as a number settling.
 *
 * Two consequences of "once" that the implementation has to respect:
 *
 * - **Whether to animate is hoisted, not local.** A `remember` inside a lazy-list item is discarded
 *   when the item is recycled, so scrolling a card off screen and back would replay the count. The
 *   caller owns the "already animated" set and passes [animate] down.
 * - **A later balance change does not re-animate.** When [animate] is false the figure snaps
 *   straight to its value, so adding a transaction updates the card without a second count-up.
 *
 * The easing is a tween, never a spring: nothing in Dastranj overshoots, and a balance that
 * overshoots shows the user a number they do not have.
 */
@Composable
fun CountUpAmount(
    targetToman: Long,
    animate: Boolean,
    onAnimationFinished: () -> Unit,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // Progress from 0 to 1 rather than the amount itself, so the same spec drives any magnitude.
    val progress = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(targetToman, animate) {
        if (!animate) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = Dastranj.motion.count,
                easing = Dastranj.motion.easeOut,
            ),
        )
        onAnimationFinished()
    }

    // Computed in Double: a Float carries only ~7 significant digits, which would visibly quantise
    // a balance in the tens of millions during the count.
    val shown = (targetToman.toDouble() * progress.value).toLong()

    Text(
        text = PersianNumbers.formatGrouped(shown),
        style = style,
        color = color,
        maxLines = 1,
        modifier = modifier,
    )
}
