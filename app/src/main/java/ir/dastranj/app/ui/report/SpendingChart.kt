package ir.dastranj.app.ui.report

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.dastranj.app.domain.report.ChartHitTest
import ir.dastranj.app.ui.theme.Dastranj

/** One bar's data, already resolved to a fraction of the plot height. */
data class ChartBar(
    val monthIndex: Int,
    val monthName: String,
    /** 0f..1f of the plot height. Ignored when [isFuture]. */
    val fraction: Float,
    val isFuture: Boolean,
    val isZero: Boolean,
    val isCurrentMonth: Boolean,
    /** Spoken form, e.g. «مرداد — ۲٬۳۲۰٬۰۰۰ تومان». */
    val contentDescription: String,
)

/**
 * The twelve-month spending chart.
 *
 * Drawn on a Canvas rather than composed from Boxes, and hand-built rather than taken from a
 * charting library: the four bar states, the RTL month order and the dashed future bars are all
 * specific enough that a library would need fighting, and PRD §13.4's size budget does not want a
 * charting dependency for one screen.
 *
 * ## RTL
 *
 * Canvas coordinates always run left-to-right, but the chart must read right-to-left: Farvardin
 * sits at the **right** edge and Esfand at the left. The bars are therefore laid out by mirroring
 * the slot index, and the tap handler mirrors the same way — which is the part that would silently
 * select the wrong month if it were done in only one of the two places.
 *
 * ## Four bar states
 *
 * - **future** — a dashed outline stub, because a month that has not happened is not zero spending;
 * - **current month** — brand green, so "so far this month" is distinguishable from a finished one;
 * - **selected** — solid ink;
 * - **normal** — neutral grey.
 */
@Composable
fun SpendingChart(
    bars: List<ChartBar>,
    selectedMonthIndex: Int?,
    onMonthTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Dastranj.colors

    val barColor = colors.muted.copy(alpha = if (colors.isDark) 0.55f else 0.45f)
    val selectedColor = colors.title
    val currentColor = colors.brand
    val futureColor = colors.faint

    // Grows once when the series changes, so switching category or year animates rather than
    // snapping. Ease-out, never a spring.
    val growth by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(Dastranj.motion.slow, easing = Dastranj.motion.easeOut),
        label = "chartGrowth",
    )

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PLOT_HEIGHT)
                .pointerInput(bars) {
                    detectTapGestures { offset ->
                        // Mirrored by the same function the drawing agrees with — see ChartHitTest
                        // for why this is not inlined here.
                        val index = ChartHitTest.monthIndexAt(
                            x = offset.x,
                            width = size.width.toFloat(),
                            barCount = bars.size,
                        ) ?: return@detectTapGestures
                        val bar = bars.getOrNull(index) ?: return@detectTapGestures
                        // A future month has nothing to show, so it does not respond.
                        if (!bar.isFuture) onMonthTapped(index)
                    }
                },
        ) {
            val slotWidth = size.width / bars.size
            val barWidth = minOf(slotWidth * BAR_WIDTH_RATIO, MAX_BAR_WIDTH.toPx())
            val radius = CornerRadius(BAR_RADIUS.toPx(), BAR_RADIUS.toPx())

            bars.forEachIndexed { index, bar ->
                // Index 0 at the right edge.
                val slotStart = size.width - (index + 1) * slotWidth
                val left = slotStart + (slotWidth - barWidth) / 2f

                if (bar.isFuture) {
                    drawFutureStub(left, barWidth, radius, futureColor)
                } else {
                    val minHeight = if (bar.isZero) ZERO_BAR_HEIGHT.toPx() else MIN_BAR_HEIGHT.toPx()
                    val full = bar.fraction * size.height
                    val height = maxOf(minHeight, full) * growth

                    drawRoundRect(
                        color = when {
                            index == selectedMonthIndex -> selectedColor
                            bar.isCurrentMonth -> currentColor
                            else -> barColor
                        },
                        topLeft = Offset(left, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = radius,
                    )
                }
            }
        }

        // Labels are composed rather than drawn: Canvas text would need a TextMeasurer and would
        // not pick up the font scale, which PRD §13.5 requires the layout to survive at 200%.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            bars.forEach { bar ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(LABEL_HEIGHT)
                        .semantics { contentDescription = bar.contentDescription },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        text = bar.monthName,
                        style = Dastranj.type.micro,
                        fontWeight = if (bar.isCurrentMonth) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            bar.isFuture -> colors.faint
                            bar.isCurrentMonth -> colors.title
                            else -> colors.muted
                        },
                        maxLines = 1,
                        // The design sets the labels vertical (`writing-mode: vertical-rl` plus a
                        // 180° turn), which reads as a -90° rotation here.
                        modifier = Modifier.rotate(-90f),
                    )
                }
            }
        }
    }
}

/**
 * A future month: a short dashed outline rather than a zero-height bar.
 *
 * The distinction is the point — an empty slot would read as "you spent nothing", which is a
 * different and wrong claim about a month that has not happened.
 */
private fun DrawScope.drawFutureStub(
    left: Float,
    barWidth: Float,
    radius: CornerRadius,
    color: Color,
) {
    val height = FUTURE_STUB_HEIGHT.toPx()
    val stroke = FUTURE_STROKE.toPx()

    drawRoundRect(
        color = color.copy(alpha = 0.7f),
        topLeft = Offset(left + stroke / 2f, size.height - height + stroke / 2f),
        size = Size(barWidth - stroke, height - stroke),
        cornerRadius = radius,
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
        ),
    )
}

private val PLOT_HEIGHT = 180.dp
private val LABEL_HEIGHT = 54.dp
private val MAX_BAR_WIDTH = 20.dp
private val BAR_RADIUS = 6.dp
private val MIN_BAR_HEIGHT = 6.dp
private val ZERO_BAR_HEIGHT = 3.dp
private val FUTURE_STUB_HEIGHT = 18.dp
private val FUTURE_STROKE = 1.5.dp
private const val BAR_WIDTH_RATIO = 0.62f
