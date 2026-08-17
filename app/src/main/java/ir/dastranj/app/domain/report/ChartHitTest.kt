package ir.dastranj.app.domain.report

/**
 * Maps a tap's x-coordinate to a month index in the right-to-left chart.
 *
 * Extracted from the chart because it is the one piece of that drawing code that can be wrong
 * without looking wrong: a mirrored-the-wrong-way hit test still selects *a* bar, just not the one
 * under the finger, and the tooltip would confidently show Esfand's figure for a tap on Farvardin.
 *
 * The chart draws index 0 at the **right** edge, so this mirrors to match. Both the drawing and
 * this function must agree; that agreement is what the tests pin down.
 */
object ChartHitTest {

    /**
     * @param x tap position in pixels, measured from the left edge as Canvas reports it.
     * @param width total chart width in pixels.
     * @param barCount number of bars, normally twelve.
     * @return the month index, or null when the geometry is degenerate.
     */
    fun monthIndexAt(x: Float, width: Float, barCount: Int): Int? {
        if (barCount <= 0 || width <= 0f) return null

        val slot = width / barCount
        // Distance from the right edge, in slots.
        val fromRight = (width - x) / slot

        return fromRight.toInt().coerceIn(0, barCount - 1)
    }
}
