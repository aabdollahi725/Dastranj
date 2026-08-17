package ir.dastranj.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

/**
 * tokens/motion.css — "Calm: things ease out, never bounce. Nothing in Dastranj overshoots."
 *
 * There is deliberately no spring spec here. A spring on a balance figure reads as a slot machine,
 * which the DS guide calls out by name; every animation in the app uses [tween] with one of these
 * easings so overshoot is structurally impossible.
 */
@Immutable
data class DastranjMotion(
    val easeOut: Easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f),
    val easeInOut: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f),
    /** Sheets and modals. */
    val easeEnter: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),

    /** Press feedback. */
    val instant: Int = 90,
    /** Hover, colour, opacity. */
    val fast: Int = 160,
    /** Toggles, expanding rows, and the tab-bar pill + page transition. */
    val base: Int = 240,
    /** Sheets, screen transitions. */
    val slow: Int = 360,
    /** Balance count-up — once on first paint, never again. */
    val count: Int = 900,

    val pressScale: Float = 0.97f,
    val cardPressScale: Float = 0.99f,
)
