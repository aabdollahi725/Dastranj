package ir.dastranj.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * tokens/shape.css. "Nothing is square" — actions are full pills, containers 20–24dp.
 */
@Immutable
data class DastranjShapes(
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 28.dp,
    val card: Dp = 20.dp,
    val row: Dp = 16.dp,
    /** Icon tiles inside rows are 42×42 at 14dp radius. */
    val iconTile: Dp = 14.dp,
    val sheet: Dp = 28.dp,
) {
    val cardShape = RoundedCornerShape(card)
    val rowShape = RoundedCornerShape(row)
    val iconTileShape = RoundedCornerShape(iconTile)
    val pill = RoundedCornerShape(percent = 50)

    /** Bottom sheets round their top corners only. */
    val sheetShape = RoundedCornerShape(topStart = sheet, topEnd = sheet)
}

/**
 * tokens/shape.css shadows: wide, low-opacity, almost colourless — "nothing pops".
 *
 * Compose's `shadow()` cannot express a two-layer shadow with independent blur and opacity the way
 * the CSS tokens do, so these carry the *elevation* half of each token and the tinting is dropped.
 * The one visually load-bearing shadow, `--sh-brand` (the green glow under the gradient CTA), is
 * reproduced with `ambientColor`/`spotColor` at the call site instead.
 */
@Immutable
data class DastranjElevation(
    val card: Dp = 2.dp,
    val raised: Dp = 8.dp,
    val brand: Dp = 10.dp,
    val sheet: Dp = 16.dp,
    val none: Dp = 0.dp,
)
