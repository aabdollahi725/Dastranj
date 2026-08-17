package ir.dastranj.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A dashed rounded-rectangle outline.
 *
 * `Modifier.border` cannot dash, so the "add account" card's `1.5px dashed` outline is drawn
 * directly. The stroke is inset by half its width, because a stroke straddles the path it follows —
 * without the inset the outer half would be clipped away by the card's own rounded clip and the
 * line would read thinner on the corners than on the edges.
 */
fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 5.dp,
): Modifier = drawBehind {
    val stroke = strokeWidth.toPx()
    val inset = stroke / 2f
    val radius = cornerRadius.toPx()

    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius - inset, radius - inset),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            ),
        ),
    )
}
