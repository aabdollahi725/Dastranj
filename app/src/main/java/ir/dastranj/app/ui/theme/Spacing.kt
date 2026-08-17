package ir.dastranj.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * tokens/spacing.css — 4dp base grid. Dastranj screens are airy; 20dp is the default gutter.
 */
@Immutable
data class DastranjSpacing(
    val none: Dp = 0.dp,
    val x1: Dp = 4.dp,
    val x2: Dp = 8.dp,
    val x3: Dp = 12.dp,
    val x4: Dp = 16.dp,
    val x5: Dp = 20.dp,
    val x6: Dp = 24.dp,
    val x7: Dp = 28.dp,
    val x8: Dp = 32.dp,
    val x10: Dp = 40.dp,
    val x12: Dp = 48.dp,
    val x16: Dp = 64.dp,
    val x20: Dp = 80.dp,

    /** Side padding of every screen. */
    val screenGutter: Dp = 20.dp,
    /** Between stacked cards. */
    val cardGap: Dp = 12.dp,
    /** Between titled sections. */
    val sectionGap: Dp = 28.dp,
    /** Inside a card. */
    val cardPadding: Dp = 18.dp,
    /** Inside a list row. */
    val rowPadding: Dp = 14.dp,

    /**
     * Never smaller. Note the DS token says 44dp but CLAUDE.md §9 and PRD §13.5 both require a
     * 48dp minimum, so [tapMin] follows the stricter product rule and [tapMinDs] records what the
     * design system itself asked for.
     */
    val tapMin: Dp = 48.dp,
    val tapMinDs: Dp = 44.dp,

    val tabBarHeight: Dp = 64.dp,
    val appBarHeight: Dp = 56.dp,
)
